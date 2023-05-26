package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.PMDProcessingError;
import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;

/**
 * PMD leaf tree node for processing errors. It is Navigatable,
 * so that the user can navigate to the source of the processing error.
 *
 * @author jborgers
 */
public class PMDErrorNode extends PMDLeafNode {

    private final PMDProcessingError pmdProcessingError;

    public PMDErrorNode(PMDProcessingError error) {
        pmdProcessingError = error;
    }

    @Override
    public String getToolTip() {
        return pmdProcessingError.getCauseMsg();
    }

    @Override
    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.setIcon(Severity.BLOCKER.getIcon());
        // Show error position greyed, like idea shows.
        cellRenderer.append(pmdProcessingError.getPositionText(), GRAYED_ATTRIBUTES);
        cellRenderer.append(pmdProcessingError.getMsg()); // includes simple class and file name
    }

    @Override
    public int getErrorCount() {
        return 1;
    }

    /**
     * Open editor and select/navigate to the correct line and column.
     *
     * @param requestFocus Focus the editor.
     */
    public void navigate(boolean requestFocus) {
        highlightFindingInEditor(pmdProcessingError);
    }
}
