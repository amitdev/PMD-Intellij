package com.intellij.plugins.bodhi.pmd;

import com.intellij.ide.AutoScrollToSourceOptionProvider;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.ExporterToTextFile;
import com.intellij.ide.OccurenceNavigator;
import com.intellij.ide.OccurenceNavigatorSupport;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.plugins.bodhi.pmd.core.PMDViolation;
import com.intellij.plugins.bodhi.pmd.tree.PMDCellRenderer;
import com.intellij.plugins.bodhi.pmd.tree.PMDPopupMenu;
import com.intellij.plugins.bodhi.pmd.tree.PMDTreeNodeData;
import com.intellij.plugins.bodhi.pmd.tree.PMDTreeNodeFactory;
import com.intellij.pom.Navigatable;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.util.ui.tree.TreeUtil;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.renderers.HTMLRenderer;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TooManyListenersException;

/**
 * The result panel where the PMD results are shown. This includes a toolbar and
 * tree to show the violations found in a pmd run.
 *
 * @author bodhi
 * @version 1.2
 */
public class PMDResultPanel extends JPanel {

    private JTree resultTree;
    private PMDProjectComponent projectComponent;
    private DefaultMutableTreeNode rootNode;
    private boolean scrolling;
    private PMDPopupMenu popupMenu;
    public static final String PMD_SUPPRESSION = "//NOPMD";

