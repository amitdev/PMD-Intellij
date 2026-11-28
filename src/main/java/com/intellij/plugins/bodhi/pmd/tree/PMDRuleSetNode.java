package com.intellij.plugins.bodhi.pmd.tree;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import com.intellij.plugins.bodhi.pmd.core.RuleSetScore;
import com.intellij.plugins.bodhi.pmd.core.RuleSetScoreSpec;
import com.intellij.plugins.bodhi.pmd.core.SustainabilityLevel;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Optional;

import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.STYLE_BOLD;
import static com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN;

public class PMDRuleSetNode extends PMDBranchNode {

    private static final Color GOOD_GRADE_COLOR = new JBColor(new Color(0, 128, 0), new Color(98, 200, 98));

    @GuardedBy("this")
    private @Nullable RuleSetScoreSpec scoreSpec;
    @GuardedBy("this")
    private @Nullable RuleSetScoreSpec sustainabilitySpec;
    @GuardedBy("this")
    private int codeLines;

    /**
     * Create a node with the given value as node name.
     *
     * @param nodeName The node name to set.
     */
    public PMDRuleSetNode(String nodeName) {
        super(nodeName);
    }

    /**
     * Sets what is needed to render the rule set scores.
     *
     * @param scoreSpec          the score definition of the rule set, null for no score
     * @param sustainabilitySpec the sustainability score definition of the rule set, null for none
     * @param codeLines          the number of non-comment code lines scanned by the rule set
     */
    public synchronized void setScoreInput(@Nullable RuleSetScoreSpec scoreSpec, @Nullable RuleSetScoreSpec sustainabilitySpec, int codeLines) {
        this.scoreSpec = scoreSpec;
        this.sustainabilitySpec = sustainabilitySpec;
        this.codeLines = codeLines;
    }

    @Override
    public synchronized void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.append(getNodeName());
        if (getViolationCount() > 0 ) {
            cellRenderer.append(" (" + getViolationCount() + " violation" + ((getViolationCount() == 1) ? ": " : "s: "), GRAYED_ATTRIBUTES);
            boolean first = true;
            for (Severity sev : Severity.values()) {
                int count = getSevViolationCount(sev);
                if (count > 0) {
                    if (!first) {
                        cellRenderer.append(" + ", GRAYED_ATTRIBUTES);
                    }
                    cellRenderer.append(Integer.toString(count), new SimpleTextAttributes(STYLE_PLAIN, sev.getColor()));
                    first = false;
                }
            }
            cellRenderer.append(")", GRAYED_ATTRIBUTES);
            renderScore(cellRenderer);
        }
        if (getSuppressedCount() > 0) {
            cellRenderer.append(getCountMsg("suppressed violation", getSuppressedCount()), GRAYED_ATTRIBUTES);
        }
        if (getErrorCount() > 0) {
            cellRenderer.append(getCountMsg("processing error", getErrorCount()), GRAYED_ATTRIBUTES);
        }
        if (getUselessSuppressionCount() > 0) {
            cellRenderer.append(getCountMsg("useless suppression", getUselessSuppressionCount()), GRAYED_ATTRIBUTES);
        }
        if (getViolationCount() == 0 && getSuppressedCount() == 0 && getErrorCount() == 0 && getUselessSuppressionCount() == 0) {
            cellRenderer.append(getCountMsg("violation", getViolationCount()), GRAYED_ATTRIBUTES);
        }
        if (getViolationCount() == 0) {
            renderScore(cellRenderer);
        }
    }

    /**
     * @return the score of this rule set, empty when the rule set defines no score or no code lines were scanned
     */
    public synchronized Optional<RuleSetScore> getScore() {
        if (scoreSpec == null) {
            return Optional.empty();
        }
        Severity[] severities = Severity.values(); // in PMD priority order 1..5
        int[] violationsPerPriority = new int[severities.length];
        for (int i = 0; i < severities.length; i++) {
            violationsPerPriority[i] = getSevViolationCount(severities[i]);
        }
        return RuleSetScore.of(scoreSpec, violationsPerPriority, codeLines);
    }

    /**
     * @return the sustainability score of this rule set, empty when the rule set defines none or no code lines were scanned
     */
    public synchronized Optional<RuleSetScore> getSustainabilityScore() {
        if (sustainabilitySpec == null) {
            return Optional.empty();
        }
        SustainabilityLevel[] levels = SustainabilityLevel.values(); // in order of the weights: high, medium, low
        int[] violationsPerLevel = new int[levels.length];
        for (int i = 0; i < levels.length; i++) {
            violationsPerLevel[i] = getSustainabilityViolationCount(levels[i]);
        }
        return RuleSetScore.of(sustainabilitySpec, violationsPerLevel, codeLines);
    }

    private void renderScore(PMDCellRenderer cellRenderer) {
        getScore().ifPresent(score -> {
            cellRenderer.append("  ");
            cellRenderer.append("Score: " + score.grade(), new SimpleTextAttributes(STYLE_BOLD, gradeColor(score.gradeIndex())));
        });
        getSustainabilityScore().ifPresent(sustainability -> cellRenderer.appendLeaves(sustainability.gradeIndex()));
    }

    /**
     * @return green for A and B, orange for C and D, red for E and beyond
     */
    public static Color gradeColor(int gradeIndex) {
        if (gradeIndex <= 1) {
            return GOOD_GRADE_COLOR;
        }
        return gradeIndex <= 3 ? Severity.HIGH.getColor() : Severity.BLOCKER.getColor();
    }
}
