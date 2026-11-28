package com.intellij.plugins.bodhi.pmd.tree;

import org.junit.Test;

import static org.junit.Assert.*;

public class SustainabilityLeavesTest {

    @Test
    public void leafCountPerLevel() {
        int[] expected = {3, 2, 1, 1, 2, 3};
        for (int level = 0; level < 6; level++) {
            assertEquals("level " + level, expected[level], SustainabilityLeaves.count(level));
        }
    }

    @Test
    public void freshForBestThreeLevels() {
        for (int level = 0; level < 6; level++) {
            assertEquals("level " + level, level <= 2, SustainabilityLeaves.isFresh(level));
        }
    }

    @Test
    public void svgResourcePerLevelAndTheme() {
        assertEquals("/icons/leafWithered_dark.svg", SustainabilityLeaves.svgResource(4, true));
        assertEquals("/icons/leafFresh.svg", SustainabilityLeaves.svgResource(0, false));
        for (int level : new int[]{0, 5}) {
            for (boolean dark : new boolean[]{false, true}) {
                assertNotNull(SustainabilityLeaves.svgResource(level, dark),
                        SustainabilityLeaves.class.getResource(SustainabilityLeaves.svgResource(level, dark)));
            }
        }
    }
}
