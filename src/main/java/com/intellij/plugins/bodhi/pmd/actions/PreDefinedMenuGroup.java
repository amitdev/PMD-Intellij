package com.intellij.plugins.bodhi.pmd.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.plugins.bodhi.pmd.PMDInvoker;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.PMDUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Properties;

/**
 * This ActionGroup defines the actions for pre defined rulesets that
 * comes with PMD. The actions are created dynamically.
 *
 * @author bodhi
 * @version 1.0
 */
public class PreDefinedMenuGroup extends DefaultActionGroup {

    // A string that represents all the rulesets as comma separated value.
    private static String allRules = "";

    private PMDProjectComponent component;

    //The ruleset property file which lists all the predefined rulesets
    public static final String RULESETS_PROPERTY_FILE = "category/java/categories.properties";
    public static final String RULESETS_FILENAMES_KEY = "rulesets.filenames";

    /**
     * Loads all the predefined rulesets in PMD and create actions for them.
     */
    public PreDefinedMenuGroup() {
        AnAction action = new AnAction("All") {
            public void actionPerformed(@NotNull AnActionEvent e) {
                PMDInvoker.getInstance().runPMD(e, allRules);
                getComponent().setLastRunActionAndRules(e, allRules, false);
            }
        };
        Properties props = new Properties();
        try {
            //Load the property file which has all the rulesets.
            props.load(getRuleResourceStream());
            String[] rulesetFilenames = props.getProperty(RULESETS_FILENAMES_KEY).split(PMDInvoker.RULE_DELIMITER);

            //We have 'All' rules in addition to the rulesets
            add(action);

            StringBuilder allRulesBuilder = new StringBuilder();

            for (int i = 0; i < rulesetFilenames.length; ++i) {
                final String ruleFileName = rulesetFilenames[i];
                final String ruleName = PMDUtil.getBareFileNameFromPath(ruleFileName);

                allRulesBuilder.append(ruleFileName);
                if (i < rulesetFilenames.length - 1) {
                    allRulesBuilder.append(PMDInvoker.RULE_DELIMITER);
                }

                AnAction ruleAction = new AnAction(ruleName) {
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        PMDInvoker.getInstance().runPMD(e, ruleFileName);
                        getComponent().setLastRunActionAndRules(e, ruleFileName, false);
                    }
                };
                add(ruleAction);
            }
            allRules = allRulesBuilder.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private @Nullable InputStream getRuleResourceStream() {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(RULESETS_PROPERTY_FILE);
        if (resourceAsStream == null) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(RULESETS_PROPERTY_FILE);
        }
        return resourceAsStream;
    }

    public void setComponent(PMDProjectComponent component) {
        this.component = component;
    }

    public PMDProjectComponent getComponent() {
        return component;
    }
}
