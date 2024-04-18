package com.intellij.plugins.bodhi.pmd;

import com.intellij.CommonBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.plugins.bodhi.pmd.actions.AnEDTAction;
import com.intellij.plugins.bodhi.pmd.core.*;
import com.intellij.plugins.bodhi.pmd.tree.*;
import com.intellij.pom.Navigatable;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.util.ui.tree.TreeUtil;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.*;

/**
 * The result panel where the PMD results are shown. This includes a toolbar and
 * tree to show the violations found in a pmd run.
 *
 * @author bodhi
 * @version 1.2
 */
public class PMDResultPanel extends JPanel {

    private final JTree resultTree;
    private final PMDProjectComponent projectComponent;
    private PMDRootNode rootNode;
    private PMDErrorBranchNode processingErrorsNode;
    private boolean scrolling;
    private final PMDPopupMenu popupMenu;
    public static final String PMD_SUPPRESSION = "//NOPMD";

    /**
     * Create an instance of the result panel.
     *
     * @param projectComponent The Project Component.
     */
    public PMDResultPanel(final PMDProjectComponent projectComponent) {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.projectComponent = projectComponent;
        setBorder(new EmptyBorder(2, 2, 2, 2));

        // Create the tree which can show tooltips as well.
        resultTree = new JTree() {
            public String getToolTipText(MouseEvent evt) {
                if (getRowForLocation(evt.getX(), evt.getY()) == -1)
                    return null;
                TreePath curPath = getPathForLocation(evt.getX(), evt.getY());
                if (curPath != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)curPath.getLastPathComponent();
                    if (node instanceof BasePMDNode) {
                        return ((BasePMDNode) node).getToolTip();
                    }
                }
                return super.getToolTipText(evt);
            }
        };
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
        JBScrollPane treeScrollPane = new JBScrollPane(resultTree);
        treeScrollPane.setPreferredSize(new Dimension(600, 300));
        add(treeScrollPane);


        JTextArea textArea = new JTextArea("Click on a rule or violation node to see its description with example.");
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JBScrollPane textScrollPane = new JBScrollPane(textArea);

        textScrollPane.setPreferredSize(new Dimension(600, 300));
        add(textScrollPane);

