package com.intellij.plugins.bodhi.pmd.tree;

import javax.swing.tree.TreeNode;
import java.util.Enumeration;
import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;

/**
 * Represents the node data for branch nodes like rule sets and rules.
 * Root branch node contains rule set branch nodes and processing error branch node.
 * Rule set branch nodes contain rule branch nodes and suppressed violation leaf nodes.
 * Rule nodes contain violation leaf nodes.
 *
 * @author bodhi
 * @version 1.2
 */
public class PMDBranchNode extends BasePMDNode {

    private final String nodeName;
    private String toolTip;
    private int violationCount = 0;
    private int suppressedCount = 0;
    private int errorCount = 0;

    /**
     * Create a node with the given value as node name.
     *
     * @param nodeName The node name to set.
     */
    public PMDBranchNode(String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * Calculate the number of violations, suppressed violations and processing errors from the child nodes.
     * The leaf nodes will answer with 0 or 1, and the branch nodes will aggregate/calculate recursively.
     */
    public void calculateCounts() {
        violationCount = 0;
        suppressedCount = 0;
        errorCount = 0;
        Enumeration<TreeNode> children = children();
        while (children.hasMoreElements()) {
            Object child = children.nextElement();
            if (child instanceof BasePMDNode) {
                BasePMDNode node = (BasePMDNode) child;
                if (node instanceof PMDBranchNode) {
                    ((PMDBranchNode) node).calculateCounts();
                }
                violationCount += node.getViolationCount();
                suppressedCount += node.getSuppressedCount();
                errorCount += node.getErrorCount();
            }
        }
    }


    /**
     * Returns the name of this node.
     * @return the name of this node.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * The violation (child) count of this node. If count == 0 then all counts will be calculated from the children.
     *
     * @return the violation count
     */
    public int getViolationCount() {
        if (violationCount == 0) {
            calculateCounts();
        }
        return violationCount;
    }

    /**
     * The suppressed violation (child) count of this node.
     *
     * @return the violation count
     */
    public int getSuppressedCount() {
        return suppressedCount;
    }

    /**
     * The suppressed violation (child) count of this node.
     *
     * @return the violation count
     */
    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Sets the tooltip.
     *
     * @param toolTip tooltip to set
     */
    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public String getToolTip() {
        return toolTip;
    }

    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.append(getNodeName());
        if (violationCount > 0 ) {
            cellRenderer.append(getCountMsg("violation", violationCount), GRAYED_ATTRIBUTES);
        }
        if (suppressedCount > 0) {
            cellRenderer.append(getCountMsg("suppressed violation", suppressedCount), GRAYED_ATTRIBUTES);
        }
        if (errorCount > 0) {
            cellRenderer.append(getCountMsg("processing error", errorCount), GRAYED_ATTRIBUTES);
        }
        if (violationCount == 0 && suppressedCount == 0 && errorCount == 0) {
            cellRenderer.append(getCountMsg("violation", violationCount), GRAYED_ATTRIBUTES);
        }
    }

    protected String getCountMsg(String countName, int count) {
        return " (" + count + " " + countName + ((count != 1) ? "s" : "") + ")";
    }

    @Override
    public void navigate(boolean b) { }  // no-op

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

}