package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import net.sourceforge.pmd.lang.rule.RulePriority;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

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

    private static final int LEAF_GAP = 1;

    // sustainability leaves of the row being rendered, painted over the placeholder fragment
    private int leavesLevel = -1;
    private int leavesFragmentIndex = -1;

    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
        leavesLevel = -1;
        leavesFragmentIndex = -1;
        if (value instanceof BasePMDNode) {
            ((BasePMDNode)value).render(this, expanded);
        }
    }

    public void setIconForRulePriority(RulePriority priority) {
        setIcon(Severity.iconOf(priority));
    }

    /**
     * Appends the sustainability leaves of the level (see {@link SustainabilityLeaves}): a placeholder fragment of
     * spaces, wide enough for the leaves, which are painted over it in {@link #paintComponent(Graphics)}.
     */
    public void appendLeaves(int level) {
        Icon icon = SustainabilityLeaves.icon(level);
        int width = SustainabilityLeaves.count(level) * (icon.getIconWidth() + LEAF_GAP);
        Font font = getFont();
        int spaceWidth = font == null ? 4 : Math.max(1, getFontMetrics(font).charWidth(' '));
        append(" ");
        leavesLevel = level;
        leavesFragmentIndex = getFragmentCount();
        append(" ".repeat((width + spaceWidth - 1) / spaceWidth));
    }

    /**
     * @return the sustainability level of the leaves in the rendered row, -1 when none
     */
    public int getLeavesLevel() {
        return leavesLevel;
    }

    int getLeavesFragmentIndex() {
        return leavesFragmentIndex;
    }

    /**
     * Finds the x where the leaves placeholder fragment starts. The tree renderer replaces fragment attributes on a
     * selected row, so attribute based painting hooks are not called then; the position is used instead.
     *
     * @return the x of the placeholder, -1 when not found
     */
    int findLeavesX() {
        int width = getWidth();
        // skip the icon area before the text; from the first text fragment on, fragment indices only increase,
        // until past the end of the text where findFragmentAt returns a negative value
        int lo = 0;
        while (lo < width && findFragmentAt(lo) < 0) {
            lo++;
        }
        if (lo >= width || findFragmentAt(lo) > leavesFragmentIndex) {
            return -1;
        }
        // binary search for the first x with fragment index >= placeholder index (or past the end of the text)
        int hi = width;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            int index = findFragmentAt(mid);
            if (index >= leavesFragmentIndex || index < 0) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo < width && findFragmentAt(lo) == leavesFragmentIndex ? lo : -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (leavesFragmentIndex < 0) {
            return;
        }
        int x = findLeavesX();
        if (x < 0) {
            return;
        }
        Icon icon = SustainabilityLeaves.icon(leavesLevel);
        int y = (getHeight() - icon.getIconHeight()) / 2;
        for (int i = 0; i < SustainabilityLeaves.count(leavesLevel); i++) {
            icon.paintIcon(this, g, x + i * (icon.getIconWidth() + LEAF_GAP), y);
        }
    }
}
