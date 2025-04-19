package com.intellij.plugins.bodhi.pmd.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Constraints;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.plugins.bodhi.pmd.PMDInvoker;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.PMDUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.*;

/**
 * This ActionGroup defines the actions for pre defined rulesets that
 * comes with PMD. The actions are created dynamically.
 *
 * @author bodhi
 * @version 1.0
 */
public abstract class PreDefinedAbstractClass extends DefaultActionGroup {

    private PMDProjectComponent component;

    public static final String RULESETS_FILENAMES_KEY = "rulesets.filenames";

    /**
     * Loads all the predefined rulesets in PMD and create actions for them.
     */
    PreDefinedAbstractClass(String rulesetFilename) {
        Properties props = new Properties();
        try {
            // Load the property file which has all the rulesets for Java and Kotlin.
            props.load(getRuleResourceStream(rulesetFilename));

            List<String> rulesetFilenames = List.of(props.getProperty(RULESETS_FILENAMES_KEY).split(PMDInvoker.RULE_DELIMITER));

            for (final String ruleFileName : rulesetFilenames) {
                final String ruleName = PMDUtil.getBareFileNameFromPath(ruleFileName);

                // Add rule action to the respective category group
                AnAction ruleAction = new AnAction(ruleName) {
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        PMDInvoker.getInstance().runPMD(e, ruleFileName);
                        getComponent().setLastRunActionAndRules(e, ruleFileName, false);
                    }
                };
                this.add(ruleAction);

                // Ensure an "All" action for the category group if not already added
                if (this.getChildrenCount() == 1) { // First action being added to the group
                    AnAction allAction = new AnAction("All") {
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            String categoryAllRules = String.join(PMDInvoker.RULE_DELIMITER, rulesetFilenames);
                            PMDInvoker.getInstance().runPMD(e, categoryAllRules);
                            getComponent().setLastRunActionAndRules(e, categoryAllRules, false);
                        }
                    };
                    this.add(allAction, Constraints.FIRST);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private @Nullable InputStream getRuleResourceStream(String filePath) {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (resourceAsStream == null) {
            return Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
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
