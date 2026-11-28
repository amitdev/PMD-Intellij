package com.intellij.plugins.bodhi.pmd.core;

import org.junit.Test;

import static org.junit.Assert.*;

public class RuleSetScoreSpecTest {

    private static final double[] DEFAULT_THRESHOLDS = {0, 5, 10, 15, 25};

    private static RuleSetScoreSpec parse(String description) {
        return RuleSetScoreSpec.parse(description).orElseThrow();
    }

    @Test
    public void parsesFullMarker() {
        RuleSetScoreSpec spec = parse("jPinpoint rules\npmd-score: name=jPinpoint-score; weights=27,9,3,1,0; thresholds=0,5,10,15,25");
        assertEquals("jPinpoint-score", spec.name());
        assertArrayEquals(new int[]{27, 9, 3, 1, 0}, spec.weights());
        assertArrayEquals(DEFAULT_THRESHOLDS, spec.thresholds(), 0.0);
    }

    @Test
    public void appliesDefaults() {
        RuleSetScoreSpec spec = parse("pmd-score: weights=27,9");
        assertEquals("score", spec.name());
        assertArrayEquals(new int[]{27, 9, 0, 0, 0}, spec.weights());
        assertArrayEquals(DEFAULT_THRESHOLDS, spec.thresholds(), 0.0);
    }

    @Test
    public void toleratesSpacesAndCase() {
        RuleSetScoreSpec spec = parse("pmd-score:  Name = x ;  Weights = 27, 9, 3");
        assertEquals("x", spec.name());
        assertArrayEquals(new int[]{27, 9, 3, 0, 0}, spec.weights());
    }

    @Test
    public void noMarkerGivesEmpty() {
        assertTrue(RuleSetScoreSpec.parse("just rules").isEmpty());
        assertTrue(RuleSetScoreSpec.parse(null).isEmpty());
    }

    @Test
    public void malformedGivesEmpty() {
        assertTrue(RuleSetScoreSpec.parse("pmd-score: weights=a,b").isEmpty());
        assertTrue(RuleSetScoreSpec.parse("pmd-score: name=x").isEmpty());
        assertTrue(RuleSetScoreSpec.parse("pmd-score: weights=1,2,3,4,5,6").isEmpty());
        assertTrue(RuleSetScoreSpec.parse("pmd-score: weights=1; thresholds=10,5").isEmpty());
    }

    @Test
    public void grades() {
        RuleSetScoreSpec spec = parse("pmd-score: weights=1");
        assertEquals("A", spec.grade(0.0));
        assertEquals("B", spec.grade(0.1));
        assertEquals("B", spec.grade(4.99));
        assertEquals("C", spec.grade(5.0));
        assertEquals("D", spec.grade(14.1));
        assertEquals("E", spec.grade(24.9));
        assertEquals("F", spec.grade(25.0));
        assertEquals("F", spec.grade(1000));
    }

    @Test
    public void stripsMarker() {
        assertEquals("line1\nline3", RuleSetScoreSpec.stripMarker("line1\npmd-score: weights=1\nline3"));
        assertEquals("", RuleSetScoreSpec.stripMarker("pmd-score: weights=1"));
        assertNull(RuleSetScoreSpec.stripMarker(null));
    }

    @Test
    public void parsesSustainabilityMarkerWithDefaults() {
        RuleSetScoreSpec spec = RuleSetScoreSpec.parseSustainability("rules\npmd-sustainability: weights=9,3,1").orElseThrow();
        assertEquals("Sustainability-score", spec.name());
        assertArrayEquals(new int[]{9, 3, 1}, spec.weights());
        assertArrayEquals(DEFAULT_THRESHOLDS, spec.thresholds(), 0.0);
    }

    @Test
    public void sustainabilityMalformedGivesEmpty() {
        assertTrue(RuleSetScoreSpec.parseSustainability("pmd-sustainability: weights=1,2,3,4").isEmpty());
        assertTrue(RuleSetScoreSpec.parseSustainability("pmd-sustainability: weights=1; thresholds=0,5,10,15").isEmpty());
        assertTrue(RuleSetScoreSpec.parseSustainability("pmd-sustainability: weights=1; thresholds=0,5,10,15,25,30").isEmpty());
    }

    @Test
    public void markersDoNotMix() {
        assertTrue(RuleSetScoreSpec.parse("pmd-sustainability: weights=9").isEmpty());
        assertTrue(RuleSetScoreSpec.parseSustainability("pmd-score: weights=9").isEmpty());
    }

    @Test
    public void stripsBothMarkers() {
        assertEquals("a\nb", RuleSetScoreSpec.stripMarker("a\npmd-score: weights=1\npmd-sustainability: weights=1\nb"));
    }

    @Test
    public void stripsUpgradeHintAtEndOfLine() {
        assertEquals("jPinpoint rules for responsible Kotlin coding, sponsored by Rabobank.",
                RuleSetScoreSpec.stripMarker("""
                        jPinpoint rules for responsible Kotlin coding, sponsored by Rabobank. Upgrade your plugin to see scores.
                        pmd-score: weights=27,9,3,1,0
                        pmd-sustainability: weights=9,3,1
                        """));
    }

    @Test
    public void stripsUpgradeHintOnOwnLine() {
        assertEquals("Some rules.\nMore text.",
                RuleSetScoreSpec.stripMarker("Some rules.\n  upgrade your plugin to see scores  \nMore text."));
    }

    @Test
    public void specsWithSameContentAreEqual() {
        String marker = "pmd-score: name=x; weights=27,9,3,1,0; thresholds=0,5,10,15,25";
        assertEquals(parse(marker), parse(marker));
        assertEquals(parse(marker).hashCode(), parse(marker).hashCode());
        assertNotEquals(parse(marker), parse("pmd-score: name=x; weights=27,9,3,1,1"));
    }

    @Test
    public void defaultThresholdsAreNotShared() {
        parse("pmd-score: weights=1").thresholds()[1] = 99;
        assertArrayEquals(DEFAULT_THRESHOLDS, parse("pmd-score: weights=1").thresholds(), 0.0);
    }

    @Test
    public void specIsSerializable() {
        assertTrue(java.io.Serializable.class.isInstance(parse("pmd-score: weights=1")));
    }
}
