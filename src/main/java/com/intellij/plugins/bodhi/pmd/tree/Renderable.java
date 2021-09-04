package com.intellij.plugins.bodhi.pmd.tree;

public interface Renderable {
    /**
     * Render this node data using the cell renderer given.
     *
     * @param cellRenderer The Cell Renderer to render this node data.
     * @param expanded     true if the node is expanded, false otherwise
     */
    void render(PMDCellRenderer cellRenderer, boolean expanded);
}
