package com.intellij.plugins.bodhi.pmd.core;

import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;

/**
 * The score of a rule set run: weighted violations per 1000 non-comment code lines, graded by the {@link RuleSetScoreSpec}.
 *
 * @param spec                  the score definition of the rule set
 * @param violationsPerPriority violation count per priority, index = priority - 1, length 5
 * @param codeLines             number of non-comment, non-blank code lines scanned
 */
public record RuleSetScore(RuleSetScoreSpec spec, int[] violationsPerPriority, int codeLines) {

    /**
     * @return the score, or empty when there are no code lines to relate the violations to
     */
    public static Optional<RuleSetScore> of(RuleSetScoreSpec spec, int[] violationsPerPriority, int codeLines) {
        return codeLines <= 0 ? Optional.empty() : Optional.of(new RuleSetScore(spec, violationsPerPriority, codeLines));
    }

    public long weightedSum() {
        long sum = 0;
        for (int i = 0; i < spec.weights().length; i++) {
            sum += (long) spec.weights()[i] * violationsPerPriority[i];
        }
        return sum;
    }

    public double kloc() {
        return codeLines / 1000.0;
    }

    public double score() {
        return weightedSum() / kloc();
    }

    public int gradeIndex() {
        return spec.gradeIndex(score());
    }

    public String grade() {
        return spec.grade(score());
    }

    /**
     * @return e.g. "jPinpoint-score: D"
     */
    public String gradeLabel() {
        return spec.name() + ": " + grade();
    }

    /**
     * @return e.g. "(27x2 + 9x5 + 3x20 + 1x15 = 174) / 12.3 kloc = 14.1"; only priorities with weight &gt; 0
     */
    public String formula() {
        StringJoiner terms = new StringJoiner(" + ");
        for (int i = 0; i < spec.weights().length; i++) {
            if (spec.weights()[i] > 0) {
                terms.add(spec.weights()[i] + "x" + violationsPerPriority[i]);
            }
        }
        return String.format(Locale.ROOT, "(%s = %d) / %.1f kloc = %.1f", terms, weightedSum(), kloc(), score());
    }

    /**
     * @param markCurrent applied to the letter of the current grade, e.g. to make it bold
     * @return e.g. "A = 0 &lt; B &lt; 5 ≤ C &lt; 10 ≤ D &lt; 15 ≤ E &lt; 25 ≤ F"
     */
    public String gradeScale(UnaryOperator<String> markCurrent) {
        double[] thresholds = spec.thresholds();
        int current = gradeIndex();
        StringBuilder scale = new StringBuilder(letter(0, current, markCurrent))
                .append(thresholds[0] == 0 ? " = " : " ≤ ").append(format(thresholds[0]));
        for (int i = 1; i <= thresholds.length; i++) {
            // the lower bound is exclusive for B (score > t[0]) and inclusive for the next grades (score >= t[i-1])
            scale.append(i == 1 ? " < " : " ≤ ").append(letter(i, current, markCurrent));
            if (i < thresholds.length) {
                scale.append(" < ").append(format(thresholds[i]));
            }
        }
        return scale.toString();
    }

    private static String letter(int index, int current, UnaryOperator<String> markCurrent) {
        String letter = String.valueOf((char) ('A' + index));
        return index == current ? markCurrent.apply(letter) : letter;
    }

    private static String format(double threshold) {
        return threshold == Math.rint(threshold) ? String.valueOf((long) threshold) : String.valueOf(threshold);
    }
}
