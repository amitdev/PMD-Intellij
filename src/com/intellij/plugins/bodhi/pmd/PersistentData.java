package com.intellij.plugins.bodhi.pmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistentData {
    private static final boolean DEFAULT_SKIP_TEST_SRC = true;
    private List<String> customRuleSets;
    private Map<String, String> options;
    private boolean skipTestSources = DEFAULT_SKIP_TEST_SRC;
    private boolean scanFilesBeforeCheckin;

    public PersistentData() {
        this.customRuleSets = new ArrayList<>();
        this.options = new HashMap<>();
    }

    public List<String> getCustomRuleSets() {
        return customRuleSets;
    }

    public void setCustomRuleSets(List<String> rules) {
        this.customRuleSets = rules;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> opts) {
        options = opts;
    }

    public void setSkipTestSources(boolean skip) {
        skipTestSources = skip;
    }

    public boolean isSkipTestSources() {
        return skipTestSources;
    }

    public boolean isScanFilesBeforeCheckin() {
        return scanFilesBeforeCheckin;
    }

    public void setScanFilesBeforeCheckin(boolean scan) {
        scanFilesBeforeCheckin = scan;
    }

}
