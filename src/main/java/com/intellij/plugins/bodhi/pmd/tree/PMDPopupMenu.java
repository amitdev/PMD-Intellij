package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.PMDViolation;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the presentation class for the popup menu on violation result tree.
 *
 * @author bodhi
 * @version 1.0
 */
public class PMDPopupMenu {
    /** Menu label for suppress */
    public static final String SUPPRESS = "Suppress";

    private final JPopupMenu menu;
    private final JMenuItem suppressMenuItem;

    private final List<PMDViolation> violations = new ArrayList<>();


    /**
     * Creates a popup menu and associates the given action listener with it.
     *
     * @param actionListener The actionlistener to use
     */
    public PMDPopupMenu(@NotNull ActionListener actionListener) {
        this.menu = new JPopupMenu();
        this.suppressMenuItem = new JMenuItem(SUPPRESS);
        suppressMenuItem.addActionListener(actionListener);
        suppressMenuItem.setVisible(false);
        this.menu.add(suppressMenuItem);
    }

    /**
     * Sets the violation to given one.
     *
     * @param violation The PMD Violation of the node on which menu is shown
     */
    public void addViolation(PMDViolation violation) {
        suppressMenuItem.setVisible(true);
        this.violations.add(violation);
    }

    /**
     * Gets the Violation.
     *
     * @return The Violation of the node where this menu is shown
     */
    public List<PMDViolation> getViolations() {
        return violations;
    }

    public void clearViolationsAndUrl() {
        suppressMenuItem.setVisible(false);
        violations.clear();
    }

    /**
     * Get the JPopupMenu component to show.
     *
     * @return the JPopupMenu of this popup menu.
     */
    public JPopupMenu getMenu() {
        return menu;
    }

    public boolean hasVisibleMenuItems() {
        return suppressMenuItem.isVisible();
    }
}
