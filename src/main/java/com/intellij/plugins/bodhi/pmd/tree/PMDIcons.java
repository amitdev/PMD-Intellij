package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * For icons which are not available from the IntelliJ runtime, or not in every version, or not the nice-looking version of it.
 */
public class PMDIcons {

    public static final Icon PMD = IconLoader.getIcon("/icons/pmd.svg", PMDIcons.class);
    public static final Icon PMD_GRAY = IconLoader.getIcon("/icons/pmd_gray.svg", PMDIcons.class);

    public static final Icon ICON_HIGH = IconLoader.getIcon("/icons/errorIntroduction.svg", PMDIcons.class);
}

