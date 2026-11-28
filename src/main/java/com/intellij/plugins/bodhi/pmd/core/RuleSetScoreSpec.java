package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Score definition of a rule set, read from a marker line in the rule set description, e.g.:
 * <pre>pmd-score: name=jPinpoint-score; weights=27,9,3,1,0; thresholds=0,5,10,15,25</pre>
 * Weights are per PMD priority 1..5; thresholds are the ascending upper bounds of the grades A, B, C, ...
 * A {@code pmd-sustainability:} line defines the sustainability score the same way, with weights per sustainability level.
 *
 * @param name       label of the score
 * @param weights    weight per category: per priority (index = priority - 1) or per {@link SustainabilityLevel} (ordinal)
 * @param thresholds ascending grade bounds; n thresholds give n + 1 grades
 */
public record RuleSetScoreSpec(String name, int[] weights, double[] thresholds) {

    private static final Logger LOG = Logger.getInstance(RuleSetScoreSpec.class);
    private static final double[] DEFAULT_THRESHOLDS = {0, 5, 10, 15, 25};

    /**
     * Kind of score definition.
     *
     * @param marker         line prefix in the rule set description
     * @param categoryCount  number of weights
     * @param defaultName    name when none is given
     * @param thresholdCount required number of thresholds, 0 for any
     */
    private record Kind(String marker, int categoryCount, String defaultName, int thresholdCount) {
        boolean isMarkerLine(String line) {
            return line.trim().toLowerCase(Locale.ROOT).startsWith(marker);
        }
    }

    /** Weights per PMD priority 1..5, any number of grades. */
    private static final Kind SCORE = new Kind("pmd-score:", 5, "score", 0);
    /** Weights per sustainability level high, medium, low; always 6 levels (leaves). */
    private static final Kind SUSTAINABILITY = new Kind("pmd-sustainability:", SustainabilityLevel.values().length, "Sustainability-score", 5);

    /**
     * Parses the {@code pmd-score:} line from a rule set description.
     *
     * @return the spec, or empty when the description has no marker or the marker is malformed
     */
    public static Optional<RuleSetScoreSpec> parse(@Nullable String description) {
        return parse(description, SCORE);
    }

    /**
     * Parses the {@code pmd-sustainability:} line from a rule set description; weights for high, medium, low.
     *
     * @return the spec, or empty when the description has no marker or the marker is malformed
     */
    public static Optional<RuleSetScoreSpec> parseSustainability(@Nullable String description) {
        return parse(description, SUSTAINABILITY);
    }

    private static Optional<RuleSetScoreSpec> parse(@Nullable String description, Kind kind) {
        if (description == null) {
            return Optional.empty();
        }
        Optional<String> markerLine = description.lines().filter(kind::isMarkerLine).findFirst();
        if (markerLine.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(parseMarker(markerLine.get().trim().substring(kind.marker().length()), kind));
        } catch (IllegalArgumentException e) {
            LOG.warn("Ignoring malformed rule set score definition '" + markerLine.get().trim() + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * @return the description without the score marker lines, trimmed; null for null
     */
    public static @Nullable String stripMarker(@Nullable String description) {
        if (description == null) {
            return null;
        }
        return description.lines()
                .filter(line -> !SCORE.isMarkerLine(line) && !SUSTAINABILITY.isMarkerLine(line))
                .collect(Collectors.joining("\n")).trim();
    }

    private static RuleSetScoreSpec parseMarker(String settings, Kind kind) {
        String name = kind.defaultName();
        int[] weights = null;
        double[] thresholds = DEFAULT_THRESHOLDS;
        for (String part : settings.split(";")) {
            if (part.isBlank()) {
                continue;
            }
            int eq = part.indexOf('=');
            if (eq < 0) {
                throw new IllegalArgumentException("missing '=' in '" + part.trim() + "'");
            }
            String key = part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String value = part.substring(eq + 1).trim();
            switch (key) {
                case "name" -> name = value.isEmpty() ? kind.defaultName() : value;
                case "weights" -> weights = parseWeights(value, kind.categoryCount());
                case "thresholds" -> thresholds = parseThresholds(value);
                default -> throw new IllegalArgumentException("unknown key '" + key + "'");
            }
        }
        if (weights == null) {
            throw new IllegalArgumentException("weights missing");
        }
        if (kind.thresholdCount() > 0 && thresholds.length != kind.thresholdCount()) {
            throw new IllegalArgumentException("exactly " + kind.thresholdCount() + " thresholds required");
        }
        return new RuleSetScoreSpec(name, weights, thresholds);
    }

    private static int[] parseWeights(String value, int categoryCount) {
        String[] parts = value.split(",");
        if (parts.length > categoryCount) {
            throw new IllegalArgumentException("more than " + categoryCount + " weights");
        }
        int[] weights = new int[categoryCount];
        for (int i = 0; i < parts.length; i++) {
            weights[i] = Integer.parseInt(parts[i].trim()); // NumberFormatException is an IllegalArgumentException
            if (weights[i] < 0) {
                throw new IllegalArgumentException("negative weight");
            }
        }
        return weights;
    }

    private static double[] parseThresholds(String value) {
        double[] thresholds = Arrays.stream(value.split(",")).map(String::trim).mapToDouble(Double::parseDouble).toArray();
        for (int i = 1; i < thresholds.length; i++) {
            if (thresholds[i] <= thresholds[i - 1]) {
                throw new IllegalArgumentException("thresholds not ascending");
            }
        }
        return thresholds;
    }

    /**
     * @return 0 for A, 1 for B, ...: score &lt;= t[0] gives 0; otherwise the first i &gt;= 1 with score &lt; t[i]; else thresholds.length
     */
    public int gradeIndex(double score) {
        if (score <= thresholds[0]) {
            return 0;
        }
        for (int i = 1; i < thresholds.length; i++) {
            if (score < thresholds[i]) {
                return i;
            }
        }
        return thresholds.length;
    }

    public String grade(double score) {
        return String.valueOf((char) ('A' + gradeIndex(score)));
    }
}
