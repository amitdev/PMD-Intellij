package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.ui.SimpleTextAttributes;

/**
 * Represents the node data for the rule nodes. These nodes contains the actual
 * violation nodes. It is used to group similar violations.
 *
 * @author bodhi
 * @version 1.1
 */
public class PMDRuleNode implements PMDTreeNodeData {

    private String nodeName;
    private String toolTip;
    private int childCount;
    private static final char CUSTOM_RULE_DELIM = ';';

    /**
     * Create a node with the given value as node name.
     *
     * @param nodeName The node name to set.
     */
    public PMDRuleNode(String nodeName) {
        this.nodeName = nodeName;
        //For custom rulesets, delimiter is used to separate name and tooltip
        int i = nodeName.indexOf(CUSTOM_RULE_DELIM);
        if (i != -1) {
            this.nodeName = nodeName.substring(0, i);
            setToolTip(nodeName.substring(i+1));
        }
    }

    /**
     * The child count of this node.
     *
     * @return the child count
     */
    public int getChildCount() {
        return childCount;
    }

    /**
     * Returns the name of this node.
     * @return the name of this node.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * Adds multiple children to this node.
     *
     * @param count number of children to add
     */
    public void addChildren(int count) {
        childCount += count;
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
        cellRenderer.append(getNodeName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        cellRenderer.append(getViolationMsg(getChildCount()), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        if (expanded) {
            cellRenderer.setIcon(PMDCellRenderer.OPEN_ICON);
        } else {
            cellRenderer.setIcon(PMDCellRenderer.CLOSED_ICON);
        }
    }

    private String getViolationMsg(int count) {
        if (count <= 1) {
            return " (" + count + " violation)";
        } else {
            return " (" + count + " violations)";
        }
    }
}