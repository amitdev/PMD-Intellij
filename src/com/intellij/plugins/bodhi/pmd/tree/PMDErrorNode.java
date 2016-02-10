package com.intellij.plugins.bodhi.pmd.tree;

public class PMDErrorNode extends PMDRuleNode {

    private String errorMsg;

    public PMDErrorNode(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getToolTip() {
        return "Error in running PMD rule on this file";
    }

    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.setIcon(PMDCellRenderer.ERROR);
        cellRenderer.append(errorMsg);
    }

}
