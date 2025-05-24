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
    /** Menu label for details - showing rule details */
    public static final String DETAILS = "Show online rule documentation";

    private final JPopupMenu menu;
    private final JMenuItem suppressMenuItem;
    private final JMenuItem detailsMenuItem;

    private final List<PMDViolation> violations = new ArrayList<>();
    private String detailsUrl = "";


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

        detailsMenuItem = new JMenuItem(DETAILS);
        detailsMenuItem.addActionListener(actionListener);
        detailsMenuItem.setVisible(false);
        this.menu.add(detailsMenuItem);
    }

    /**
     * Sets the violation to given one.
     *
     * @param violation The PMD Violation of the node on which menu is shown
     */
    public void addViolation(PMDViolation violation) {
        suppressMenuItem.setVisible(true);
        this.violations.add(violation);
        if (detailsUrl.isEmpty()) {
            setDetailsUrl(violation.getExternalUrl());
        }
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
        setDetailsUrl("");
    }

    /**
     * Get the JPopupMenu component to show.
     *
     * @return the JPopupMenu of this popup menu.
     */
    public JPopupMenu getMenu() {
        return menu;
    }

    @NotNull
    public String getDetailsUrl() {
        return detailsUrl;
    }

    public void setDetailsUrl(String externalInfoUrl) {
        detailsUrl = externalInfoUrl != null ? externalInfoUrl : "";
        detailsMenuItem.setVisible(!detailsUrl.isBlank());
    }

    public boolean hasVisibleMenuItems() {
        return detailsMenuItem.isVisible() || suppressMenuItem.isVisible();
    }
}
