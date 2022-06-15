package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.openapi.util.IconLoader;
import net.sourceforge.pmd.RulePriority;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Severity enum for 5 levels, with name, PMD priority and icon.
 *
 * @author jborgers
 */
public enum Severity {
    BLOCKER(RulePriority.HIGH, "Blocker", IconLoader.getIcon("/ide/fatalError.png", Severity.class)),
    CRITICAL(RulePriority.MEDIUM_HIGH, "Critical", IconLoader.getIcon("/runConfigurations/testError.png", Severity.class)),
    MAJOR(RulePriority.MEDIUM, "Major", IconLoader.getIcon("/general/warning.png", Severity.class)),
    MINOR(RulePriority.MEDIUM_LOW, "Minor", IconLoader.getIcon("/nodes/warningIntroduction.png", Severity.class)),
    INFO(RulePriority.LOW, "Info", IconLoader.getIcon("/general/information.png", Severity.class));

    private final RulePriority rulePriority;
    private final String name;
    private final Icon icon;

    private static final Map<RulePriority, Icon> prioToIcon = Stream.of(values()).collect(toMap(Severity::getRulePriority, Severity::getIcon));

    Severity(@NotNull RulePriority rulePrio, @NotNull String nm, @NotNull Icon ic) {
        rulePriority = rulePrio;
        name = nm;
        icon = ic;
    }

    public RulePriority getRulePriority() {
        return rulePriority;
    }

    public static Icon iconOf(RulePriority rulePrio) {
        return prioToIcon.get(rulePrio);
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return "Severity{" +
                "rulePriority=" + rulePriority +
                ", name='" + name + '\'' +
                ", icon=" + icon +
                '}';
    }
}
