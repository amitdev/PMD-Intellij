package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.PMDSuppressedViolation;

import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;

/**
 * Tree leaf node that has a PMD Suppressed Violation. It is
 * Navigatable, so that the user can navigate to the source of the suppressed violation.
 *
 * @author jborgers
 */
public class PMDSuppressedNode extends PMDLeafNode {

    private PMDSuppressedViolation pmdSuppressedViolation;

    /**
     * Create a node with the given suppressed pmd violation.
     *
     * @param pmdSuppressed The suppressed violation to encapsulate
     */
    public PMDSuppressedNode(PMDSuppressedViolation pmdSuppressed) {
        pmdSuppressedViolation = pmdSuppressed;
    }

    /**
     * Open editor and select/navigate to the correct line and column.
     *
     * @param requestFocus Focus the editor.
     */
    public void navigate(boolean requestFocus) {
        highlightFindingInEditor(pmdSuppressedViolation);
    }

    @Override
    public String getToolTip() {
        final String userMessage = pmdSuppressedViolation.getUserMessage();
        if (pmdSuppressedViolation.suppressedByNOPMD()) {
            // NOPMD should be followed by a reason explaining the suppression
            if (containsNoReasonDescription(userMessage)) {
                return "Warn: No reason for suppression documented";
            }
            else {
                return "Reason: " + userMessage;
            }
        }
        return null; // Annotation cannot include a reason, should be documented in // comment
    }

    private boolean containsNoReasonDescription(String userMessage) {
        return userMessage == null || userMessage.isEmpty() || userMessage.length() < 3
                || (userMessage.contains("NOSONAR") && userMessage.length() < 13);
    }

    @Override
    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
        final String userMessage = pmdSuppressedViolation.getUserMessage();
        String reason;
        if (pmdSuppressedViolation.suppressedByNOPMD()) {
            // NOPMD should be followed by a reason explaining the suppression
            if (containsNoReasonDescription(userMessage)) {
                cellRenderer.setIcon(PMDCellRenderer.WARN);
            }
            else {
                cellRenderer.setIcon(PMDCellRenderer.INFO);
            }
        }
        else { // suppressed by Annotation has no option to describe the reason, should be documented in // comment
            cellRenderer.setIcon(PMDCellRenderer.INFO);
        }
        cellRenderer.append("suppressed: ", GRAYED_ATTRIBUTES);
        cellRenderer.append(pmdSuppressedViolation.getPMDViolation().getRuleName() + " ");
        cellRenderer.append("for " + pmdSuppressedViolation.getPMDViolation().getPositionText(), GRAYED_ATTRIBUTES);
        cellRenderer.append(pmdSuppressedViolation.getPMDViolation().getClassAndMethodMsg());
        cellRenderer.append(pmdSuppressedViolation.getPMDViolation().getPackageMsg(), GRAYED_ATTRIBUTES);
    }

    @Override
    public int getSuppressedCount() {
        return 1;
    }
}

