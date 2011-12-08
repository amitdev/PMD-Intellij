package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.PMDViolation;
import com.intellij.plugins.bodhi.pmd.PMDResultPanel;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * A Factory that creates different types of tree nodes used by PMD plugin.
 * This is a singleton.
 *
 * @author bodhi
 * @version 1.0
 */
public class PMDTreeNodeFactory {

    // The singletone instance
    private static final PMDTreeNodeFactory factory = new PMDTreeNodeFactory();

    /**
     * Prevents instantiation. Only allows potential subclasses.
     */
    protected PMDTreeNodeFactory() {
    }

    /**
     * Get the singletone factory.
     * @return Get the factory
     */
    public static PMDTreeNodeFactory getInstance() {
        return factory;
    }

    /**
     * Creates a tree node object based on the user object that has to be wrapped.
     * Supports String as well as PMDViolation as userobjects.
     *
     * @param userObject String or PMDViolation object that has to be wrapped.
     * @return The created treenode or null if userObject is invalid.
     */
    public DefaultMutableTreeNode createNode(Object userObject) {
        if (userObject instanceof PMDViolation) {
            return new PMDResultNode(userObject);
        } else if (userObject instanceof String) {
            return new DefaultMutableTreeNode(new PMDRuleNode((String)userObject));
        }
        return null;
    }

    /**
     * Convinience method to create root node for PMD plugin tree.
     *
     * @param resultPanel The Panel where the tree resides
     * @return The tree node to be used as root.
     */
    public DefaultMutableTreeNode createRootNode(PMDResultPanel resultPanel) {
        return new PMDRootNode(resultPanel);
    }
}
