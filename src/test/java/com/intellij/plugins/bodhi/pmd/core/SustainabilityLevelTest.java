package com.intellij.plugins.bodhi.pmd.core;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class SustainabilityLevelTest {

    @Test
    public void readsLevelFromTags() {
        assertEquals(Optional.of(SustainabilityLevel.MEDIUM), SustainabilityLevel.ofTags("jpinpoint-rule,memory,sustainability-medium"));
    }

    @Test
    public void highestLevelWins() {
        assertEquals(Optional.of(SustainabilityLevel.HIGH), SustainabilityLevel.ofTags("sustainability-low, sustainability-high"));
    }

    @Test
    public void noSustainabilityTagGivesEmpty() {
        assertEquals(Optional.empty(), SustainabilityLevel.ofTags("memory"));
        assertEquals(Optional.empty(), SustainabilityLevel.ofTags(""));
        assertEquals(Optional.empty(), SustainabilityLevel.ofTags(null));
    }
}
