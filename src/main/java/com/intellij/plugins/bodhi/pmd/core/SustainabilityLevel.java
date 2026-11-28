package com.intellij.plugins.bodhi.pmd.core;

import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Sustainability impact of a rule, from its {@code sustainability-high|medium|low} tag.
 * The ordinal is the index of the weight in the {@code pmd-sustainability:} definition.
 */
public enum SustainabilityLevel {
    HIGH, MEDIUM, LOW;

    private static final String TAG_PREFIX = "sustainability-";

    private String tag() {
        return TAG_PREFIX + name().toLowerCase(Locale.ROOT);
    }

    /**
     * @param tags comma separated tags, e.g. "jpinpoint-rule,memory,sustainability-medium"
     * @return the highest sustainability level among the tags, empty when there is none
     */
    public static Optional<SustainabilityLevel> ofTags(@Nullable String tags) {
        if (tags == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()) // in order of impact, highest first
                .filter(level -> Arrays.stream(tags.split(",")).map(String::trim).anyMatch(level.tag()::equals))
                .findFirst();
    }

    /**
     * @return the highest sustainability level of the rule's {@code tags} property, empty when there is none
     */
    public static Optional<SustainabilityLevel> of(@NotNull Rule rule) {
        PropertyDescriptor<?> tagsDescriptor = rule.getPropertyDescriptor("tags");
        if (tagsDescriptor == null) {
            return Optional.empty();
        }
        Object value = rule.getProperty(tagsDescriptor);
        return value == null ? Optional.empty() : ofTags(value.toString());
    }
}
