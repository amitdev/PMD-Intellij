package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * A Custom Cell renderer that will render the user objects of this plugin
 * correctly. It extends the ColoredTreeCellRenderer to make use of the text
 * attributes.
 *
 * @author bodhi
 * @version 1.1
 */
public class PMDCellRenderer extends ColoredTreeCellRenderer {

    //Default tree icons
    public static Icon CLOSED_ICON = UIManager.getIcon("Tree.closedIcon");
    public static Icon OPEN_ICON = UIManager.getIcon("Tree.openIcon");
    public static final Icon ERROR = IconLoader.getIcon("/compiler/error.png");
    public static final Icon WARN = IconLoader.getIcon("/compiler/warning.png");
    public static final Icon INFO = IconLoader.getIcon("/compiler/information.png");

    //Try to load idea specific icons for the tree.
    static {
        CLOSED_ICON = IconLoader.getIcon("/nodes/folder.png");
        OPEN_ICON = IconLoader.getIcon("/nodes/folderOpen.png");
    }

    public void customizeCellRenderer(JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)value;
        Object userObject = treeNode.getUserObject();

        if (userObject instanceof PMDTreeNodeData) {
            //Each PMDTreeNodeData knows how to render itself.
            ((PMDTreeNodeData)userObject).render(this, expanded);
        }
    }

}