    /**
     * Create an instance of the result panel.
     *
     * @param projectComponent The Project Component.
     */
    public PMDResultPanel(final PMDProjectComponent projectComponent) {
        super(new BorderLayout());
        this.projectComponent = projectComponent;
        setBorder(new EmptyBorder(2, 2, 2, 2));

        // Create the tree which can show tooltips as well.
        resultTree = new JTree() {
            public String getToolTipText(MouseEvent evt) {
                if (getRowForLocation(evt.getX(), evt.getY()) == -1)
                    return null;
                TreePath curPath = getPathForLocation(evt.getX(), evt.getY());
                Object userObj = null;
                if (curPath != null) {
                    userObj = ((DefaultMutableTreeNode)curPath.getLastPathComponent()).getUserObject();
                }
                //Only for PMDTreeNodeData we show tool tips.
                if (userObj instanceof PMDTreeNodeData) {
                    return ((PMDTreeNodeData)userObj).getToolTip();
                }
                return super.getToolTipText(evt);
            }
        };
        ToolTipManager.sharedInstance().registerComponent(resultTree);

        //Create the actions of the toolbar and create it.
        ActionGroup actionGrp = createActions();
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(PMDProjectComponent.TOOL_ID, actionGrp, false);
        toolbar.getComponent().setVisible(true);
        add(toolbar.getComponent(), BorderLayout.WEST);

        initializeTree();

        resultTree.setCellRenderer(new PMDCellRenderer());
        TreeUtil.expandAll(resultTree);
        add(new JBScrollPane(resultTree), BorderLayout.CENTER);

        //Add selection listener to support autoscroll to source.
        resultTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                if (scrolling) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treeSelectionEvent.getPath().getLastPathComponent();
                    highlightError(treeNode);
                }
            }
        });

        //Add right-click menu to the tree
        popupMenu = new PMDPopupMenu(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final List<PMDViolation> results = popupMenu.getViolations();
                if (e.getActionCommand().equals(PMDPopupMenu.SUPPRESS)) {
                    Map<String, PMDViolation> unique = new HashMap<>();
                    for (PMDViolation result : results) {
                        unique.put(result.getFilename()+":"+result.getBeginLine(), result);
                    }
                    for (PMDViolation result : unique.values()) {
                        //Suppress the violation
                        final Editor editor = openEditor(result);
                        executeWrite(editor, result);
                    }
                } else if (e.getActionCommand().equals(PMDPopupMenu.DETAILS)) {
                    Set<String> urls = new HashSet<>();
                    for (PMDViolation result : results) {
                        urls.add(result.getExternalUrl());
                    }
                    for (String url : urls) {
                        //Open a browser and show rule details
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
                    if (e.getClickCount() == 2) {
                        for (DefaultMutableTreeNode treeNode : treeNodes) {
                            highlightError(treeNode);
                        }
                    } else {
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

    /**
     * Displays the right click popup menu for a tree node.
     *
     * @param treeNodes The DefaultMutableTreeNodes where to show the popup
     * @param e the MouseEvent
     */
    private void showPopup(DefaultMutableTreeNode[] treeNodes, MouseEvent e) {
        //Check if its a popup trigger
        if (treeNodes != null && e.isPopupTrigger()) {
            popupMenu.clearViolations();
            //Only for violation nodes, popups are supported
            for (int i = 0; i < treeNodes.length; ++i) {
                if (treeNodes[i].getUserObject() instanceof PMDViolation) {
                    PMDViolation pmdViolation = (PMDViolation) treeNodes[i].getUserObject();
                    //Set the violation node
                    popupMenu.addViolation(pmdViolation);
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
                                editor.getDocument().insertString(offset, PMD_SUPPRESSION);
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

        actionGroup.add(OpenApiAdapter.getInstance().createCollapseAllAction(treeExpander, this));
        actionGroup.add(OpenApiAdapter.getInstance().createExpandAllAction(treeExpander, this));

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
                return null;
            }

            public String getPreviousOccurenceActionName() {
                return null;
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

            public void addSettingsChangedListener(ChangeListener listener) throws TooManyListenersException {
            }

            public void removeSettingsChangedListener(ChangeListener listener) {
            }

            public String getReportText() {
                Report r = PMDResultCollector.report;
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

            public String getDefaultFilePath() {
                return "report.html";
            }

            public void exportedTo(String filePath) {
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
     * Highlights a given violation/error represented by the given tree node.
     *
     * @param treeNode The tree node having the violation
     */
    public void highlightError(DefaultMutableTreeNode treeNode) {
        if (treeNode != null) {
            Object obj = treeNode.getUserObject();
            if (obj instanceof PMDViolation) {
                openEditor((PMDViolation) obj);
            }
        }
    }

    /**
     * Opens the given violation's file in the Editor and returns the Editor.
     *
     * @param result The Violation
     * @return the editor with caret at the violation
     */
    private Editor openEditor(PMDViolation result) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(projectComponent.getCurrentProject());
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(
                result.getFilename().replace(File.separatorChar, '/'));
        if (virtualFile != null) {
            return fileEditorManager.openTextEditor(new OpenFileDescriptor(
                    projectComponent.getCurrentProject(),
                    virtualFile,
                    Math.max(result.getBeginLine()-1, 0),
                    Math.max(result.getBeginColumn()-1, 0)),
                    true);
        }
        return null;
    }

    /**
     * Adds a node to the tree as a direct child of the root, and return it
     *
     * @param nodeValue the user object of the tree node to create
     * @return the created node
     */
    public DefaultMutableTreeNode addNode(Object nodeValue) {
        return addNode(rootNode, PMDTreeNodeFactory.getInstance().createNode(nodeValue));
    }

    /**
     * Adds a node to the given node as parent.
     *
     * @param parent The parent node
     * @param node The child not which is added as child to parent node
     * @return the child node
     */
    public DefaultMutableTreeNode addNode(DefaultMutableTreeNode parent, DefaultMutableTreeNode node) {
        parent.add(node);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                ((DefaultTreeModel) resultTree.getModel()).reload();
            }
        });
        return node;
    }


    /**
     * Get the root node of the violation tree.
     *
     * @return the root node
     */
    public DefaultMutableTreeNode getRootNode() {
        return rootNode;
    }

    /**
     * Inner class for rerun action.
     */
    private class ReRunAction extends AnAction {
        public ReRunAction() {
            super(UsageViewBundle.message("action.rerun"), UsageViewBundle.message("action.description.rerun"), IconLoader.getIcon("/actions/refreshUsages.png"));
            registerCustomShortcutSet(CommonShortcuts.getRerun(), PMDResultPanel.this);
        }

        public void actionPerformed(AnActionEvent e) {
            Project project = e.getData(PlatformDataKeys.PROJECT);
            //Run the last run rule
            if (project != null) {
                PMDProjectComponent component = project.getComponent(PMDProjectComponent.class);
                String rule = component.getLastRunRules();
                AnActionEvent action = component.getLastRunAction();
                boolean isCustom = component.isLastRunRulesCustom();
                AnActionEvent actionToRun = (action != null) ? action : e;
                PMDInvoker.getInstance().runPMD(actionToRun, rule, isCustom);
                resultTree.repaint();
            }
        }
    }

    /**
     * Inner class for close action.
     */
    private class CloseAction extends AnAction {
        private static final String ACTION_CLOSE = "action.close";

        private CloseAction() {
            super(UsageViewBundle.message(ACTION_CLOSE), null, IconLoader.getIcon("/actions/cancel.png"));
        }

        public void actionPerformed(AnActionEvent e) {
            Project project = e.getData(PlatformDataKeys.PROJECT);
            if (project != null) {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PMDProjectComponent.TOOL_ID);
                toolWindow.activate(null);
                PMDProjectComponent plugin = project.getComponent(PMDProjectComponent.class);
                plugin.closeResultWindow();
            }
        }
    }

}
