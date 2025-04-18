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
public class PreDefinedMenuGroup extends DefaultActionGroup {

    private PMDProjectComponent component;

    //The ruleset property file which lists all the predefined rulesets
    public static final String RULESETS_PROPERTY_JAVA_FILE = "category/java/categories.properties";
    public static final String RULESETS_PROPERTY_KOTLIN_FILE = "category/kotlin/categories.properties";
    public static final String RULESETS_FILENAMES_KEY = "rulesets.filenames";

    /**
     * Loads all the predefined rulesets in PMD and create actions for them.
     */
    public PreDefinedMenuGroup() {
        Properties propsJava = new Properties();
        Properties propsKotlin = new Properties();
        try {
            // Load the property file which has all the rulesets for Java and Kotlin.
            propsJava.load(getRuleResourceStream(RULESETS_PROPERTY_JAVA_FILE));
            propsKotlin.load(getRuleResourceStream(RULESETS_PROPERTY_KOTLIN_FILE));

            List<String> javaRules = List.of(propsJava.getProperty(RULESETS_FILENAMES_KEY).split(PMDInvoker.RULE_DELIMITER));
            List<String> kotlinRules = List.of(propsKotlin.getProperty(RULESETS_FILENAMES_KEY).split(PMDInvoker.RULE_DELIMITER));
            
            Map<String, List<String>> categoryToRulesMap = new HashMap<>();
            categoryToRulesMap.put("java", javaRules);
            categoryToRulesMap.put("kotlin", kotlinRules);

            List<String> rulesetFilenames = new ArrayList<>();
            rulesetFilenames.addAll(javaRules);
            rulesetFilenames.addAll(kotlinRules);

            // Group rules by category for menu usage
            Map<String, DefaultActionGroup> categoryGroups = new HashMap<>();

            for (final String ruleFileName : rulesetFilenames) {
                final String ruleName = PMDUtil.getBareFileNameFromPath(ruleFileName);
                final String categoryName = PMDUtil.getCategoryNameFromPath(ruleFileName);

                // Create category group if not already created
                categoryGroups.computeIfAbsent(categoryName, k -> {
                    DefaultActionGroup categoryGroup = new DefaultActionGroup(categoryName, true);
                    add(categoryGroup);
                    return categoryGroup;
                });

                DefaultActionGroup categoryGroup = categoryGroups.get(categoryName);

                // Add rule action to the respective category group
                AnAction ruleAction = new AnAction(ruleName) {
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        PMDInvoker.getInstance().runPMD(e, ruleFileName);
                        getComponent().setLastRunActionAndRules(e, ruleFileName, false);
                    }
                };
                categoryGroup.add(ruleAction);

                // Ensure an "All" action for the category group if not already added
                if (categoryGroup.getChildrenCount() == 1) { // First action being added to the group
                    AnAction allAction = new AnAction("All") {
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            String categoryAllRules = String.join(PMDInvoker.RULE_DELIMITER, categoryToRulesMap.get(categoryName));
                            PMDInvoker.getInstance().runPMD(e, categoryAllRules);
                            getComponent().setLastRunActionAndRules(e, categoryAllRules, false);
                        }
                    };
                    categoryGroup.add(allAction, Constraints.FIRST);
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
