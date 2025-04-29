package com.intellij.plugins.bodhi.pmd.actions;

public class PreDefinedKotlinMenuGroup extends PreDefinedAbstractClass {

    // The ruleset property file which lists all the predefined rulesets
    public static final String RULESETS_KOTLIN_PROPERTY_FILE = "category/kotlin/categories.properties";

    public PreDefinedKotlinMenuGroup() {
        super(RULESETS_KOTLIN_PROPERTY_FILE);
    }

}
