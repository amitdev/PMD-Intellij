package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleNode;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Invokes PMD using the PMDResultCollector and gets results from that. This acts as a
 * bridge between the UI and the PMD APIs. It is a singleton.
 *
 * @author bodhi
 * @version 1.0
 */
public class PMDInvoker {

    /**
     * The delimiter used for delimiting multiple rules.
     */
    public static final String RULE_DELIMITER = ",";

    // The singleton instance
    private static final PMDInvoker instance = new PMDInvoker();

    /**
     * Prevents instantiation by other classes.
     */
    private PMDInvoker() {
    }

    /**
     * Get the singletone instance.
     *
     * @return the singleton instance
     */
    public static PMDInvoker getInstance() {
        return instance;
    }

    /**
     * Runs PMD based on the given parameters, and populates result.
     *
     * @param actionEvent The action event that triggered run
     * @param rule The rule(s) to run
     * @param isCustomRuleSet Is it a custom ruleset or not.
     */
    public void runPMD(AnActionEvent actionEvent, String rule, boolean isCustomRuleSet) {
        //If no rule is selected, nothing to do
        if (rule == null || rule.length() == 0) {
            return;
        }
        //Show the tool window
        PMDUtil.getProjectComponent(actionEvent).setupToolWindow();

        List<File> files = new LinkedList<File>();
        if (actionEvent.getPlace().equals(ActionPlaces.PROJECT_VIEW_POPUP)) {
            //If selected by right-click on file/folder (s)
            VirtualFile[] selectedFiles = actionEvent.getData(DataKeys.VIRTUAL_FILE_ARRAY);
            if (selectedFiles.length == 0) {
                //toolWindow.displayErrorMessage("Please select a file to process first");
                return;
            }
            for (int i = 0; i < selectedFiles.length; i++) {
                //Add all java files recursively
                PMDUtil.listFiles(new File(selectedFiles[i].getPresentableUrl()), files,
                        PMDUtil.createFileExtensionFilter("java"));
            }
        } else if (actionEvent.getPlace().equals("ChangesViewPopup")) {
            VirtualFile[] selectedFiles = VcsContextFactory.SERVICE.getInstance().createContextOn(actionEvent).getSelectedFiles();
            if (selectedFiles.length == 0) {
                //toolWindow.displayErrorMessage("Please select a file to process first");
                return;
            }
            for (int i = 0; i < selectedFiles.length; i++) {
                //Add all java files recursively
                PMDUtil.listFiles(new File(selectedFiles[i].getPresentableUrl()), files,
                        PMDUtil.createFileExtensionFilter("java"));
            }            
        } else {
            //Run on currently open file in the editor
            Project project = actionEvent.getData(DataKeys.PROJECT);
            VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selectedFiles.length == 0) {
                //toolWindow.displayErrorMessage("Please select a file to process first");
                return;
            }
            files.add(new File(selectedFiles[0].getPresentableUrl()));
        }

        //Got the files, start processing now
        processFiles(actionEvent, rule, files, isCustomRuleSet);
    }

    /**
     * Runs PMD on given files.
     *
     * @param event The action event that triggered run
     * @param rule The rule(s) to run
     * @param files The files on which to run
     * @param isCustomRuleSet Is it a custom ruleset or not.
     */
    private void processFiles(AnActionEvent event, final String rule, final List<File> files, final boolean isCustomRuleSet) {
        //Activate tool window
        final Project project = event.getData(DataKeys.PROJECT);
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PMDProjectComponent.TOOL_ID);
        toolWindow.activate(null);

        //Save all files
        ApplicationManager.getApplication().saveAll();

        final PMDProjectComponent component = project.getComponent(PMDProjectComponent.class);
        //Run PMD asynchronously
        Runnable runnable = new Runnable() {
            public void run() {
                //Show a progressindicator.
                ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
                String[] rules = rule.split(RULE_DELIMITER);
                PMDResultPanel resultPanel = component.getResultPanel();

                PMDRuleNode rootNodeData = ((PMDRuleNode) resultPanel.getRootNode().getUserObject());
                PMDResultCollector.report = null;
                for (int i = 0; i < rules.length; i++) {
                    //TODO: better progress
                    progress.setText("Running : " + rules[i]);

                    //Create a result collector to get results
                    PMDResultCollector collector = new PMDResultCollector(isCustomRuleSet);

                    //Get the tree nodes from result collector
                    List<DefaultMutableTreeNode> results = collector.getResults(files, rules[i], component.getOptions());

                    if (results.size() != 0) {
                        if (isCustomRuleSet) {
                            //For custom rulesets, using a separate format for rendering
                            rules[i] = PMDUtil.getRuleNameFromPath(rules[i]) + ";" + rules[i];
                        }
                        DefaultMutableTreeNode node = resultPanel.addNode(rules[i]);
                        //Add all nodes to the tree
                        int childCount = 0;
                        for (DefaultMutableTreeNode pmdResult : results) {
                            resultPanel.addNode(node, pmdResult);
                            childCount += ((PMDRuleNode)pmdResult.getUserObject()).getChildCount();
                        }
                        ((PMDRuleNode)node.getUserObject()).addChildren(childCount);
                        rootNodeData.addChildren(childCount);
                    }
                }
            }
        };
        Object[] params = new Object[] {runnable, "Running PMD", true, project};
        OpenApiAdapter.getInstance().runProcessWithProgressSynchronously(params);
    }
}