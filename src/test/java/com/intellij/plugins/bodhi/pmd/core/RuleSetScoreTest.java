package com.intellij.plugins.bodhi.pmd.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class RuleSetScoreTest {

    private static final RuleSetScoreSpec JPINPOINT = RuleSetScoreSpec
            .parse("pmd-score: name=jPinpoint-score; weights=27,9,3,1,0; thresholds=0,5,10,15,25").orElseThrow();

    @Test
    public void computesExampleFromSpec() {
        RuleSetScore score = RuleSetScore.of(JPINPOINT, new int[]{2, 5, 20, 15, 7}, 12300).orElseThrow();
        assertEquals(174, score.weightedSum());
        assertEquals("D", score.grade());
        assertEquals("jPinpoint-score: D", score.gradeLabel());
        assertEquals("(27x2 + 9x5 + 3x20 + 1x15 = 174) / 12.3 kloc = 14.1", score.formula());
    }

    @Test
    public void zeroViolationsIsA() {
        RuleSetScore score = RuleSetScore.of(JPINPOINT, new int[]{0, 0, 0, 0, 0}, 500).orElseThrow();
        assertEquals(0.0, score.score(), 0.0);
        assertEquals("A", score.grade());
        assertEquals("(27x0 + 9x0 + 3x0 + 1x0 = 0) / 0.5 kloc = 0.0", score.formula());
    }

    @Test
    public void noCodeLinesGivesEmpty() {
        assertTrue(RuleSetScore.of(JPINPOINT, new int[]{1, 0, 0, 0, 0}, 0).isEmpty());
    }

    @Test
    public void gradeScaleMarksCurrentGrade() {
        RuleSetScore score = RuleSetScore.of(JPINPOINT, new int[]{2, 5, 20, 15, 7}, 12300).orElseThrow();
        assertEquals("A = 0 < B < 5 ≤ C < 10 ≤ [D] < 15 ≤ E < 25 ≤ F", score.gradeScale(g -> "[" + g + "]"));
    }

    @Test
    public void gradeScaleWithNonZeroFirstThresholdAndDecimals() {
        RuleSetScoreSpec spec = RuleSetScoreSpec.parse("pmd-score: weights=1; thresholds=1,2.5").orElseThrow();
        RuleSetScore score = RuleSetScore.of(spec, new int[]{0, 0, 0, 0, 0}, 1000).orElseThrow();
        assertEquals("[A] ≤ 1 < B < 2.5 ≤ C", score.gradeScale(g -> "[" + g + "]"));
    }
}
