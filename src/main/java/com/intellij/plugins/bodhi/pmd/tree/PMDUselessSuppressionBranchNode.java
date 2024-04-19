package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.HasMessage;
import org.jetbrains.annotations.NotNull;

/**
 * PMD branch tree node for useless suppressions, @SuppressWarnings of rules witohut actual suppressions.
 * It has a Severity.MAJOR icon and can be sorted after rules and then by name.
 *
 * @author jborgers
 */
public class PMDUselessSuppressionBranchNode extends PMDRuleSetEntryNode implements HasMessage {
    public PMDUselessSuppressionBranchNode(String name) {
        super(name);
    }

    @Override
    public String getToolTip() {
        return getMessage();
    }

    @Override
    public String getMessage() {
        return "Occurrences of @SuppressWarnings for a rule for which no violation is actually suppressed.";
    }

    @Override
    public synchronized void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.setIcon(Severity.MAJOR.getIcon());
        super.render(cellRenderer, expanded);
    }

    @Override
    public int compareTo(@NotNull PMDRuleSetEntryNode o) {
        if (o instanceof PMDUselessSuppressionBranchNode) {
            return getNodeName().compareTo(o.getNodeName());
        }
        return 2; // always after rule and after actual suppressions
    }
}
