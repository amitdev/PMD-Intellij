package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.ui.SimpleTextAttributes;

import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN;

public class PMDRuleSetNode extends PMDBranchNode {
    /**
     * Create a node with the given value as node name.
     *
     * @param nodeName The node name to set.
     */
    public PMDRuleSetNode(String nodeName) {
        super(nodeName);
    }

    @Override
    public synchronized void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.append(getNodeName());
        if (getViolationCount() > 0 ) {
            cellRenderer.append(" (" + getViolationCount() + " violation" + ((getViolationCount() == 1) ? ": " : "s: "), GRAYED_ATTRIBUTES);
            boolean first = true;
            for (Severity sev : Severity.values()) {
                int count = getSevViolationCount(sev);
                if (count > 0) {
                    if (!first) {
                        cellRenderer.append(" + ", GRAYED_ATTRIBUTES);
                    }
                    cellRenderer.append(Integer.toString(count), new SimpleTextAttributes(STYLE_PLAIN, sev.getColor()));
                    first = false;
                }
            }
            cellRenderer.append(")", GRAYED_ATTRIBUTES);
        }
        if (getSuppressedCount() > 0) {
            cellRenderer.append(getCountMsg("suppressed violation", getSuppressedCount()), GRAYED_ATTRIBUTES);
        }
        if (getErrorCount() > 0) {
            cellRenderer.append(getCountMsg("processing error", getErrorCount()), GRAYED_ATTRIBUTES);
        }
        if (getUselessSuppressionCount() > 0) {
            cellRenderer.append(getCountMsg("useless suppression", getUselessSuppressionCount()), GRAYED_ATTRIBUTES);
        }
        if (getViolationCount() == 0 && getSuppressedCount() == 0 && getErrorCount() == 0 && getUselessSuppressionCount() == 0) {
            cellRenderer.append(getCountMsg("violation", getViolationCount()), GRAYED_ATTRIBUTES);
        }
    }
}
