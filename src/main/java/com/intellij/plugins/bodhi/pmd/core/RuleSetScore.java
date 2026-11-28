package com.intellij.plugins.bodhi.pmd.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

/**
 * The score of a rule set run: weighted violations per 1000 non-comment code lines, graded by the {@link RuleSetScoreSpec}.
 *
 * @param spec                  the score definition of the rule set
 * @param violationsPerPriority violation count per category, same length and order as the spec weights
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
        return String.format(Locale.ROOT, "(%s = %d) / %s kloc = %s", terms, weightedSum(), formatNumber(kloc()), formatNumber(score()));
    }

    /**
     * @return one decimal for 0 and values from 1, two significant digits between 0 and 1 (e.g. 0.016),
     * so a small value doesn't show as 0.0
     */
    static String formatNumber(double value) {
        if (value == 0 || value >= 1) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return new BigDecimal(value).round(new MathContext(2)).stripTrailingZeros().toPlainString();
    }

    /**
     * @param markCurrent applied to the letter of the current grade, e.g. to make it bold
     * @return e.g. "A = 0 &lt; B &lt; 5 ≤ C &lt; 10 ≤ D &lt; 15 ≤ E &lt; 25 ≤ F"
     */
    public String gradeScale(UnaryOperator<String> markCurrent) {
        return gradeScale(index -> String.valueOf((char) ('A' + index)), markCurrent);
    }

    /**
     * @param symbol      the symbol of a grade index, e.g. a letter or leaves
     * @param markCurrent applied to the symbol of the current grade
     * @return e.g. "A = 0 &lt; B &lt; 5 ≤ C &lt; 10 ≤ D &lt; 15 ≤ E &lt; 25 ≤ F"
     */
    public String gradeScale(IntFunction<String> symbol, UnaryOperator<String> markCurrent) {
        double[] thresholds = spec.thresholds();
        int current = gradeIndex();
        IntFunction<String> marked = index -> index == current ? markCurrent.apply(symbol.apply(index)) : symbol.apply(index);
        StringBuilder scale = new StringBuilder(marked.apply(0))
                .append(thresholds[0] == 0 ? " = " : " ≤ ").append(format(thresholds[0]));
        for (int i = 1; i <= thresholds.length; i++) {
            // the lower bound is exclusive for the second grade (score > t[0]) and inclusive for the next ones
            scale.append(i == 1 ? " < " : " ≤ ").append(marked.apply(i));
            if (i < thresholds.length) {
                scale.append(" < ").append(format(thresholds[i]));
            }
        }
        return scale.toString();
    }

    private static String format(double threshold) {
        return threshold == Math.rint(threshold) ? String.valueOf((long) threshold) : String.valueOf(threshold);
    }
}
