package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.PMDResultPanel;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Encapsulates the root node.
 *
 * @author bodhi
 * @version 1.0
 */
class PMDRootNode extends DefaultMutableTreeNode {

    /**
     * Lable of the root node.
     */
    public static final String LABEL = "PMD Results";

    //The panel where tree resides
    private PMDResultPanel resultPanel;

    /**
     * Creates a root node with given panel.
     *
     * @param resultPanel panel where tree resides
     */
    public PMDRootNode(PMDResultPanel resultPanel) {
        this(resultPanel, new PMDRuleNode(LABEL));
    }

    /**
     * Creates a root node with given panel and ruleNode.
     *
     * @param resultPanel panel where tree resides
     * @param ruleNode rule node to use.
     */
    public PMDRootNode(PMDResultPanel resultPanel, PMDRuleNode ruleNode) {
        super(ruleNode);
        this.resultPanel = resultPanel;
    }

    /**
     * Get the result panel in which the tree is present.
     *
     * @return the result panel
     */
    public PMDResultPanel getResultPanel() {
        return resultPanel;
    }
}