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
    private List<String> customRuleSetPaths = new ArrayList<String>();
    private Map<String, String> options = new HashMap<>();
    private Map<String, Pair<String, AnAction>> customActionsMap = new HashMap<String, Pair<String, AnAction>>();
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

    void updateCustomRulesMenu() {
        PMDCustom actionGroup = (PMDCustom) ActionManager.getInstance().getAction("PMDCustom");
        for (final String rulePath : customRuleSetPaths) {
            if (customActionsMap.containsKey(rulePath)) {
                continue;
            }
            try {
                RuleSet ruleSet = PMDResultCollector.loadRuleSet(rulePath);
                String ruleFileName = PMDUtil.getRuleNameFromPath(rulePath);
                AnAction action = new AnAction(String.format(Locale.ENGLISH, "%s (%s)",ruleSet.getName() , ruleFileName)) {
                    public void actionPerformed(AnActionEvent e) {
                        PMDInvoker.getInstance().runPMD(e, rulePath, true);
                        setLastRunActionAndRules(e, rulePath, true);
                    }
                };
                customActionsMap.put(rulePath, Pair.create(ruleFileName, action));
                actionGroup.add(action);
            } catch (PMDResultCollector.InvalidRuleSetException e) {
                JOptionPane.showMessageDialog(resultPanel,
                                              "The ruleset file is not available or not a valid PMD ruleset:\n"
                                              + e.getMessage(),
                                              "Invalid File", JOptionPane.ERROR_MESSAGE);
            }
        }
        for (Iterator<Map.Entry<String, Pair<String, AnAction>>> iterator = customActionsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Pair<String, AnAction>> entry = iterator.next();
            String rulePath = entry.getKey();
            if (!customRuleSetPaths.contains(rulePath)) {
                actionGroup.remove(entry.getValue().getSecond());
                iterator.remove();
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

    public void projectClosed() {
        //When the project is closed, the result window has to be closed.
        if (toolWindowManager.getToolWindow(TOOL_ID) != null) {
            toolWindowManager.unregisterToolWindow(TOOL_ID);
        }
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
        this.customRuleSetPaths = new ArrayList(customRuleSetPaths);
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
