package com.intellij.plugins.bodhi.pmd.tree;

/**
 * Represents the Tree node userdata.
 *
 * @author bodhi
 * @version 1.0
 */
public interface PMDTreeNodeData extends Renderable {

    /**
     * Get the tool tip associated with this node.
     *
     * @return the tooltip text
     */
    String getToolTip();

}
