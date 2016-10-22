package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.plugins.bodhi.pmd.actions.PMDCustom;
import com.intellij.plugins.bodhi.pmd.actions.PreDefinedMenuGroup;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

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
      id = "other",
      file = "$PROJECT_FILE$"
    )}
)
public class PMDProjectComponent implements ProjectComponent, PersistentStateComponent<PersistentData> {

    /**
     * The Tool ID of the results panel.
     */
    static final String TOOL_ID = "PMD";

    private static final String COMPONENT_NAME = "PMDProjectComponent";

    private Project currentProject;
    private PMDResultPanel resultPanel;
    private ToolWindow resultWindow;
    private String lastRunRules;
    private List<String> customRuleSets = new ArrayList<String>();
    private Map<String, String> options = new HashMap<String, String>();
    private Map<String, Pair<String, AnAction>> customActionsMap = new HashMap<String, Pair<String, AnAction>>();
    private ToolWindowManager toolWindowManager;
    private boolean skipTestSources;

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
        for (final String rulePath : customRuleSets) {
            String ruleName = PMDUtil.getRuleNameFromPath(rulePath);
            if (!customActionsMap.containsKey(rulePath)) {
                AnAction action = new AnAction(ruleName) {
                    public void actionPerformed(AnActionEvent e) {
                        PMDInvoker.getInstance().runPMD(e, rulePath, true);
                        setLastRunRules(rulePath);
                    }
                };
                customActionsMap.put(rulePath, Pair.create(ruleName, action));
                actionGroup.add(action);
            }
        }
        for (Iterator<Map.Entry<String, Pair<String, AnAction>>> iterator = customActionsMap.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, Pair<String, AnAction>> entry = iterator.next();
            if (!customRuleSets.contains(entry.getKey())) {
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
     * Set the last run PMD rule(s). Multiple rules should be delimited by
     * PMDInvoker.RULE_DELIMITER.
     * @param lastRunRules The last run rule name
     */
    public void setLastRunRules(String lastRunRules) {
        this.lastRunRules = lastRunRules;
    }

    public List<String> getCustomRuleSets() {
        return customRuleSets;
    }

    public void setCustomRuleSets(List<String> customRuleSets) {
        this.customRuleSets = customRuleSets;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options;
    }

    public PersistentData getState() {
        final PersistentData pd = new PersistentData();
        for (String item : customRuleSets) {
            pd.getCustomRuleSets().add(item);
        }
        for (String key : options.keySet()) {
            pd.getOptions().put(key, options.get(key));
        }
        pd.skipTestSources(skipTestSources);
        return pd;
    }

    public void loadState(PersistentData state) {
        customRuleSets.clear();
        options.clear();
        for (String item : state.getCustomRuleSets()) {
            customRuleSets.add(item);
        }
        for (String key : state.getOptions().keySet()) {
            options.put(key, state.getOptions().get(key));
        }
        this.skipTestSources = state.isSkipTestSources();
    }

    public void skipTestSources(boolean skipTestSources)
    {
        this.skipTestSources = skipTestSources;
    }

    public boolean isSkipTestSources()
    {
        return skipTestSources;
    }
}
