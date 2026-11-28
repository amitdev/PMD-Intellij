package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.SustainabilityLevel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PMDBranchNodeSustainabilityTest {

    /** Leaf counting one violation of the given level; real violation nodes need a full PMD report. */
    private static PMDLeafNode leafWith(SustainabilityLevel level) {
        return new PMDLeafNode() {
            @Override
            public int getSustainabilityViolationCount(SustainabilityLevel l) {
                return l == level ? 1 : 0;
            }

            @Override
            public String getToolTip() {
                return null;
            }

            @Override
            public void render(PMDCellRenderer cellRenderer, boolean expanded) {
            }

            @Override
            public void navigate(boolean requestFocus) {
            }
        };
    }

    @Test
    public void aggregatesSustainabilityCountsFromChildren() {
        PMDRuleSetNode node = new PMDRuleSetNode("rules");
        node.add(leafWith(SustainabilityLevel.HIGH));
        node.add(leafWith(SustainabilityLevel.LOW));
        node.calculateCounts();
        assertEquals(1, node.getSustainabilityViolationCount(SustainabilityLevel.HIGH));
        assertEquals(0, node.getSustainabilityViolationCount(SustainabilityLevel.MEDIUM));
        assertEquals(1, node.getSustainabilityViolationCount(SustainabilityLevel.LOW));
    }
}
