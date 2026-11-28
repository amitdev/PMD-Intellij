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
        int scaleAt = html.indexOf("A = 0 &lt; B &lt; 5 ≤ C &lt; 10 ≤ <span style='border: 1px solid " + gray);
        assertTrue("current grade framed: " + html, scaleAt > detailsAt);
        assertTrue(html, html.indexOf(";'>D</span> &lt; 15 ≤ E &lt; 25 ≤ F", scaleAt) > scaleAt);
        assertEquals("one framed current grade", 1, count(html, "border:"));
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
        String html = PMDHTMLUtil.getScoreDetailsHtml(null, Optional.of(score), Optional.of(sustainability));
        int scoreAt = html.indexOf("jPinpoint-score: D");
        assertTrue(html, scoreAt >= 0);
        assertTrue(html, html.indexOf("Sustainability-score:") > scoreAt);
    }

    public void testScoreDetailsHtmlWithOnlySustainability() {
        RuleSetScore sustainability = RuleSetScore.of(SUSTAINABILITY, new int[]{3, 10, 10}, 12300).orElseThrow();
        String html = PMDHTMLUtil.getScoreDetailsHtml(null, Optional.empty(), Optional.of(sustainability));
        assertTrue(html, html.contains("Sustainability-score:"));
        assertFalse(html, html.contains("jPinpoint-score"));
    }

    public void testLegendLeavesAreGrayLabelLeavesInColor() {
        RuleSetScore score = RuleSetScore.of(SUSTAINABILITY, new int[]{3, 10, 10}, 12300).orElseThrow(); // level 2
        String html = PMDHTMLUtil.getSustainabilityHtml(score, false);
        assertEquals("only the 12 legend leaves are gray", 12, count(html, "grayscale"));
        String label = html.substring(0, html.indexOf("</div>"));
        assertFalse("label leaves in color: " + label, label.contains("grayscale"));
    }

    private static PMDRuleSetNode ruleSetNode(String name, RuleSetScoreSpec score, RuleSetScoreSpec sustainability) {
        PMDRuleSetNode node = new PMDRuleSetNode(name);
        node.setScoreInput(score, sustainability, 1000);
        node.calculateCounts();
        return node;
    }

    public void testRuleSetScoresHtmlWithoutLegendAndCalculation() {
        String html = PMDHTMLUtil.getRuleSetScoresHtml(java.util.List.of(
                ruleSetNode("jpinpoint-java-rules", SPEC, SUSTAINABILITY),
                ruleSetNode("jpinpoint-kotlin-rules", null, SUSTAINABILITY)));
        int javaAt = html.indexOf("jpinpoint-java-rules");
        int kotlinAt = html.indexOf("jpinpoint-kotlin-rules");
        assertTrue(html, javaAt >= 0 && kotlinAt > javaAt);
        int scoreAt = html.indexOf("jPinpoint-score: A");
        assertTrue(html, scoreAt > javaAt && scoreAt < kotlinAt);
        assertEquals(html, 2, count(html, "Sustainability-score:"));
        assertFalse("no calculation: " + html, html.contains("kloc"));
        assertFalse("rule set name not bold: " + html, html.contains("<strong>"));
        assertFalse("no legend: " + html, html.contains("&lt;"));
        assertFalse("no legend: " + html, html.contains("border:"));
    }

    public void testRuleSetScoresHtmlSkipsRuleSetsWithoutScore() {
        String html = PMDHTMLUtil.getRuleSetScoresHtml(java.util.List.of(
                ruleSetNode("other-rules", null, null),
                ruleSetNode("jpinpoint-java-rules", SPEC, null)));
        assertFalse(html, html.contains("other-rules"));
        assertTrue(html, html.contains("jpinpoint-java-rules"));
        assertEquals("", PMDHTMLUtil.getRuleSetScoresHtml(java.util.List.of(ruleSetNode("other-rules", null, null))));
    }

    public void testScoreDetailsHtmlStartsWithPlainRuleSetDescription() {
        RuleSetScore score = RuleSetScore.of(SPEC, new int[]{2, 5, 20, 15, 7}, 12300).orElseThrow();
        String html = PMDHTMLUtil.getScoreDetailsHtml("Rules for <performance> & sustainability.",
                Optional.of(score), Optional.empty());
        String description = "<p>Rules for &lt;performance&gt; &amp; sustainability.</p>";
        int descriptionAt = html.indexOf(description);
        assertTrue(html, descriptionAt >= 0);
        assertTrue("description first: " + html, descriptionAt < html.indexOf("jPinpoint-score: D"));
        assertFalse(html, html.startsWith("<p style") || html.contains("<strong>"));
    }

    public void testScoreDetailsHtmlWithoutDescription() {
        RuleSetScore score = RuleSetScore.of(SPEC, new int[]{2, 5, 20, 15, 7}, 12300).orElseThrow();
        assertTrue(PMDHTMLUtil.getScoreDetailsHtml("", Optional.of(score), Optional.empty()).startsWith("<div class=\"title\""));
    }
}
