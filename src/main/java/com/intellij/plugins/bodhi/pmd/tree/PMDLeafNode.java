package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.HasPositionInFile;

/**
 * Abstract base class for PMD Leaf Tree Nodes.
 * Contains a finding: violation, suppressed violation or processing error.
 * This has a location in a file and the containing node is navigatable.
 *
 * @author jborgers
 */
public abstract class PMDLeafNode extends BasePMDNode {
    public boolean canNavigate() {
        return true;
    }

    public boolean canNavigateToSource() {
        return true;
    }

    @Override
    public int getViolationCount() {
        return 0;
    }

    @Override
    public int getSuppressedCount() {
        return 0;
    }

    @Override
    public int getErrorCount() {
        return 0;
    }

    @Override
    public int getSevViolationCount(Severity sev) {
        return 0;
    }

    /**
     * Open editor and select/navigate to the correct line and column in the file.
     *
     * @param finding The violation, suppressed violation or processing error.
     */
    protected void highlightFindingInEditor(HasPositionInFile finding) {
        getRootResultPanel().highlightFindingInEditor(finding);
    }

}
