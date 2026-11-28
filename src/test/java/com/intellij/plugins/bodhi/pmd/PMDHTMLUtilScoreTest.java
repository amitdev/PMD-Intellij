package com.intellij.plugins.bodhi.pmd;

import com.intellij.plugins.bodhi.pmd.core.RuleSetScore;
import com.intellij.plugins.bodhi.pmd.core.RuleSetScoreSpec;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleSetNode;
import com.intellij.plugins.bodhi.pmd.tree.SustainabilityLeaves;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.SimpleTextAttributes;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;

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

    private static final RuleSetScoreSpec SUSTAINABILITY = RuleSetScoreSpec
            .parseSustainability("pmd-sustainability: weights=9,3,1").orElseThrow();

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    private static String base64Of(String resource) throws IOException {
        try (InputStream in = SustainabilityLeaves.class.getResourceAsStream(resource)) {
            return Base64.getEncoder().encodeToString(in.readAllBytes());
        }
    }

    public void testSustainabilityHtmlHasColoredLabelWithLeavesAndGrayDetails() throws IOException {
        RuleSetScore score = RuleSetScore.of(SUSTAINABILITY, new int[]{3, 10, 10}, 12300).orElseThrow(); // 5.4 -> level 2
        String html = PMDHTMLUtil.getSustainabilityHtml(score, false);

        int labelAt = html.indexOf("Sustainability-score:");
        assertTrue(html, labelAt > 0);
        assertTrue("label in leaf color: " + html, html.lastIndexOf(hex(SustainabilityLeaves.labelColor(2)), labelAt) >= 0);
        String gray = hex(SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor());
        int detailsAt = html.indexOf(gray, labelAt);
        assertTrue("details in gray after label: " + html, detailsAt > labelAt);
        assertTrue(html, html.indexOf("(9x3 + 3x10 + 1x10 = 67) / 12.3 kloc = 5.4") > detailsAt);
        // label: 1 leaf of level 2; scale: 3+2+1+1+2+3 leaves
        assertEquals(1 + 12, count(html, "<img"));
        assertEquals("one framed current level", 1, count(html, "border:"));
        assertTrue(html.contains(base64Of(SustainabilityLeaves.svgResource(2, false))));
        assertTrue(html.contains(base64Of(SustainabilityLeaves.svgResource(5, false))));
    }

    public void testSustainabilityHtmlUsesDarkLeavesInDarkTheme() throws IOException {
        RuleSetScore score = RuleSetScore.of(SUSTAINABILITY, new int[]{3, 10, 10}, 12300).orElseThrow();
        String html = PMDHTMLUtil.getSustainabilityHtml(score, true);
        assertTrue(html.contains(base64Of(SustainabilityLeaves.svgResource(2, true))));
        assertFalse(html.contains(base64Of(SustainabilityLeaves.svgResource(2, false))));
    }

    public void testScoreDetailsHtmlShowsScoreBeforeSustainability() {
        RuleSetScore score = RuleSetScore.of(SPEC, new int[]{2, 5, 20, 15, 7}, 12300).orElseThrow();
        RuleSetScore sustainability = RuleSetScore.of(SUSTAINABILITY, new int[]{3, 10, 10}, 12300).orElseThrow();
        String html = PMDHTMLUtil.getScoreDetailsHtml(Optional.of(score), Optional.of(sustainability));
        int scoreAt = html.indexOf("jPinpoint-score: D");
        assertTrue(html, scoreAt >= 0);
        assertTrue(html, html.indexOf("Sustainability-score:") > scoreAt);
    }

    public void testScoreDetailsHtmlWithOnlySustainability() {
        RuleSetScore sustainability = RuleSetScore.of(SUSTAINABILITY, new int[]{3, 10, 10}, 12300).orElseThrow();
        String html = PMDHTMLUtil.getScoreDetailsHtml(Optional.empty(), Optional.of(sustainability));
        assertTrue(html, html.contains("Sustainability-score:"));
        assertFalse(html, html.contains("jPinpoint-score"));
    }
}
