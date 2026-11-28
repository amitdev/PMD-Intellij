package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.RuleSetScoreSpec;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class PMDRuleSetNodeTest extends BasePlatformTestCase {

    private static final RuleSetScoreSpec SPEC = RuleSetScoreSpec
            .parse("pmd-score: name=jPinpoint-score; weights=27,9,3,1,0").orElseThrow();

    private static String renderedText(PMDRuleSetNode node) {
        PMDCellRenderer renderer = new PMDCellRenderer();
        node.render(renderer, false);
        return renderer.getCharSequence(false).toString();
    }

    public void testScoreDirectlyAfterViolations() {
        // 3 High violations and 2 suppressed ones; counts faked, real violation nodes need a full PMD report
        PMDRuleSetNode node = new PMDRuleSetNode("jpinpoint-java-rules") {
            @Override
            public synchronized int getViolationCount() {
                return 3;
            }

            @Override
            public synchronized int getSevViolationCount(Severity sev) {
                return sev == Severity.HIGH ? 3 : 0;
            }

            @Override
            public synchronized int getSuppressedCount() {
                return 2;
            }
        };
        node.setScoreInput(SPEC, 1000);
        assertEquals("jpinpoint-java-rules (3 violations: 3)  Score: F (2 suppressed violations)", renderedText(node));
    }

    public void testScoreAfterZeroViolations() {
        PMDRuleSetNode node = new PMDRuleSetNode("jpinpoint-java-rules");
        node.setScoreInput(SPEC, 1000);
        node.calculateCounts();
        assertEquals("jpinpoint-java-rules (0 violations)  Score: A", renderedText(node));
    }

    public void testNoScoreWithoutSpec() {
        PMDRuleSetNode node = new PMDRuleSetNode("other-rules");
        node.calculateCounts();
        assertEquals("other-rules (0 violations)", renderedText(node));
    }
}
