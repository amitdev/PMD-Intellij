package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vcs.actions.VcsContextFactory;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.plugins.bodhi.pmd.tree.PMDBranchNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDRootNode;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static com.intellij.plugins.bodhi.pmd.filter.VirtualFileFilters.*;

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
    private static final VirtualFileFilter SUPPORTED_EXTENSIONS = or(
            fileHasExtension("java"),
            fileHasExtension("xml"),
            fileHasExtension("cls"),
            fileHasExtension("trigger"),
            fileHasExtension("page"));

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
     * @param ruleSetPaths The ruleSetPath(s) for rules to run
     * @param isCustomRuleSet Is it a custom ruleset or not.
     */
    public void runPMD(AnActionEvent actionEvent, String ruleSetPaths, boolean isCustomRuleSet) {
        //If no ruleSetPath is selected, nothing to do
        if (ruleSetPaths == null || ruleSetPaths.length() == 0) {
            return;
        }
        //Show the tool window
        PMDUtil.getProjectComponent(actionEvent).setupToolWindow();

        Project project = actionEvent.getData(PlatformDataKeys.PROJECT);
        PMDProjectComponent projectComponent = project.getComponent(PMDProjectComponent.class);
        PMDResultPanel resultPanel = projectComponent.getResultPanel();
        PMDRootNode rootNode = resultPanel.getRootNode();

        List<File> files = new LinkedList<File>();
        if (actionEvent.getPlace().equals(ActionPlaces.PROJECT_VIEW_POPUP)
                || actionEvent.getPlace().equals(ActionPlaces.SCOPE_VIEW_POPUP)
                || actionEvent.getPlace().equals(ActionPlaces.CHANGES_VIEW_POPUP)
                || actionEvent.getPlace().equals(ActionPlaces.MAIN_MENU)
        ) {

            //If selected by right-click on file/folder (s)
            VirtualFile[] selectedFiles;
            switch (actionEvent.getPlace()) {
                case ActionPlaces.CHANGES_VIEW_POPUP:
                    selectedFiles = VcsContextFactory.SERVICE.getInstance().createContextOn(actionEvent).getSelectedFiles();
                    break;
                case ActionPlaces.MAIN_MENU:
                    VirtualFile[] contentRoots = ProjectRootManager.getInstance(project).getContentRoots();
                    selectedFiles = VfsUtil.getCommonAncestors(contentRoots);
                    break;
                default:
                    selectedFiles = actionEvent.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
                    break;
            }

            if (selectedFiles == null || selectedFiles.length == 0) {
                //toolWindow.displayErrorMessage("Please select a file to process first");
                rootNode.setFileCount(0);
                return;
            }
            VirtualFileFilter filter = and(SUPPORTED_EXTENSIONS, fileInSources(project));
            if (projectComponent.isSkipTestSources()) {
                filter = and(filter, not(fileInTestSources(project)));
            }
            filter = or(filter, isDirectory());
            for (VirtualFile selectedFile : selectedFiles) {
                //Add all java files recursively
                PMDUtil.listFiles(selectedFile, files, filter, true);
            }
        } else {
            //Run on currently open file in the editor
            VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
            if (selectedFiles.length == 0) {
                //toolWindow.displayErrorMessage("Please select a file to process first");
                rootNode.setFileCount(0);
                return;
            }
            files.add(new File(selectedFiles[0].getPresentableUrl()));
        }

        //Got the files, start processing now
        processFiles(project, ruleSetPaths, files, isCustomRuleSet, projectComponent);
    }

    /**
     * Runs PMD on given files.
     *  @param project the project
     * @param ruleSetPaths The ruleSetPath(s) of rules to run
     * @param files The files on which to run
     * @param isCustomRuleSet Is it a custom ruleset or not.
     * @param projectComponent
     */
    public void processFiles(Project project, final String ruleSetPaths, final List<File> files, final boolean isCustomRuleSet, final PMDProjectComponent projectComponent) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PMDProjectComponent.TOOL_ID);
        toolWindow.activate(null);

        //Save all files
        ApplicationManager.getApplication().saveAll();

        //Run PMD asynchronously
        Runnable runnable = new Runnable() {
            public void run() {
                //Show a progress indicator.
                ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
                String[] ruleSetPathArray = ruleSetPaths.split(RULE_DELIMITER);
                PMDResultPanel resultPanel = projectComponent.getResultPanel();

                PMDRootNode rootNode = resultPanel.getRootNode();
                PMDResultCollector.clearReport();
                rootNode.setFileCount(files.size());
                rootNode.setRuleSetCount(ruleSetPathArray.length);
                rootNode.setRunning(true);
                for (String ruleSetPath : ruleSetPathArray) {
                    //TODO: even better progress
                    progress.setText("Running : " + ruleSetPath + " on " + files.size() + " file(s)");

                    //Create a result collector to get results
                    PMDResultCollector collector = new PMDResultCollector();

                    //Get the tree nodes from result collector
                    List<PMDBranchNode> resultRuleNodes = collector.runPMDAndGetResults(files, ruleSetPath, projectComponent);

                    if (resultRuleNodes.size() != 0) {
                        String ruleSetName;
                        if (isCustomRuleSet) {
                            //For custom rulesets, using a separate format for rendering
                            ruleSetName = PMDUtil.getBareFileNameFromPath(ruleSetPath) + ";" + ruleSetPath;
                        } else {
                            ruleSetName = PMDUtil.getBareFileNameFromPath(ruleSetPath);
                        }
                        PMDBranchNode ruleSetNode = resultPanel.addCreateBranchNodeAtRoot(ruleSetName);
                        //Add all rule nodes to the tree
                        for (PMDBranchNode resultRuleNode : resultRuleNodes) {
                            resultPanel.addNode(ruleSetNode, resultRuleNode);
                        }
                        rootNode.calculateCounts();
                        resultPanel.reloadResultTree();
                    }
                }
                resultPanel.addProcessingErrorsNodeToRootIfHasAny(); // as last node
                rootNode.calculateCounts();
                rootNode.setRunning(false);
                resultPanel.reloadResultTree();
            }
        };
        ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Running PMD", true, project);
    }
}