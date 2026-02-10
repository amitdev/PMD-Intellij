package com.intellij.plugins.bodhi.pmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistentData {
    private static final boolean DEFAULT_SKIP_TEST_SRC = true;
    private List<String> customRuleSets;
    private Map<String, String> optionKeyToValue;
    private boolean skipTestSources = DEFAULT_SKIP_TEST_SRC;
    private boolean scanFilesBeforeCheckin;
    private List<String> inEditorAnnotationRules;
    private List<String> excludeRoots;


    public PersistentData() {
        this.customRuleSets = new ArrayList<>();
        this.inEditorAnnotationRules = new ArrayList<>();
        this.optionKeyToValue = new HashMap<>();
        this.excludeRoots = new ArrayList<>();
    }

    public List<String> getCustomRuleSets() {
        return customRuleSets;
    }

    @SuppressWarnings("unused")
    public void setCustomRuleSets(List<String> rules) {
        this.customRuleSets = rules;
    }

    public Map<String, String> getOptionKeyToValue() {
        return optionKeyToValue;
    }

    @SuppressWarnings("unused")
    public void setOptionKeyToValue(Map<String, String> opts) {
        optionKeyToValue = opts;
    }

    public void setSkipTestSources(boolean skip) {
        skipTestSources = skip;
    }

    public boolean isSkipTestSources() {
        return skipTestSources;
    }

    @SuppressWarnings("unused")
    public void setInEditorAnnotationRules(List<String> inEditorAnnotationRules) {
        this.inEditorAnnotationRules = inEditorAnnotationRules;
    }
    public List<String> getInEditorAnnotationRules() {
        return inEditorAnnotationRules;
    }

    public boolean isScanFilesBeforeCheckin() {
        return scanFilesBeforeCheckin;
    }

    public void setScanFilesBeforeCheckin(boolean scan) {
        scanFilesBeforeCheckin = scan;
    }

    public List<String> getExcludeRoots() {
        return excludeRoots;
    }

    @SuppressWarnings("unused")
    public void setExcludeRoots(List<String> excludeRoots) {
        this.excludeRoots = excludeRoots;
    }
}
