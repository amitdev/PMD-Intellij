package com.intellij.plugins.bodhi.pmd.handlers;

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.plugins.bodhi.pmd.PMDInvoker;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.PMDResultPanel;
import com.intellij.plugins.bodhi.pmd.PMDUtil;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.plugins.bodhi.pmd.tree.PMDBranchNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDRootNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDTreeNodeFactory;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.UIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PMDCheckinHandler extends CheckinHandler {

    private static final Log log = LogFactory.getLog(PMDCheckinHandler.class);
    private static final String BUNDLE = "com.intellij.plugins.bodhi.pmd.PMD-Intellij";

    @NonNls
    private final CheckinProjectPanel checkinProjectPanel;

    /* default */ PMDCheckinHandler(CheckinProjectPanel checkinProjectPanel) {
        this.checkinProjectPanel = checkinProjectPanel;
    }

    @Nullable
    @Override
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        final JCheckBox checkBox = new JCheckBox(message("handler.before.checkin.checkbox"));

        Project project = checkinProjectPanel.getProject();
        final PMDProjectComponent projectComponent = project.getComponent(PMDProjectComponent.class);

        return new RefreshableOnComponent() {
            @Override
            public JComponent getComponent() {
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(checkBox);
                return panel;
            }

            @Override
            public void refresh() {
            }

            @Override
            public void saveState() {
                projectComponent.setScanFilesBeforeCheckin(checkBox.isSelected());
            }

            @Override
            public void restoreState() {
                checkBox.setSelected(projectComponent.isScanFilesBeforeCheckin());
            }
        };
    }

    @NotNull
    private String message(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
        return CommonBundle.message(ResourceBundle.getBundle(BUNDLE), key, params);
    }

    @Override
    public ReturnResult beforeCheckin(@Nullable CommitExecutor executor,
                                      PairConsumer<Object, Object> additionalDataConsumer) {
        Project project = checkinProjectPanel.getProject();
        if (project == null) {
            log.error("Could not get project for check-in panel, skipping");
            return ReturnResult.COMMIT;
        }

        PMDProjectComponent plugin = project.getComponent(PMDProjectComponent.class);
        if (plugin == null) {
            log.error("Could not find the PMD plugin, skipping");
            return ReturnResult.COMMIT;
        }

        if (!plugin.isScanFilesBeforeCheckin()) {
            return ReturnResult.COMMIT;
        }

        PMDResultCollector.report = null;

        List<PMDBranchNode> ruleSetResultNodes = new ArrayList<>();
        for (String ruleSetPath : plugin.getCustomRuleSetPaths()) {
            PMDBranchNode ruleSetResultNode = scanFiles(ruleSetPath, plugin);
            if (ruleSetResultNode != null) {
                ruleSetResultNodes.add(ruleSetResultNode);
            }
        }
        return processScanResults(ruleSetResultNodes, project);
    }

    private PMDBranchNode scanFiles(String ruleSetPath, PMDProjectComponent plugin) {
        PMDBranchNode ruleSetResultNode = null;
        PMDResultCollector collector = new PMDResultCollector();
        List<File> files = new ArrayList<>(checkinProjectPanel.getFiles());

        List<PMDBranchNode> ruleSetResultNodes = collector.runPMDAndGetResults(files, ruleSetPath, plugin);
        if (!ruleSetResultNodes.isEmpty()) {
            ruleSetResultNode = createRuleSetNodeWithResults(ruleSetPath, ruleSetResultNodes);
        }
        return ruleSetResultNode;
    }

    private PMDBranchNode createRuleSetNodeWithResults(String ruleSetPath, List<PMDBranchNode> ruleResultNodes) {
        ruleSetPath = PMDUtil.getFileNameFromPath(ruleSetPath) + ";" + ruleSetPath;
        PMDBranchNode ruleSetNode = PMDTreeNodeFactory.getInstance().createBranchNode(ruleSetPath);

        for (PMDBranchNode ruleResultNode : ruleResultNodes) {
            ruleSetNode.add(ruleResultNode);
        }
        return ruleSetNode;
    }

    @NotNull
    private ReturnResult processScanResults(List<PMDBranchNode> ruleSetResultNodes, Project project) {
        int violations = toViolations(ruleSetResultNodes);
        if (violations > 0) {
            int answer = promptUser(project, violations);
            if (answer == Messages.OK) {
                showToolWindow(ruleSetResultNodes, project);
                return ReturnResult.CLOSE_WINDOW;
            }
            if (answer == Messages.CANCEL || answer == -1) {
                return ReturnResult.CANCEL;
            }
        }
        return ReturnResult.COMMIT;
    }

    private int toViolations(List<PMDBranchNode> ruleSetResultNodes) {
        int violations = 0;
        for (PMDBranchNode ruleSetResultNode : ruleSetResultNodes) {
            violations += ruleSetResultNode.getViolationCount();
        }
        return violations;
    }

    private int promptUser(Project project, int violations) {
        String[] buttons = new String[]{message("handler.before.checkin.error.review"),
                checkinProjectPanel.getCommitActionName(),
                CommonBundle.getCancelButtonText()};

        return Messages.showDialog(project, message("handler.before.checkin.error.text", violations),
                message("handler.before.checkin.error.title"), buttons, 0, UIUtil.getWarningIcon());
    }

    private void showToolWindow(List<PMDBranchNode> ruleSetResultNodes, Project project) {
        PMDProjectComponent plugin = project.getComponent(PMDProjectComponent.class);
        PMDResultPanel resultPanel = plugin.getResultPanel();
        plugin.setupToolWindow();

        PMDRootNode rootNode = resultPanel.getRootNode();
        for (PMDBranchNode ruleSetNode : ruleSetResultNodes) {
            resultPanel.addNode(rootNode, ruleSetNode);
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PMDProjectComponent.TOOL_ID);
        toolWindow.activate(null);
        plugin.setLastRunActionAndRules(null, StringUtils.join(plugin.getCustomRuleSetPaths(), PMDInvoker.RULE_DELIMITER), true);
    }
}
