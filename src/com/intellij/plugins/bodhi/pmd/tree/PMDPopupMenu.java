package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.PMDViolation;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * The is the presentation class for the popup menu on violation result tree.
 *
 * @author bodhi
 * @version 1.0
 */
public class PMDPopupMenu {

    private List<PMDViolation> violations = new ArrayList<PMDViolation>();
    private JPopupMenu menu;
    /** Menu label for suppress */
    public static final String SUPPRESS = "Suppress";
    /** Menu label for details - showing rule details */
    public static final String DETAILS = "Details";

    /**
     * Creates a popup menu and associates the given action listener with it.
     *
     * @param actionListener The actionlistener to use
     */
    public PMDPopupMenu(@NotNull ActionListener actionListener) {
        this.menu = new JPopupMenu();
        JMenuItem item = new JMenuItem(SUPPRESS);
        item.addActionListener(actionListener);
        this.menu.add(item);

        item = new JMenuItem(DETAILS);
        item.addActionListener(actionListener);
        this.menu.add(item);
    }

    /**
     * Sets the violation to given one.
     *
     * @param violation The PMD Violation of the node on which menu is shown
     */
    public void addViolation(PMDViolation violation) {
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

    public void clearViolations() {
        this.violations.clear();
    }

    /**
     * Get the JPopupMenu component to show.
     *
     * @return the JPopupMenu of this popup menu.
     */
    public JPopupMenu getMenu() {
        return menu;
    }
}