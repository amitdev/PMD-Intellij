package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.pom.Navigatable;
import com.intellij.plugins.bodhi.pmd.tree.PMDRootNode;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * This is a tree node that will encapsulate a PMD Violation. It implements
 * Navigatable, so that the user can navigate to the source of the problem.
 *
 * @author bodhi
 * @version 1.1
 */
class PMDResultNode extends DefaultMutableTreeNode implements Navigatable {

    /**
     * Create a node with the given user object.
     *
     * @param userObject The user object to encapsulate
     */
    public PMDResultNode(Object userObject) {
        super(userObject);
    }

    /**
     * Open editor and select/navigate to the correct line and column.
     *
     * @param requestFocus Focus the editor.
     */
    public void navigate(boolean requestFocus) {
        //The root has the resultpanel which can highligh the error.
        TreeNode rootNode = getRoot();
        if (rootNode instanceof PMDRootNode) {
            ((PMDRootNode)rootNode).getResultPanel().highlightError(this);
        }
    }

    public boolean canNavigate() {
        return true;
    }

    public boolean canNavigateToSource() {
        return true;
    }
}
