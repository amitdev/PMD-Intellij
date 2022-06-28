package com.intellij.plugins.bodhi.pmd.tree;

import org.jetbrains.annotations.NotNull;

/**
 * PMD branch tree node for suppressed rules. It has a Severity.INFO icon and can be sorted after rules and then by name.
 *
 * @author jborgers
 */public class PMDSuppressedBranchNode extends PMDRuleSetEntryNode {
    public PMDSuppressedBranchNode(String name) {
        super(name);
    }

    @Override
    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
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
