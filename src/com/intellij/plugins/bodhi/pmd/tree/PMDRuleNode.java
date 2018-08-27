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
    private int violationCount = -1;
    private int fileCount = -1;
    private int ruleSetCount = -1;

    private static final char CUSTOM_RULE_DELIM = ';';

    public void setFileCount(int count) {
        this.fileCount = count;
    }

    public void setRuleSetCount(int count) {
        this.ruleSetCount = count;
    }

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

    public PMDRuleNode() {
    }

    /**
     * The violation (child) count of this node.
     *
     * @return the violation count
     */
    public int getViolationCount() {
        return violationCount;
    }

    /**
     * Returns the name of this node.
     * @return the name of this node.
     */
    public String getNodeName() {
        return nodeName;
    }

    /**
     * Adds a number to the violations (child) count of this node.
     *
     * @param count number of violations to add
     */
    public void addToViolationCount(int count) {
        if (violationCount == -1) violationCount = 0; // results available (successful pmd processing)
        violationCount += count;
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
        cellRenderer.append(getNodeName(), SimpleTextAttributes.REGULAR_ATTRIBUTES); // test
        cellRenderer.append(getViolationMsg(getViolationCount()), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        // hack to make cell large enough to display all
        cellRenderer.append("                                                           ",
                SimpleTextAttributes.GRAYED_ATTRIBUTES);
        if (expanded) {
            cellRenderer.setIcon(PMDCellRenderer.OPEN_ICON);
        } else {
            cellRenderer.setIcon(PMDCellRenderer.CLOSED_ICON);
        }
    }

    private String getViolationMsg(int violationCount) {
        if (fileCount == 0) {
            return " No results: No source file(s) found to scan.";
        }
        if (violationCount == -1) {
            return " No results yet";
        }
        String result = " (" + violationCount + " violation";
        if (violationCount != 1) result += "s";
        if (fileCount > -1) {
            result += " in " + fileCount + " scanned file";
            if (fileCount != 1) result += "s";
        }
        if (ruleSetCount > -1) {
            result += " using " + ruleSetCount + " rule set";
            if (ruleSetCount != 1) result += "s";
        }
        return result + ")";
    }
}