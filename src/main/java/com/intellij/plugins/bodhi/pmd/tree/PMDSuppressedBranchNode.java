package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.HasMessage;
import org.jetbrains.annotations.NotNull;

/**
 * PMD branch tree node for suppressed rules. It has a Severity.INFO icon and can be sorted after rules and then by name.
 *
 * @author jborgers
 */
public class PMDSuppressedBranchNode extends PMDRuleSetEntryNode implements HasMessage {
    public PMDSuppressedBranchNode(String name) {
        super(name);
    }

    @Override
    public String getToolTip() {
        return getMessage();
    }

    @Override
    public String getMessage() {
        return getNodeName() + ".";
    }

    @Override
    public synchronized void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.setIcon(Severity.INFO.getIcon());
        super.render(cellRenderer, expanded);
    }

    @Override
    public int compareTo(@NotNull PMDRuleSetEntryNode o) {
        if (o instanceof PMDSuppressedBranchNode) {
            return getNodeName().compareTo(o.getNodeName());
        }
        return 1; // always after rule
    }
}
