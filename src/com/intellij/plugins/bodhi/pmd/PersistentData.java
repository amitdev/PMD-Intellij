package com.intellij.plugins.bodhi.pmd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistentData {
    public List<String> customRuleSets = new ArrayList<String>();
    public Map<String, String> options = new HashMap<String, String>();
    public String skipTestSources;

    public PersistentData() {
        this.customRuleSets = new ArrayList<String>();
        this.options = new HashMap<String, String>();
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

    public void skipTestSources(boolean skipTestSources)
    {
        this.skipTestSources = String.valueOf(skipTestSources);
    }

    public boolean isSkipTestSources()
    {
        return Boolean.valueOf(skipTestSources);
    }
}
