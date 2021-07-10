package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.PMDResultPanel;

import static com.intellij.ui.SimpleTextAttributes.ERROR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;

/**
 * Encapsulates the root node.
 *
 * @author bodhi
 * @version 1.1
 */
public class PMDRootNode extends PMDBranchNode {

    /**
     * Label of the root node.
     */
    private static final String LABEL = "PMD Results";

    /**
     * The panel where tree resides
     */
    private PMDResultPanel resultPanel;

    private int fileCount = -1;
    private int ruleSetCount = -1;
    private String exportErrorMessage = null;
    private String ruleSetErrorMsg = null;

    private volatile boolean running = false;

    /**
     * Creates a root node with given panel.
     *
     * @param panel panel where tree resides
     */
    public PMDRootNode(PMDResultPanel panel) {
        super(LABEL);
        resultPanel = panel;
    }

    /**
     * Get the result panel in which the tree is present.
     *
     * @return the result panel
     */
    public PMDResultPanel getResultPanel() {
        return resultPanel;
    }

    @Override
    public String getToolTip() {
        return null;
    }

    public void setFileCount(int count) {
        this.fileCount = count;
    }

    public void setRuleSetCount(int count) {
        this.ruleSetCount = count;
    }

    public void setRunning(boolean r) {
        running = r;
    }

    public void setExportErrorMsg(String exportErrMsg) {
        exportErrorMessage = exportErrMsg;
    }

    public void setRuleSetErrorMsg(String msg) { ruleSetErrorMsg = msg; }

    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.append(getNodeName());
        if (fileCount == 0) {
            cellRenderer.append(" No results: No source file(s) found to scan.");
            return;
        }
        if (running) {
            cellRenderer.append(" Processing...");
            return;
        }
        String result = " (" + getViolationCount() + " violation";
        if (getViolationCount() != 1) result += "s";
        if (getSuppressedCount() > 0) {
            result += ", " + getSuppressedCount() + " suppressed violation";
            if (getSuppressedCount() > 1) {
                result += "s";
            }
        }
        if (getErrorCount() > 0) {
            result += ", " + getErrorCount() + " processing error";
            if (getErrorCount() > 1) {
                result += "s";
            }
        }
        if (fileCount > -1) {
            result += " in " + fileCount + " scanned file";
            if (fileCount != 1) result += "s";
        }
        if (ruleSetCount > -1) {
            result += " using " + ruleSetCount + " rule set";
            if (ruleSetCount != 1) result += "s";
        }
        if (exportErrorMessage != null) {
            if (exportErrorMessage.length() == 0) {
                result += " - exported";
            }
            else {
                result += " - WARN: export failed: " + exportErrorMessage;
            }
        }
        cellRenderer.append(result + ")", GRAYED_ATTRIBUTES);

        if (ruleSetErrorMsg != null) {
            cellRenderer.append("  - ruleset ERROR: " + ruleSetErrorMsg, ERROR_ATTRIBUTES);
        }
    }


}