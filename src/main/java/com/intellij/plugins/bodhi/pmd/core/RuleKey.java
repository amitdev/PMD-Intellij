package com.intellij.plugins.bodhi.pmd.core;

import net.sourceforge.pmd.Rule;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Class for using Rule as key in a Map, and to compare/sort first by priority and then by name
 * @author jborgers
 */
public class RuleKey implements Comparable<RuleKey> {
    private final String name;
    private final int priority;

    public RuleKey(@NotNull String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public RuleKey(@NotNull Rule rule) {
        name = rule.getName();
        priority = rule.getPriority().getPriority();
    }

    @Override
    public String toString() {
        return "RuleKey{" +
                "name='" + name + '\'' +
                ", priority=" + priority +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleKey ruleKey = (RuleKey) o;
        return priority == ruleKey.priority && name.equals(ruleKey.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, priority);
    }

    @Override
    public int compareTo(@NotNull RuleKey other) {
        if (priority != other.priority) {
            return Integer.compare(priority, other.priority);
        }
        return name.compareTo(other.name);
    }
}
