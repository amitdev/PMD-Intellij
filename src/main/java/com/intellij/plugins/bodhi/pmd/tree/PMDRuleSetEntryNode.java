package com.intellij.plugins.bodhi.pmd.tree;

/**
 * Abstract class for a Branch node which is a direct entry of a rule set node and is Comparable so can be sorted.
 *
 * @author jborgers
 */
public abstract class PMDRuleSetEntryNode extends PMDBranchNode implements Comparable<PMDRuleSetEntryNode> {
    /**
     * Create a node with the given value as node name.
     *
     * @param nodeName The node name to set.
     */
    public PMDRuleSetEntryNode(String nodeName) {
        super(nodeName);
    }

}