        //Add selection listener to support autoscroll to source.
        resultTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                if (scrolling) {
//                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treeSelectionEvent.getPath().getLastPathComponent();
//                    highlightError(treeNode);

                }
            }
        });

        //Add right-click menu to the tree
        popupMenu = new PMDPopupMenu(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final List<PMDViolation> violations = popupMenu.getViolations();
                if (e.getActionCommand().equals(PMDPopupMenu.SUPPRESS)) {
                    // suppress all selected violations, max 1 per file+line
                    Map<String, PMDViolation> uniqueViolationsMap = new HashMap<>();
                    for (PMDViolation violation : violations) {
                        uniqueViolationsMap.put(violation.getFilename() + ":" + violation.getBeginLine(), violation);
                    }
                    for (PMDViolation violation : uniqueViolationsMap.values()) {
                        //Suppress the violation
                        final Editor editor = openEditor(violation);
                        if (editor != null) {
                            executeWrite(editor, violation);
                        }
                    }
                } else if (e.getActionCommand().equals(PMDPopupMenu.DETAILS)) {
                    // show rule details documentation in browser
                    String url = popupMenu.getDetailsUrl();
                    if (!url.isEmpty()) {
                        BrowserUtil.browse(url);
                    }
                }
            }
        });

        //Add mouse listener to support double click and popup actions.
        resultTree.addMouseListener(new MouseAdapter() {
            //Get the current tree node where the mouse event happened
            private DefaultMutableTreeNode[] getNodeFromEvent(MouseEvent e) {
                TreePath[] selectionPaths = resultTree.getSelectionPaths();
                if (selectionPaths != null) {
                    DefaultMutableTreeNode[] result = new DefaultMutableTreeNode[selectionPaths.length];
                    for (int i = 0; i < result.length; i++) {
                        result[i] = (DefaultMutableTreeNode) selectionPaths[i].getLastPathComponent();
                    }
                    return result;
                }
                return null;
            }

            public void mousePressed(MouseEvent e) {
                DefaultMutableTreeNode[] treeNodes = getNodeFromEvent(e);
                if (treeNodes != null) {
                    DefaultMutableTreeNode node = treeNodes[0];
                    Rule rule = null;
                    String message = "";
                    if (node instanceof HasRule) {
                        rule = ((HasRule) node).getRule();
                        message = rule.getMessage();
                    }
                    if (node instanceof HasMessage) {
                        message = ((HasMessage) node).getMessage();
                    }
                    textArea.setText(getFormattedText(message, rule));
                    textArea.setCaretPosition(0);

                    if (e.getClickCount() == 2) {
                        for (DefaultMutableTreeNode treeNode : treeNodes) {
                            highlightFindingInEditor(treeNode);
                        }
                    }
                    else {
                        showPopup(treeNodes, e);
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                DefaultMutableTreeNode[] treeNodes = getNodeFromEvent(e);
                showPopup(treeNodes, e);
            }
        });
    }

    private static @NotNull String getFormattedText(String message, @Nullable Rule rule) {
        String header = message.trim() + "\n\n";
        String desc = "";
        String examples = "";
        if (rule != null) {
            desc = rule.getDescription().replaceAll("\\s+(Solution: |Note: |Exceptions:)", "\n\n$1");
            desc = desc.replaceAll("([^\\.\\n])\\n", "$1 "); // remove line breaks within a sentence
            desc = desc.replaceAll(" +", " "); // remove multiple spaces
            desc = desc.replace("\\.\\n ", ". ");
            desc = desc.replaceAll("\\s+\\([\\w-]+-rules\\)\\s*", "");
            desc = desc.trim();
            StringBuilder examplesBld = new StringBuilder();
            for (String example : rule.getExamples()) {
                examplesBld.append(example.trim()).append("\n\n");
            }
            if (!rule.getExamples().isEmpty()) {
                examples += "\n\nExample:\n\n" + examplesBld;
            }
        }
        return header + desc + examples;
    }

    /**
     * Displays the right click popup menu for a tree node.
     *
     * @param treeNodes The DefaultMutableTreeNodes where to show the popup
     * @param e the MouseEvent
     */
    private void showPopup(DefaultMutableTreeNode[] treeNodes, MouseEvent e) {
        //Check if it's a popup trigger
        if (treeNodes != null && e.isPopupTrigger()) {
            popupMenu.clearViolationsAndUrl();
            //Only for violation nodes, popups suppress+details are supported
            for (DefaultMutableTreeNode treeNode : treeNodes) {
                //Only for violation nodes, popups suppress+details are supported
                if (treeNode instanceof PMDViolationNode) {
                    popupMenu.addViolation(((PMDViolationNode)treeNode).getPmdViolation());
                }
                // if it has a rule, popup details is supported
                else if (treeNode instanceof HasRule) {
                    popupMenu.setDetailsUrl(((HasRule)treeNode).getRule().getExternalInfoUrl());
                }
            }
            //Display popup
            popupMenu.getMenu().show(resultTree, e.getX(), e.getY());
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
                new Runnable() {
                    public void run() {
                        //All writes must be through a write action.
                        ApplicationManager.getApplication().runWriteAction(new Runnable() {
                            public void run() {
                                int offset = editor.getDocument().getLineEndOffset(result.getBeginLine()-1);
                                //Append PMD special comment to end of line.
                                editor.getDocument().insertString(offset, " " + PMD_SUPPRESSION + " - suppressed " + result.getRuleName() + " - TODO explain reason for suppression");
                            }
                        });
                    }
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
                if (node instanceof Navigatable) {
                    Navigatable navigatable = (Navigatable) node;
                    return navigatable.canNavigate() ? navigatable : null;
                }
                return null;
            }

            public String getNextOccurenceActionName() {
                return UsageViewBundle.message("action.next.occurrence");
            }

            public String getPreviousOccurenceActionName() {
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
                    renderer.renderBody(w, r);
                    return w.getBuffer().toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "";
            }

            @NotNull public String getDefaultFilePath() {
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
    }

    /**
     * Highlights a given violation/suppressed/error represented by the given tree node.
     *
     * @param treeNode The tree node having the violation/suppressed/error
     */
    public void highlightFindingInEditor(DefaultMutableTreeNode treeNode) {
        if (treeNode instanceof Navigatable) {
            ((Navigatable)treeNode).navigate(true);
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
                finding.getFilename().replace(File.separatorChar, '/'));
        if (virtualFile != null) {
            return fileEditorManager.openTextEditor(new OpenFileDescriptor(
                    projectComponent.getCurrentProject(),
                    virtualFile,
                    Math.max(finding.getBeginLine()-1, 0),
                    Math.max(finding.getBeginColumn()-1, 0)),
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
        return (PMDRuleSetNode)addNode(rootNode, new PMDRuleSetNode(name));
    }

    /**
     * Adds a node to the given node as parent.
     *
     * @param parent The parent node
     * @param node The child not which is added as child to parent node
     * @return the child node
     */
    public BasePMDNode addNode(PMDBranchNode parent, BasePMDNode node) {
        parent.add(node);
        reloadResultTree();
        return node;
    }

    public void reloadResultTree() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                ((DefaultTreeModel) resultTree.getModel()).reload();
            }
        });
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
                PMDProjectComponent component = project.getComponent(PMDProjectComponent.class);
                String ruleSetPaths = component.getLastRunRuleSetPaths();
                AnActionEvent action = component.getLastRunAction();
                boolean isCustom = component.isLastRunRulesCustom();
                AnActionEvent actionToRun = (action != null) ? action : e;
                PMDInvoker.getInstance().runPMD(actionToRun, ruleSetPaths, isCustom);
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
                PMDProjectComponent plugin = project.getComponent(PMDProjectComponent.class);
                plugin.closeResultWindow();
            }
        }
    }

}
