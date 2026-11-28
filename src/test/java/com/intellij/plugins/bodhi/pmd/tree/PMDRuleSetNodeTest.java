package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.RuleSetScoreSpec;
import com.intellij.plugins.bodhi.pmd.core.SustainabilityLevel;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class PMDRuleSetNodeTest extends BasePlatformTestCase {

    private static final RuleSetScoreSpec SCORE = RuleSetScoreSpec
            .parse("pmd-score: name=jPinpoint-score; weights=27,9,3,1,0").orElseThrow();
    private static final RuleSetScoreSpec SUSTAINABILITY = RuleSetScoreSpec
            .parseSustainability("pmd-sustainability: weights=9,3,1").orElseThrow();

    private PMDCellRenderer renderer;

    private String renderedText(PMDRuleSetNode node) {
        renderer = new PMDCellRenderer();
        node.render(renderer, false);
        return renderer.getCharSequence(false).toString();
    }

    /** 3 High violations of sustainability-high rules and 2 suppressed ones; real violation nodes need a full PMD report. */
    private static PMDRuleSetNode nodeWithFakeCounts() {
        return new PMDRuleSetNode("jpinpoint-java-rules") {
            @Override
            public synchronized int getViolationCount() {
                return 3;
            }

            @Override
            public synchronized int getSevViolationCount(Severity sev) {
                return sev == Severity.HIGH ? 3 : 0;
            }

            @Override
            public synchronized int getSustainabilityViolationCount(SustainabilityLevel level) {
                return level == SustainabilityLevel.HIGH ? 3 : 0;
            }

            @Override
            public synchronized int getSuppressedCount() {
                return 2;
            }
        };
    }

    public void testScoreDirectlyAfterViolations() {
        PMDRuleSetNode node = nodeWithFakeCounts();
        node.setScoreInput(SCORE, null, 1000);
        assertEquals("jpinpoint-java-rules (3 violations: 3)  Score: F (2 suppressed violations)", renderedText(node));
        assertEquals(-1, renderer.getLeavesLevel());
    }

    public void testLeavesDirectlyAfterScore() {
        PMDRuleSetNode node = nodeWithFakeCounts();
        node.setScoreInput(SCORE, SUSTAINABILITY, 1000); // 9x3 / 1 kloc = 27 -> worst level
        assertTrue(renderedText(node), renderedText(node).matches(
                "jpinpoint-java-rules \\(3 violations: 3\\)  Score: F {2,} \\(2 suppressed violations\\)"));
        assertEquals(5, renderer.getLeavesLevel());
    }

    public void testOnlyLeavesWithoutScore() {
        PMDRuleSetNode node = new PMDRuleSetNode("jpinpoint-java-rules");
        node.setScoreInput(null, SUSTAINABILITY, 1000);
        node.calculateCounts();
        assertTrue(renderedText(node), renderedText(node).matches("jpinpoint-java-rules \\(0 violations\\) {2,}"));
        assertEquals(0, renderer.getLeavesLevel());
        assertTrue(node.getSustainabilityScore().isPresent());
        assertTrue(node.getScore().isEmpty());
    }

    public void testScoreAfterZeroViolations() {
        PMDRuleSetNode node = new PMDRuleSetNode("jpinpoint-java-rules");
        node.setScoreInput(SCORE, null, 1000);
        node.calculateCounts();
        assertEquals("jpinpoint-java-rules (0 violations)  Score: A", renderedText(node));
    }

    public void testNoScoreWithoutSpec() {
        PMDRuleSetNode node = new PMDRuleSetNode("other-rules");
        node.calculateCounts();
        assertEquals("other-rules (0 violations)", renderedText(node));
        assertEquals(-1, renderer.getLeavesLevel());
    }

    /** Reference: the first x of the leaves placeholder, scanning pixel by pixel. */
    private static int leavesXByScan(PMDCellRenderer renderer, int fragmentIndex) {
        for (int x = 0; x < renderer.getWidth(); x++) {
            if (renderer.findFragmentAt(x) == fragmentIndex) {
                return x;
            }
        }
        return -1;
    }

    public void testLeavesPositionFoundLikePixelScan() {
        for (boolean withScore : new boolean[]{true, false}) {
            PMDRuleSetNode node = nodeWithFakeCounts();
            node.setScoreInput(withScore ? SCORE : null, SUSTAINABILITY, 1000);
            renderedText(node);
            renderer.setIcon(com.intellij.icons.AllIcons.Nodes.Folder);
            renderer.setSize(800, 20);
            int expected = leavesXByScan(renderer, renderer.getLeavesFragmentIndex());
            assertTrue("placeholder found by scan", expected > 0);
            assertEquals("with score: " + withScore, expected, renderer.findLeavesX());
        }
    }

    public void testLeavesPositionFoundWhenLeavesAreLastInRow() {
        PMDRuleSetNode node = new PMDRuleSetNode("jpinpoint-java-rules");
        node.setScoreInput(null, SUSTAINABILITY, 1000);
        node.calculateCounts();
        renderedText(node); // "jpinpoint-java-rules (0 violations)" + leaves, nothing after
        renderer.setIcon(com.intellij.icons.AllIcons.Nodes.Folder);
        renderer.setSize(800, 20);
        int expected = leavesXByScan(renderer, renderer.getLeavesFragmentIndex());
        assertTrue("placeholder found by scan", expected > 0);
        assertEquals(expected, renderer.findLeavesX());
    }
}
