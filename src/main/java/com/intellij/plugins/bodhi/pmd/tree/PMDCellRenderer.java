package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import net.sourceforge.pmd.lang.rule.RulePriority;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * A Custom Cell renderer that will render the user objects of this plugin
 * correctly. It extends the ColoredTreeCellRenderer to make use of the text
 * attributes.
 *
 * @author bodhi
 * @version 1.2
 */
public class PMDCellRenderer extends ColoredTreeCellRenderer {

    //Default tree icons
    private static final Icon CLOSED_ICON;
    private static final Icon OPEN_ICON;

    //Try to load idea specific icons for the tree.
    static {
        CLOSED_ICON = AllIcons.Nodes.Folder;
        OPEN_ICON = AllIcons.Nodes.Folder;
    }

    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
        if (value instanceof BasePMDNode) {
            ((BasePMDNode)value).render(this, expanded);
        }
    }

    public void setIconForRulePriority(RulePriority priority) {
        setIcon(Severity.iconOf(priority));
    }
}
