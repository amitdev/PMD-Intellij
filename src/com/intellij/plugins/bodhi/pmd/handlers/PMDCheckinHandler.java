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
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleNode;
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
import javax.swing.tree.DefaultMutableTreeNode;
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

        List<DefaultMutableTreeNode> results = new ArrayList<>();
        for (String ruleSet : plugin.getCustomRuleSets()) {
            DefaultMutableTreeNode result = scanFiles(ruleSet, plugin);
            if (result != null) {
                results.add(result);
            }
        }
        return processScanResults(results, project);
    }

    private DefaultMutableTreeNode scanFiles(String ruleSet, PMDProjectComponent plugin) {
        DefaultMutableTreeNode result = null;
        PMDResultCollector collector = new PMDResultCollector(true);
        List<File> files = new ArrayList<>(checkinProjectPanel.getFiles());

        List<DefaultMutableTreeNode> ruleSetResults = collector.getResults(files, ruleSet, plugin.getOptions());
        if (!ruleSetResults.isEmpty()) {
            result = createRuleSetNode(ruleSet, ruleSetResults);
        }
        return result;
    }

    private DefaultMutableTreeNode createRuleSetNode(String ruleSet, List<DefaultMutableTreeNode> results) {
        ruleSet = PMDUtil.getRuleNameFromPath(ruleSet) + ";" + ruleSet;
        DefaultMutableTreeNode ruleSetNode = PMDTreeNodeFactory.getInstance().createNode(ruleSet);

        int childCount = 0;
        for (DefaultMutableTreeNode pmdResult : results) {
            childCount += ((PMDRuleNode) pmdResult.getUserObject()).getChildCount();
            ruleSetNode.add(pmdResult);
        }
        ((PMDRuleNode) ruleSetNode.getUserObject()).addChildren(childCount);
        return ruleSetNode;
    }

    @NotNull
    private ReturnResult processScanResults(List<DefaultMutableTreeNode> results, Project project) {
        int violations = toViolations(results);
        if (violations > 0) {
            int answer = promptUser(project, violations);
            if (answer == Messages.OK) {
                showToolWindow(results, project);
                return ReturnResult.CLOSE_WINDOW;
            }
            if (answer == Messages.CANCEL || answer == -1) {
                return ReturnResult.CANCEL;
            }
        }
        return ReturnResult.COMMIT;
    }

    private int toViolations(List<DefaultMutableTreeNode> results) {
        int violations = 0;
        for (DefaultMutableTreeNode result : results) {
            violations += ((PMDRuleNode) result.getUserObject()).getChildCount();
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

    private void showToolWindow(List<DefaultMutableTreeNode> results, Project project) {
        PMDProjectComponent plugin = project.getComponent(PMDProjectComponent.class);
        PMDResultPanel resultPanel = plugin.getResultPanel();
        plugin.setupToolWindow();

        DefaultMutableTreeNode rootNode = resultPanel.getRootNode();
        PMDRuleNode rootNodeData = ((PMDRuleNode) rootNode.getUserObject());
        for (DefaultMutableTreeNode node : results) {
            PMDRuleNode nodeData = (PMDRuleNode) node.getUserObject();
            resultPanel.addNode(rootNode, node);
            rootNodeData.addChildren(nodeData.getChildCount());
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PMDProjectComponent.TOOL_ID);
        toolWindow.activate(null);
        plugin.setLastRunRules(StringUtils.join(plugin.getCustomRuleSets(), PMDInvoker.RULE_DELIMITER));
    }
}
