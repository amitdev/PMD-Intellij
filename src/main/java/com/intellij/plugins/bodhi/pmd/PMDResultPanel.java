package com.intellij.plugins.bodhi.pmd;

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.plugins.bodhi.pmd.actions.AnEDTAction;
import com.intellij.plugins.bodhi.pmd.core.*;
import com.intellij.plugins.bodhi.pmd.tree.*;
import com.intellij.pom.Navigatable;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.jcef.JCEFHtmlPanel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import net.sourceforge.pmd.reporting.Report;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION;

/**
 * The result panel where the PMD results are shown. This includes a toolbar, a
 * tree to show the violations found in a pmd run and details about the violations:
 * resizable documentation on HTML and example code.
 *
 * @author bodhi
 * @version 1.3
 */
public class PMDResultPanel extends JPanel implements HTMLReloadable {

    public static final String PMD_SUPPRESSION = "//NOPMD";
    private final Tree resultTree;
    private final PMDProjectComponent projectComponent;
    // html documentation
    private final JCEFHtmlPanel ruleHtmlPanel = new JCEFHtmlPanel("");
    // code example with syntax highlighting
    private final EditorTextField ruleExampleFieldJava = new EditorTextField();
    private final EditorTextField ruleExampleFieldKotlin = new EditorTextField();
    private final OnePixelSplitter detailSplit = new OnePixelSplitter(true);
    private PMDRootNode rootNode;
    private PMDErrorBranchNode processingErrorsNode;
    private boolean scrolling;
    private PMDPopupMenu popupMenu;

    private @NotNull String lastHtmlContent = "";

    static {
        // switch-off jcef logging if property not set, otherwise it bloats the home directory
        if (System.getProperty("ide.browser.jcef.log.level") == null) {
            System.setProperty("ide.browser.jcef.log.level", "disable");
        }
    }

    /**
     * Create an instance of the result panel.
     *
     * @param projectComponent The Project Component.
     */
    public PMDResultPanel(final PMDProjectComponent projectComponent) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.projectComponent = projectComponent;
        setBorder(JBUI.Borders.empty(2));

        // Create the tree which can show tooltips as well.
        resultTree = createResultTree();

        ToolTipManager.sharedInstance().registerComponent(resultTree);

        //Create the actions of the toolbar and create it.
        ActionGroup actionGrp = createActions();
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(PMDProjectComponent.TOOL_ID, actionGrp, false);
        JComponent toolbarComponent = toolbar.getComponent();
        toolbar.setTargetComponent(toolbarComponent); // prevent warning
        toolbarComponent.setVisible(true);
        toolbarComponent.setMinimumSize(new Dimension(20, 100));
        toolbarComponent.setMaximumSize(new Dimension(20, 1000));
        toolbarComponent.setPreferredSize(new Dimension(20, 300));
        add(toolbarComponent);

        initializeTree();

        resultTree.setCellRenderer(new PMDCellRenderer());
        TreeUtil.expandAll(resultTree);
        resultTree.setExpandsSelectedPaths(true);
        resultTree.getSelectionModel().setSelectionMode(SINGLE_TREE_SELECTION);
        add(buildMainSplit());

        //Add right-click menu to the tree
        createPmdPopupMenu();

