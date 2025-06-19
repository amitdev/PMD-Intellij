package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.HasMessage;
import com.intellij.plugins.bodhi.pmd.core.PMDUselessSuppression;

import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;

/**
 * Tree leaf node that has a useless suppression PMD Violation. It is
 * Navigatable, so that the user can navigate to the source of the violation.
 *
 * @author jborgers
 */
public class PMDUselessSuppressionNode extends PMDLeafNode implements HasMessage {

    private final PMDUselessSuppression uselessSuppression;

    /**
     * Create a node with the given useless suppression.
     *
     * @param suppression The violation to encapsulate
     */
    public PMDUselessSuppressionNode(PMDUselessSuppression suppression) {
        uselessSuppression = suppression;
    }

    /**
     * Open editor and select/navigate to the correct line and column.
     *
     * @param requestFocus Focus the editor.
     */
    public void navigate(boolean requestFocus) {
        highlightFindingInEditor(uselessSuppression);
    }

    @Override
    public String getToolTip() {
        return "Using @SuppressWarnings for a rule while there is actually no such violation to suppress";
    }

    @Override
    public String getMessage() {
        return "Using @SuppressWarnings for rule " + uselessSuppression.getSuppressedRuleName() + " while there is actually no such violation to suppress.";
    }

    @Override
    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.setIcon(Severity.MEDIUM.getIcon());
        cellRenderer.append("useless suppression of: ", GRAYED_ATTRIBUTES);
        cellRenderer.append(uselessSuppression.getSuppressedRuleName() + " ");
        cellRenderer.append(uselessSuppression.getPositionText(), GRAYED_ATTRIBUTES);
        cellRenderer.append(uselessSuppression.getClassAndMethodMsg());
        cellRenderer.append(uselessSuppression.getPackageMsg(), GRAYED_ATTRIBUTES);
    }

    @Override
    public int getUselessSuppressionCount() {
        return 1;
    }

}

