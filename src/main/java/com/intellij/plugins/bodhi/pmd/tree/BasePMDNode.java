package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.PMDResultPanel;
import com.intellij.pom.Navigatable;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Abstract base class for PMD Tree Nodes.
 * @author jborgers
 */
public abstract class BasePMDNode extends DefaultMutableTreeNode implements PMDTreeNodeData, Navigatable {

    /**
     * @deprecated Not to be used anymore, user data is now strongly typed available from a subclass of this class.
     * @return null
     */
    @Deprecated
    final public Object getUserObject() {
        return null;
    }

    /**
     * @deprecated Not to be used anymore, user data is now strongly typed available from a subclass of this class.
     * Does nothing.
     */
    @Deprecated
    final public void setUserObject(Object o) {
        // nop-op;
    }

    @Override
    public abstract String getToolTip();

    @Override
    public abstract void render(PMDCellRenderer cellRenderer, boolean expanded);

    /**
     * Return the result panel at root level, to do highlighting etc
     * @return result panel
     */
    protected PMDResultPanel getRootResultPanel() {
        return ((PMDRootNode)getRoot()).getResultPanel(); //  assume root is type PMDRootNode, otherwise exception
    }

    public abstract int getViolationCount();

    public abstract int getSuppressedCount();

    public abstract int getErrorCount();
    public abstract int getUselessSuppressionCount();

    public abstract int getSevViolationCount(Severity sev);
}
