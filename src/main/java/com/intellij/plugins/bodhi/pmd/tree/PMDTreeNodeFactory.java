package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.PMDResultPanel;
import com.intellij.plugins.bodhi.pmd.core.PMDProcessingError;
import com.intellij.plugins.bodhi.pmd.core.PMDSuppressedViolation;
import com.intellij.plugins.bodhi.pmd.core.PMDViolation;
import net.sourceforge.pmd.Rule;

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
     * Creates a rule set tree node object
     *
     * @param name the rule set node name
     * @return The created node
     */
    public PMDRuleSetNode createRuleSetNode(String name) {
        return new PMDRuleSetNode(name);
    }

    /**
     * Creates a rule branch tree node object
     *
     * @param rule the branch node rule
     * @return The created node
     */
    public PMDRuleNode createRuleNode(Rule rule) {
        return new PMDRuleNode(rule);
    }

    /**
     * Creates a suppressed branch tree node object
     *
     * @param name the branch node name
     * @return The created node
     */
    public PMDSuppressedBranchNode createSuppressedBranchNode(String name) {
        return new PMDSuppressedBranchNode(name);
    }

    /**
     * Creates a processing error branch tree node object
     *
     * @param name the branch node name
     * @return The created node
     */
    public PMDErrorBranchNode createErrorBranchNode(String name) {
        return new PMDErrorBranchNode(name);
    }

    /**
     * Creates a tree leaf node object for the violation
     *
     * @param violation PMDViolation that will be wrapped.
     * @return The created tree node 
     */
    public PMDViolationNode createViolationLeafNode(PMDViolation violation) {
        return new PMDViolationNode(violation);
    }

    /**
     * Creates a tree leaf node object for the suppressed violation
     *
     * @param suppressed the suppressed PMDViolation that will be wrapped.
     * @return The created tree node
     */
    public PMDSuppressedNode createSuppressedLeafNode(PMDSuppressedViolation suppressed) {
        return new PMDSuppressedNode(suppressed);
    }

    /**
     * Creates a tree leaf node object for the processing error
     *
     * @param error the PMDProcessingError that will be wrapped.
     * @return The created tree node
     */
    public PMDErrorNode createErrorLeafNode(PMDProcessingError error) {
        return new PMDErrorNode(error);
    }

    /**
     * Convinience method to create root node for PMD plugin tree.
     *
     * @param resultPanel The Panel where the tree resides
     * @return The tree node to be used as root.
     */
    public PMDRootNode createRootNode(PMDResultPanel resultPanel) {
        return new PMDRootNode(resultPanel);
    }


}
