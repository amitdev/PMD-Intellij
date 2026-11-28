package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;

/**
 * The leaves showing a sustainability level (grade index 0..5):
 * 0 = 3 fresh leaves (best), 1 = 2 fresh, 2 = 1 fresh, 3 = 1 withered, 4 = 2 withered, 5 = 3 withered (worst).
 */
public final class SustainabilityLeaves {

    private static final String FRESH = "/icons/leafFresh";
    private static final String WITHERED = "/icons/leafWithered";

    private static final Color WITHERED_COLOR = new JBColor(new Color(139, 90, 43), new Color(185, 133, 82));

    private SustainabilityLeaves() {
    }

    public static boolean isFresh(int level) {
        return level <= 2;
    }

    public static int count(int level) {
        return isFresh(level) ? 3 - level : level - 2;
    }

    /**
     * @return the leaf icon of the level; IconLoader picks the dark variant in a dark theme
     */
    public static Icon icon(int level) {
        return IconLoader.getIcon((isFresh(level) ? FRESH : WITHERED) + ".svg", SustainabilityLeaves.class);
    }

    /**
     * @return the class path resource of the leaf SVG of the level, e.g. "/icons/leafWithered_dark.svg"
     */
    public static String svgResource(int level, boolean dark) {
        return (isFresh(level) ? FRESH : WITHERED) + (dark ? "_dark" : "") + ".svg";
    }

    /**
     * @return the text color matching the leaves of the level: green for fresh, brown for withered
     */
    public static Color labelColor(int level) {
        return isFresh(level) ? PMDRuleSetNode.gradeColor(0) : WITHERED_COLOR;
    }
}
