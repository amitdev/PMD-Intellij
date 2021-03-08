package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.plugins.bodhi.pmd.actions.PMDCustom;
import com.intellij.plugins.bodhi.pmd.actions.PreDefinedMenuGroup;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import net.sourceforge.pmd.RuleSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 *
 * This is the Project Component of the PMD Plugin.
 *
 * @author bodhi
 * @version 1.0
 */

@State(
  name = "PDMPlugin",
  storages = {
    @Storage(
      file = "$PROJECT_FILE$"
    )}
)
public class PMDProjectComponent implements ProjectComponent, PersistentStateComponent<PersistentData> {

    /**
     * The Tool ID of the results panel.
     */
    public static final String TOOL_ID = "PMD";

    private static final String COMPONENT_NAME = "PMDProjectComponent";

    private Project currentProject;
    private PMDResultPanel resultPanel;
    private ToolWindow resultWindow;
    private String lastRunRules;
    private boolean lastRunRulesCustom;
    private AnActionEvent lastRunActionEvent;
    private Set<String> customRuleSetPaths = new LinkedHashSet<>(); // avoid duplicates, maintain order
    private Map<String, String> options = new HashMap<>();
    private ToolWindowManager toolWindowManager;
    private boolean skipTestSources;
    private boolean scanFilesBeforeCheckin;

    /**
     * Creates a PMD Project component based on the project given.
     *
     * @param project The project on which to create the component.
     */
    public PMDProjectComponent(Project project) {
        this.currentProject = project;
        toolWindowManager = ToolWindowManager.getInstance(currentProject);
    }

    public void initComponent() {
        //Add custom rules as menu items if defined.
        updateCustomRulesMenu();
        ActionGroup actionGroup = registerActions("PMDPredefined");
        if (actionGroup != null)
            ((PreDefinedMenuGroup) actionGroup).setComponent(this);
        registerActions("PMDCustom");
    }

    private ActionGroup registerActions(String actionName) {
        ActionManager actionMgr = ActionManager.getInstance();
        ActionGroup actionGroup = (ActionGroup) actionMgr.getAction(actionName);
        if (actionGroup != null) {
            for (AnAction act : actionGroup.getChildren(null)) {
                String actName = "PMD" + act.getTemplatePresentation().getText();
                if (actionMgr.getAction(actName) == null)
                    actionMgr.registerAction(actName, act);
            }
        }
        return actionGroup;
    }

    /**
     * Return a Map from rule path to label where label is the filename, except if contains duplicates: more of the path is included.
     * @param paths the rule paths
     * @return a Map from rule path to label
     */
//    private Map<String, String> getRulePathToLabel(Iterable<String> paths) {
//        Map pathToLabel = new HashMap<String, String>();
//        boolean duplicates = false;
//        List fileNames = new ArrayList();
//        for (String path : paths) {
//            String fileName = PMDUtil.getBareFileNameFromPath(path);
//            if (fileNames.contains(fileName)) {
//                duplicates = true;
//                break;
//            }
//            fileNames.add(fileName);
//        }
//        for (String path : paths) {
//            String label = (duplicates) ? PMDUtil.getExtendedFileNameFromPath(path) : PMDUtil.getFileNameFromPath(path);
//            pathToLabel.put(path, label);
//        }
//        return pathToLabel;
//    }

    private boolean hasDuplicateBareFileName(Iterable<String> paths)    {
        boolean duplicate = false;
        List fileNames = new ArrayList();
        for (String path : paths) {
            String fileName = PMDUtil.getBareFileNameFromPath(path);
            if (fileNames.contains(fileName)) {
                duplicate = true;
                break;
            }
            fileNames.add(fileName);
        }
        return duplicate;
    }

