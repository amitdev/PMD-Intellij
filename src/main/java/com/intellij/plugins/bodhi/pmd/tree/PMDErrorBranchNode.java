package com.intellij.plugins.bodhi.pmd.tree;

/**
 * PMD branch tree node for processing errors. It has a Severity.BLOCKER icon.
 *
 * @author jborgers
 */
public class PMDErrorBranchNode extends PMDBranchNode{
    public PMDErrorBranchNode(String name) {
        super(name);
    }

    @Override
    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.setIcon(Severity.BLOCKER.getIcon());
        super.render(cellRenderer, expanded);
    }
}
