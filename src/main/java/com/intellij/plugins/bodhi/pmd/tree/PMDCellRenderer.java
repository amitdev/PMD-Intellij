package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.openapi.util.IconLoader;
import com.intellij.plugins.bodhi.pmd.core.PMDViolation;
import com.intellij.ui.ColoredTreeCellRenderer;

import javax.swing.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    static Icon CLOSED_ICON;
    static Icon OPEN_ICON;
    public static final Icon ERROR = IconLoader.getIcon("/compiler/error.png");
    public static final Icon WARN = IconLoader.getIcon("/compiler/warning.png");
    public static final Icon INFO = IconLoader.getIcon("/compiler/information.png");

    private static final Map<String, Icon> prioToIcon;

    //Try to load idea specific icons for the tree.
    static {
        CLOSED_ICON = IconLoader.getIcon("/nodes/TreeClosed.png");
        OPEN_ICON = IconLoader.getIcon("/nodes/TreeOpen.png");

        Map<String, Icon> attrs = new HashMap<>();
        attrs.put("Medium", WARN);
        attrs.put("Medium High", WARN);
        attrs.put("High", ERROR);
        attrs.put("Medium Low", INFO);
        attrs.put("Low", INFO);
        prioToIcon = Collections.unmodifiableMap(attrs);
    }

    public void customizeCellRenderer(JTree tree,
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

    public void setIconForViolationPrio(PMDViolation violation) {
        setIcon(prioToIcon.get(violation.getRulePriorityName()));
    }
}