    /**
     * Reflect customRuleSetPaths into actionGroup (ActionManager singleton instance)
     */
    void updateCustomRulesMenu() {
        PMDCustom actionGroup = (PMDCustom) ActionManager.getInstance().getAction("PMDCustom");
        actionGroup.removeAll(); // start clean
        boolean hasDuplicate = hasDuplicateBareFileName(customRuleSetPaths);
        for (final String rulePath : customRuleSetPaths) {
            try {
                RuleSet ruleSet = PMDResultCollector.loadRuleSet(rulePath);
                String ruleSetName = ruleSet.getName(); // from the xml
                String extFileName = PMDUtil.getExtendedFileNameFromPath(rulePath);
                String bareFileName = PMDUtil.getBareFileNameFromPath(rulePath);
                String actionText = ruleSetName;
                if (!ruleSetName.equals(bareFileName) || hasDuplicate) {
                    actionText += " (" + extFileName + ")";
                }
                AnAction action = new AnAction(actionText) {
                    public void actionPerformed(AnActionEvent e) {
                        PMDInvoker.getInstance().runPMD(e, rulePath, true);
                        setLastRunActionAndRules(e, rulePath, true);
                    }
                };
                actionGroup.add(action);
            } catch (PMDResultCollector.InvalidRuleSetException e) {
                JOptionPane.showMessageDialog(resultPanel,
                        "The ruleset file is not available or not a valid PMD ruleset:\n" + e.getMessage(),
                        "Invalid File", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void disposeComponent() {
    }

    @NonNls
    @NotNull
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    public void projectOpened() {
        resultPanel = new PMDResultPanel(this);
    }

    /**
     * Registers a tool window for showing PMD results.
     */
    private void registerToolWindow() {
        if (toolWindowManager.getToolWindow(TOOL_ID) == null) {
            resultWindow = toolWindowManager.registerToolWindow(TOOL_ID, true, ToolWindowAnchor.BOTTOM);
            Content content = ContentFactory.SERVICE.getInstance().createContent(resultPanel, "", false);
            resultWindow.getContentManager().addContent(content);
            resultWindow.setType(ToolWindowType.DOCKED, null);
        }
    }

    /**
     * Gets the result panel where the PMD results are shown.
     *
     * @return The panel where results are shown.
     */
    public PMDResultPanel getResultPanel() {
        return resultPanel;
    }

    /**
     * Set up the tool window and initializes the result tree.
     */
    public void setupToolWindow() {
        registerToolWindow();
        resultPanel.initializeTree();
    }

    /**
     * Close the result panel and unregister the tool window.
     */
    public void closeResultWindow() {
        resultWindow.hide(null);
        resultPanel.initializeTree();
        if (toolWindowManager.getToolWindow(TOOL_ID) != null)
            toolWindowManager.unregisterToolWindow(TOOL_ID);
    }

    /**
     * Get the current project.
     *
     * @return the current project
     */
    public Project getCurrentProject() {
        return currentProject;
    }

    /**
     * Get the last run PMD rules on this project.
     *
     * @return the last run rules.
     */
    public String getLastRunRules() {
        return lastRunRules;
    }

    /**
     * Return whether the last run PMD rules on this project are custom rules.
     *
     * @return whether the last run rules are custom rules.
     */
    public boolean isLastRunRulesCustom() {
        return lastRunRulesCustom;
    }

    /**
     * Get the last run action event on this project.
     *
     * @return the last run action.
     */
    public AnActionEvent getLastRunAction() {
        return lastRunActionEvent;
    }
    /**
     * Set the last run action event and PMD rule(s). Multiple rules should be delimited by
     * PMDInvoker.RULE_DELIMITER.
     * @param lastActionEvent the last run action event
     * @param lastRunRules The last run rule name
     * @param isCustom whether the last run rules are custom rules
     */
    public void setLastRunActionAndRules(AnActionEvent lastActionEvent, String lastRunRules, boolean isCustom) {
        this.lastRunRules = lastRunRules;
        this.lastRunActionEvent = lastActionEvent;
        this.lastRunRulesCustom = isCustom;
    }

    public List<String> getCustomRuleSets() {
        return new ArrayList(customRuleSetPaths);
    }

    public void setCustomRuleSets(List<String> customRuleSetPaths) {
        this.customRuleSetPaths = new LinkedHashSet(customRuleSetPaths);
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    @NotNull
    public PersistentData getState() {
        final PersistentData pd = new PersistentData();
        for (String item : customRuleSetPaths) {
            pd.getCustomRuleSets().add(item);
        }
        for (String key : options.keySet()) {
            pd.getOptions().put(key, options.get(key));
        }
        pd.skipTestSources(skipTestSources);
        pd.setScanFilesBeforeCheckin(scanFilesBeforeCheckin);
        return pd;
    }

    public void loadState(PersistentData state) {
        customRuleSetPaths.clear();
        options.clear();
        for (String item : state.getCustomRuleSets()) {
            customRuleSetPaths.add(item);
        }
        for (String key : state.getOptions().keySet()) {
            options.put(key, state.getOptions().get(key));
        }
        this.skipTestSources = state.isSkipTestSources();
        this.scanFilesBeforeCheckin = state.isScanFilesBeforeCheckin();
    }

    public void skipTestSources(boolean skipTestSources)
    {
        this.skipTestSources = skipTestSources;
    }

    public boolean isSkipTestSources()
    {
        return skipTestSources;
    }

    public void setScanFilesBeforeCheckin(boolean scanFilesBeforeCheckin) {
        this.scanFilesBeforeCheckin = scanFilesBeforeCheckin;
    }

    public boolean isScanFilesBeforeCheckin() {
        return scanFilesBeforeCheckin;
    }

}
