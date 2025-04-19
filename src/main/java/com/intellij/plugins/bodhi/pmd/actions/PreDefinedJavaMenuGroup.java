package com.intellij.plugins.bodhi.pmd.actions;

public class PreDefinedJavaMenuGroup extends PreDefinedAbstractClass {

    // The ruleset property file which lists all the predefined rulesets
    public static final String RULESETS_JAVA_PROPERTY_FILE = "category/java/categories.properties";

    public PreDefinedJavaMenuGroup() {
        super(RULESETS_JAVA_PROPERTY_FILE);
    }

}
