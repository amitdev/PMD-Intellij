package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.icons.AllIcons;
import net.sourceforge.pmd.RulePriority;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Severity enum for 5 levels, with name, PMD priority, icon and color.
 *
 * @author jborgers
 */
public enum Severity {
    BLOCKER(RulePriority.HIGH, "Blocker", AllIcons.Ide.FatalError, new Color(188, 98, 98)),
    CRITICAL(RulePriority.MEDIUM_HIGH, "Critical", AllIcons.RunConfigurations.TestError, new Color(152, 98, 90)),
    MAJOR(RulePriority.MEDIUM, "Major", AllIcons.General.Warning, new Color(142, 122, 38)),
    MINOR(RulePriority.MEDIUM_LOW, "Minor", AllIcons.Nodes.WarningIntroduction, new Color(126, 126, 126)),
    INFO(RulePriority.LOW, "Info", AllIcons.General.Information, new Color(48, 116, 148));

    private final RulePriority rulePriority;
    private final String name;
    private final Icon icon;
    private final Color color;
    private static final Map<RulePriority, Icon> prioToIcon = Stream.of(values()).collect(toMap(Severity::getRulePriority, Severity::getIcon));


    Severity(@NotNull RulePriority rulePrio, @NotNull String nm, @NotNull Icon ic, @NotNull Color c) {
        rulePriority = rulePrio;
        name = nm;
        icon = ic;
        color = c;
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

    public Color getColor() { return color; }

    @Override
    public String toString() {
        return "Severity{" +
                "rulePriority=" + rulePriority +
                ", name='" + name + '\'' +
                ", icon=" + icon +
                ", color=" + color +
                '}';
    }


}
