package com.intellij.plugins.bodhi.pmd.tree;

/**
 * Represents the Tree node userdata.
 *
 * @author bodhi
 * @version 1.0
 */
public interface PMDTreeNodeData {

    /**
     * Get the tool tip associated with this node.
     *
     * @return the tooltip text
     */
    String getToolTip();

    /**
     * Render this node data using the cell renderer given.
     *
     * @param cellRenderer The Cell Renderer to render this node data.
     * @param expanded true if the node is expanded, false otherwise
     */
    void render(PMDCellRenderer cellRenderer, boolean expanded);
}
