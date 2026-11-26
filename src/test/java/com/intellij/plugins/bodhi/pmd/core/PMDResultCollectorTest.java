package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.plugins.bodhi.pmd.tree.PMDRuleSetEntryNode;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class PMDResultCollectorTest {

    /**
     * Scanning a selection without supported source files (e.g. an .iml file) gives no files;
     * the caller sorts the result, so it must be a mutable list.
     */
    @Test
    public void noFilesGivesSortableEmptyResult() {
        List<PMDRuleSetEntryNode> result = new PMDResultCollector().runPMDAndGetResults(List.of(), "rulesets/any.xml", null, null);
        result.sort(null);
        assertTrue(result.isEmpty());
    }
}
