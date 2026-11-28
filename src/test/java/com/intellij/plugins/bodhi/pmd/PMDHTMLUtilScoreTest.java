package com.intellij.plugins.bodhi.pmd;

import com.intellij.plugins.bodhi.pmd.core.RuleSetScore;
import com.intellij.plugins.bodhi.pmd.core.RuleSetScoreSpec;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleSetNode;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.SimpleTextAttributes;

import java.awt.*;

public class PMDHTMLUtilScoreTest extends BasePlatformTestCase {

    private static final RuleSetScoreSpec SPEC = RuleSetScoreSpec
            .parse("pmd-score: name=jPinpoint-score; weights=27,9,3,1,0; thresholds=0,5,10,15,25").orElseThrow();

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    public void testScoreHtmlHasColoredLabelAndGrayDetails() {
        RuleSetScore score = RuleSetScore.of(SPEC, new int[]{2, 5, 20, 15, 7}, 12300).orElseThrow(); // D
        String html = PMDHTMLUtil.getScoreHtml(score);

        String label = "jPinpoint-score: D";
        String gradeColor = hex(PMDRuleSetNode.gradeColor(score.gradeIndex()));
        String gray = hex(SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor());

        int labelAt = html.indexOf(label);
        assertTrue(html, labelAt > 0);
        assertTrue("label in grade color: " + html, html.lastIndexOf(gradeColor, labelAt) >= 0);
        int detailsAt = html.indexOf(gray, labelAt);
        assertTrue("details in gray after label: " + html, detailsAt > labelAt);
        assertTrue(html, html.indexOf("(27x2 + 9x5 + 3x20 + 1x15 = 174) / 12.3 kloc = 14.1") > detailsAt);
        assertTrue(html, html.indexOf("A = 0 &lt; B &lt; 5 ≤ C &lt; 10 ≤ <b>D</b> &lt; 15 ≤ E &lt; 25 ≤ F") > detailsAt);
    }
}