        //Add mouse listener to support single and double click and popup actions.
        MouseAdapter treeMouseListener = createTreeMouseListener();
        resultTree.addMouseListener(treeMouseListener);
    }

    private @NotNull Tree createResultTree() {
        final Tree resultTree;
        resultTree = new Tree() {
            public String getToolTipText(MouseEvent evt) {
                if (getRowForLocation(evt.getX(), evt.getY()) == -1)
                    return null;
                TreePath curPath = getPathForLocation(evt.getX(), evt.getY());
                if (curPath != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) curPath.getLastPathComponent();
                    if (node instanceof BasePMDNode) {
                        return ((BasePMDNode) node).getToolTip();
                    }
                }
                return super.getToolTipText(evt);
            }
        };
        return resultTree;
    }


    /**
     * Creates an instance of {@link PMDPopupMenu} configured with actions for handling PMD violations.
     * The menu includes actions for suppressing selected violations and displaying rule details.
     */
    private void createPmdPopupMenu() {
        popupMenu = new PMDPopupMenu(e -> {
            final List<PMDViolation> violations = popupMenu.getViolations();
            if (e.getActionCommand().equals(PMDPopupMenu.SUPPRESS)) {
                // suppress all selected violations, max 1 per file+line
                Map<String, PMDViolation> uniqueViolationsMap = new HashMap<>();
                for (PMDViolation violation : violations) {
                    uniqueViolationsMap.put(violation.getFilePath() + ":" + violation.getBeginLine(), violation);
                }
                for (PMDViolation violation : uniqueViolationsMap.values()) {
                    //Suppress the violation
                    final Editor editor = openEditor(violation);
                    if (editor != null) {
                        executeWrite(editor, violation);
                    }
                }
            }
        });
    }

    /**
     * Creates a MouseAdapter to handle mouse interactions with the result tree.
     * - On single click: Updates the rule details in the UI.
     * - On double click: Highlights the finding in the editor for all selected nodes.
     * - On right-click (popup trigger): Displays a context menu for the selected nodes.
     *
     * @return A MouseAdapter instance responsible for handling tree mouse interactions.
     */
    private @NotNull MouseAdapter createTreeMouseListener() {
        return new MouseAdapter() {
            //Get the current tree node where the mouse event happened
            private DefaultMutableTreeNode getNodeFromEvent(MouseEvent e) {
                DefaultMutableTreeNode result = null;
                TreePath[] selectionPaths = resultTree.getSelectionPaths();
                if (selectionPaths != null && selectionPaths.length > 0 && selectionPaths[0] != null && selectionPaths[0].getLastPathComponent() instanceof DefaultMutableTreeNode) {
                        result = (DefaultMutableTreeNode) selectionPaths[0].getLastPathComponent();
                }
                return result;
            }

            public void mousePressed(MouseEvent e) {
                DefaultMutableTreeNode treeNode = getNodeFromEvent(e);
                if (treeNode != null) {
                    if (e.getClickCount() == 2) {
                        highlightFindingInEditor(treeNode);
                    } else {
                        showPopup(treeNode, e);
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                DefaultMutableTreeNode treeNode = getNodeFromEvent(e);
                if (treeNode != null) {
                    setRuleDetailsOnDocField(treeNode);
                    showPopup(treeNode, e);
                }
            }
        };
    }

    /**
     * Builds and returns the main split panel for the PMD result view.
     *
     * @return The configured main split panel
     */
    private @NotNull OnePixelSplitter buildMainSplit() {
        configureExampleField(ruleExampleFieldJava, FileTypeManager.getInstance().getFileTypeByExtension("java"));
        configureExampleField(ruleExampleFieldKotlin, FileTypeManager.getInstance().getFileTypeByExtension("kt"));

        // Scrolling en viewport
        JBScrollPane scrollPane = new JBScrollPane(resultTree);
        scrollPane.setHorizontalScrollBarPolicy(JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Main horizontal split between primary-tree and detail-doc
        OnePixelSplitter mainSplit = new OnePixelSplitter(false); // horizontal
        mainSplit.setFirstComponent(scrollPane);

        lastHtmlContent = PMDHTMLUtil.HTML_INITIAL_BODY_CONTENT;
        // Vertical split for the html + example
        detailSplit.setBorder(JBUI.Borders.empty(2));
        detailSplit.setFirstComponent(PMDHTMLUtil.configureHtmlPanel(ruleHtmlPanel, detailSplit, this));
        detailSplit.setSecondComponent(ruleExampleFieldJava);

        // Add detailSplit to the main split
        mainSplit.setSecondComponent(detailSplit);

        // Set initial divider positions
        mainSplit.setProportion(0.5f); // 50% each
        detailSplit.setProportion(0.5f);
        return mainSplit;
    }

    private void configureExampleField(EditorTextField exampleField, @Nullable FileType fileType) {
        exampleField.setOneLineMode(false);
        exampleField.setFocusable(false);
        exampleField.setOpaque(false);
        exampleField.setViewer(true);
        exampleField.setBorder(JBUI.Borders.empty(1));
        if (fileType != null) {
            exampleField.setFileType(fileType);
        }
        exampleField.addSettingsProvider(p -> {
            setSyntaxHighlighting(p, fileType);
            p.setVerticalScrollbarVisible(true);
            p.setHorizontalScrollbarVisible(true);
            p.getSettings().setUseSoftWraps(false);
        });
        exampleField.setText("// Example code shows here");
    }

    /**
     * Update the HTML content of the panel, including appropriate CSS for current theme
     *
     * @param htmlContent The HTML BODY content to display
     */
    public void updateHtmlContent(String htmlContent) {
        // Store the HTML content for potential reloading
        lastHtmlContent = htmlContent;

        String completeHtml = PMDHTMLUtil.buildCompleteHtml(htmlContent);

        // Load the HTML content with our styling
        ruleHtmlPanel.loadHTML(completeHtml);

        // scroll to top
        ruleHtmlPanel.getCefBrowser().executeJavaScript(
                "window.scrollTo(0, 0);",
                ruleHtmlPanel.getCefBrowser().getURL(),
                0
        );
    }

    @Override
    public void reloadHTML() {
        updateHtmlContent(lastHtmlContent);
    }

    private void setSyntaxHighlighting(EditorEx p, FileType fileType) {
        ApplicationManager.getApplication().invokeLater(() -> ApplicationManager.getApplication().runWriteAction(() ->
                p.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(projectComponent.getCurrentProject(), fileType))));
    }

    /**
     * Updates the rule details in the documentation panel based on the selected tree node.
     * Displays the rule description, example code, and other relevant information.
     *
     * @param node The selected tree node containing rule information, or null if no selection
     */
    private void setRuleDetailsOnDocField(@Nullable DefaultMutableTreeNode node) {
        String htmlText = "Select a violation for details.";
        String exampleText = "// Example code shows here";
        if (node == null || (!(node instanceof HasMessage) && !(node instanceof HasRule))) {
            updateHtmlContent(htmlText);
            JLabel exampleLabel = new JLabel(exampleText);
            exampleLabel.setVerticalAlignment(SwingConstants.TOP);
            detailSplit.setSecondComponent(exampleLabel);
            return;
        }
        EditorTextField exampleField = ruleExampleFieldJava; // default
        Rule rule = null;
        String message = "";
        if (node instanceof HasRule) {
            rule = ((HasRule) node).getRule();
            message = rule.getMessage();
            String langId = rule.getLanguage().getId(); // java or kotlin
            if (langId.equals("kotlin")) {
                exampleText = getFormattedExamples(rule, "Kotlin");
                exampleField = ruleExampleFieldKotlin;
            } else { // java
                exampleText = getFormattedExamples(rule, "Java");
                exampleField = ruleExampleFieldJava;
            }
        }
        if (node instanceof HasMessage) {
            message = ((HasMessage) node).getMessage();
        }
        htmlText = PMDHTMLUtil.getHtmlText(message, rule);

        updateHtmlContent(htmlText);
        exampleField.setText(exampleText);
        detailSplit.setSecondComponent(exampleField);

        final EditorTextField finalExampleField = exampleField;
        ApplicationManager.getApplication().runReadAction(() -> {
            finalExampleField.setCaretPosition(0); // requires wrapping in read action
        });
        repaint();
        // browser will adjust split proportion
    }

    private static @NotNull String getFormattedExamples(@Nullable Rule rule, @NotNull String language) {
        String examples = "// No " + language + " example available.";
        if (rule != null) {
            StringBuilder examplesBld = new StringBuilder();
            for (String example : rule.getExamples()) {
                examplesBld.append(example.trim()).append("\n\n");
            }
            if (!rule.getExamples().isEmpty() && examplesBld.length() > 4) {
                examples = "// " + language + " example(s):\n" + examplesBld;
            }
        }
        return examples;
    }

    /**
     * Displays the right click popup menu for a tree node.
     *
     * @param treeNode The DefaultMutableTreeNodes where to show the popup
     * @param e         the MouseEvent
     */
    private void showPopup(DefaultMutableTreeNode treeNode, MouseEvent e) {
        //Check if it's a popup trigger
        if (treeNode != null && e.isPopupTrigger()) {
            popupMenu.clearViolationsAndUrl();
            //Only for violation nodes, popups suppress+details are supported
            if (treeNode instanceof PMDViolationNode) {
                popupMenu.addViolation(((PMDViolationNode) treeNode).getPmdViolation());
            }
            if (treeNode instanceof PMDRuleNode pmdRuleNode) {
                for (int i = 0; i < pmdRuleNode.getChildCount(); i++) {
                    if (pmdRuleNode.getChildAt(i) instanceof PMDViolationNode pmdViolationNode) {
                        popupMenu.addViolation(pmdViolationNode.getPmdViolation());
                    }
                }
            }
            //Display popup only if actions are possible
            if (popupMenu.hasVisibleMenuItems()) {
                popupMenu.getMenu().show(resultTree, e.getX(), e.getY());
            }
        }
    }

    /**
     * Executes the suppression of a violation. If the Editor is not read only,
     * it will add the PMD violation suppression comment to the end of line.
     *
     * @param editor The Editor to add the suppression
     * @param result The PMD Violation
     */
    private void executeWrite(final Editor editor, final PMDViolation result) {
        //If readonly show the read-only dialog
        if (!editor.getDocument().isWritable()) {
            if (!FileDocumentManager.fileForDocumentCheckedOutSuccessfully(editor.getDocument(), projectComponent.getCurrentProject()))
                return;
        }

        //Not read only, to execute a command to write to the editor
        CommandProcessor.getInstance().executeCommand(
                projectComponent.getCurrentProject(),
                () -> {
                    //All writes must be through a write action.
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        int offset = editor.getDocument().getLineEndOffset(result.getBeginLine() - 1);
                        //Append PMD special comment to end of line.
                        editor.getDocument().insertString(offset, " " + PMD_SUPPRESSION + " - suppressed " + result.getRuleName() + " - TODO explain reason for suppression");
                    });
                },
                "SuppressViolation",
                null);
    }

    /**
     * Create toolbar actions. The following actions are create:
     * 1. ReRun Action
     * 2. Close Action
     * 3. Collapse All
     * 4. Expand All
     * 5. Next
     * 6. Prev
     * 7. Autoscroll to source.
     *
     * @return The group containing all actions required for the toolbar.
     */
    private ActionGroup createActions() {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new ReRunAction());
        actionGroup.add(new CloseAction());

        // TreeExpander for expand/collapse all.
        TreeExpander treeExpander = new TreeExpander() {
            public void expandAll() {
                TreeUtil.expandAll(resultTree);
            }

            public boolean canExpand() {
                return true;
            }

            public void collapseAll() {
                TreeUtil.collapseAll(resultTree, 1);
            }

            public boolean canCollapse() {
                return true;
            }
        };

        actionGroup.add(CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, this));
        actionGroup.add(CommonActionsManager.getInstance().createExpandAllAction(treeExpander, this));

        //OccurenceNavigator for next/prev actions
        OccurenceNavigator occurenceNavigator = new OccurenceNavigatorSupport(resultTree) {
            @Nullable
            protected Navigatable createDescriptorForNode(DefaultMutableTreeNode node) {
                if (node.getChildCount() > 0) return null;
                if (node instanceof Navigatable navigatable) {
                    return navigatable.canNavigate() ? navigatable : null;
                }
                return null;
            }

            @Override
            public OccurenceInfo goNextOccurence() {
                OccurenceInfo info = super.goNextOccurence();
                if (info.getNavigateable() instanceof DefaultMutableTreeNode node) {
                    setRuleDetailsOnDocField(node);
                }
                return info;
            }

            @Override
            public OccurenceInfo goPreviousOccurence() {
                OccurenceInfo info = super.goPreviousOccurence();
                if (info.getNavigateable() instanceof DefaultMutableTreeNode node) {
                    setRuleDetailsOnDocField(node);
                }
                return info;
            }

            public @NotNull String getNextOccurenceActionName() {
                return UsageViewBundle.message("action.next.occurrence");
            }

            public @NotNull String getPreviousOccurenceActionName() {
                return UsageViewBundle.message("action.previous.occurrence");
            }
        };

        actionGroup.add(CommonActionsManager.getInstance().createNextOccurenceAction(occurenceNavigator));
        actionGroup.add(CommonActionsManager.getInstance().createPrevOccurenceAction(occurenceNavigator));
        actionGroup.add(CommonActionsManager.getInstance().installAutoscrollToSourceHandler(projectComponent.getCurrentProject(),
                resultTree, new AutoScrollToSourceOptionProvider() {
                    public boolean isAutoScrollMode() {
                        return scrolling;
                    }

                    public void setAutoScrollMode(boolean state) {
                        scrolling = state;
                    }
                }));
        actionGroup.add(CommonActionsManager.getInstance().createExportToTextFileAction(new ExporterToTextFile() {
            public JComponent getSettingsEditor() {
                return null;
            }

            public void addSettingsChangedListener(ChangeListener listener) {
            }

            public void removeSettingsChangedListener(ChangeListener listener) {
            }

            public @NotNull String getReportText() {
                Report r = PMDResultCollector.getReport();
                HTMLRenderer renderer = new HTMLRenderer();
                StringWriter w = new StringWriter();
                try {
                    renderer.renderBody(new PrintWriter(w), r);
                    return w.getBuffer().toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "";
            }

            @NotNull
            public String getDefaultFilePath() {
                return "report.html";
            }

            public void exportedTo(@NotNull String filePath) {
            }

            public boolean canExport() {
                return true;
            }
        }));
        return actionGroup;
    }

    /**
     * Initialize the tree.
     */
    public final void initializeTree() {
        rootNode = PMDTreeNodeFactory.getInstance().createRootNode(this);
        TreeModel treeModel = new DefaultTreeModel(rootNode);
        resultTree.setModel(treeModel);
        setRuleDetailsOnDocField(null); // set initial text
    }

    /**
     * Highlights a given violation/suppressed/error represented by the given tree node.
     *
     * @param treeNode The tree node having the violation/suppressed/error
     */
    public void highlightFindingInEditor(DefaultMutableTreeNode treeNode) {
        if (treeNode instanceof Navigatable) {
            ((Navigatable) treeNode).navigate(true);
        }
    }

    /**
     * Highlights a given finding: violation, suppressed violation or error, in the editor
     *
     * @param finding the finding to navigate to
     */
    public void highlightFindingInEditor(HasPositionInFile finding) {
        openEditor(finding);
    }

    /**
     * Opens the given finding's file in the Editor and returns the Editor with caret at the finding.
     *
     * @param finding The finding
     * @return the editor with caret at the finding
     */
    private Editor openEditor(HasPositionInFile finding) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(projectComponent.getCurrentProject());
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(
                finding.getFilePath().replace(File.separatorChar, '/'));
        if (virtualFile != null) {
            return fileEditorManager.openTextEditor(new OpenFileDescriptor(
                            projectComponent.getCurrentProject(),
                            virtualFile,
                            Math.max(finding.getBeginLine() - 1, 0),
                            Math.max(finding.getBeginColumn() - 1, 0)),
                    true);
        }
        return null;
    }

    /**
     * Adds a rule set node to the tree as a direct child of the root, and return it
     *
     * @param name the rule name
     * @return the created rule set node
     */
    public PMDRuleSetNode addCreateRuleSetNodeAtRoot(String name) {
        return (PMDRuleSetNode) addNode(rootNode, new PMDRuleSetNode(name));
    }

    /**
     * Adds a node to the given node as parent.
     *
     * @param parent The parent node
     * @param node   The child not which is added as child to parent node
     * @return the child node
     */
    public BasePMDNode addNode(PMDBranchNode parent, BasePMDNode node) {
        parent.add(node);
        reloadResultTree();
        return node;
    }

    public void reloadResultTree() {
        ApplicationManager.getApplication().invokeLater(() -> ((DefaultTreeModel) resultTree.getModel()).reload());
    }

    /**
     * Get the root node of the violation tree.
     *
     * @return the root node
     */
    public PMDRootNode getRootNode() {
        return rootNode;
    }

    /**
     * Returns the single processingErrors branch node. Created if needed.
     *
     * @return the processingErrors branch node
     */
    public PMDErrorBranchNode getProcessingErrorsNode() {
        if (processingErrorsNode == null) {
            createProcessingErrorNode();
        }
        return processingErrorsNode;
    }

    public PMDErrorBranchNode createProcessingErrorNode() {
        processingErrorsNode = new PMDErrorBranchNode("Processing errors");
        return processingErrorsNode;
    }

    /**
     * Add the leaf node containing the processing errors to the root node, only if it has > 0 leaf nodes.
     */
    public void addProcessingErrorsNodeToRootIfHasAny() {
        if (processingErrorsNode.getChildCount() > 0) {
            rootNode.add(processingErrorsNode);
        }
    }

    /**
     * Inner class for rerun action.
     */
    private class ReRunAction extends AnEDTAction {
        public ReRunAction() {
            super(CommonBundle.message("action.rerun"), UsageViewBundle.message("action.description.rerun"), AllIcons.Actions.Rerun);
            registerCustomShortcutSet(CommonShortcuts.getRerun(), PMDResultPanel.this);
        }

        public void actionPerformed(AnActionEvent e) {
            Project project = e.getData(PlatformDataKeys.PROJECT);
            //Run the last run rule sets
            if (project != null) {
                PMDProjectComponent component = project.getService(PMDProjectComponent.class);
                String ruleSetPaths = component.getLastRunRuleSetPaths();
                AnActionEvent action = component.getLastRunAction();
                AnActionEvent actionToRun = (action != null) ? action : e;
                PMDInvoker.getInstance().runPMD(actionToRun, ruleSetPaths);
                resultTree.repaint();
            }
        }
    }

    /**
     * Inner class for close action.
     */
    private static class CloseAction extends AnEDTAction {
        private static final String ACTION_CLOSE = "action.close";

        private CloseAction() {
            super(CommonBundle.message(ACTION_CLOSE), null, AllIcons.Actions.Cancel);
        }

        public void actionPerformed(AnActionEvent e) {
            Project project = e.getData(PlatformDataKeys.PROJECT);
            if (project != null) {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PMDProjectComponent.TOOL_ID);
                if (toolWindow != null) {
                    toolWindow.activate(null);
                }
                PMDProjectComponent plugin = project.getService(PMDProjectComponent.class);
                plugin.closeResultWindow();
            }
        }
    }

}
