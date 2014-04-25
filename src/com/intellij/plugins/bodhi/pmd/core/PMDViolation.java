package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.plugins.bodhi.pmd.tree.PMDCellRenderer;
import com.intellij.plugins.bodhi.pmd.tree.PMDTreeNodeData;
import com.intellij.ui.SimpleTextAttributes;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.Rule;

import javax.swing.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the actual violation node user data. This will be data for leaf
 * nodes of the tree and encapsulates the PMD IRuleViolations.
 * Only this and PMDResultCollector classes (in core package) are coupled
 * with the PMD Library.
 *
 * @author bodhi
 * @version 1.1
 */
public class PMDViolation implements PMDTreeNodeData {

    private RuleViolation violation;
    private String errorPosition;
    private String[] errorMsg;

    /**
     * Creates a PMDViolation which wraps the IRuleViolation given.
     *
     * @param violation the violation
     */
    public PMDViolation(RuleViolation violation) {
        this.violation = violation;
        this.errorPosition = "(" + violation.getBeginLine() + ", " + violation.getBeginColumn() + ") ";
        String fileName = violation.getFilename();
        int startIndex = fileName.lastIndexOf(File.separatorChar);
        String className = violation.getClassName();
        if (className == null || className.length() == 0) {
            if (startIndex != -1) {
                className = fileName.substring(startIndex+1, fileName.indexOf('.', startIndex));
            }
        }
        String methodName = violation.getMethodName();
        if (methodName == null) {
            methodName = "";
        }
        if (methodName.length() > 0) {
            methodName = "." + methodName + "()";
        }
        this.errorMsg = new String[3];
        //this.errorMsg[0] = violation.getRule().getPriorityName();
        String packageName = violation.getPackageName();
        this.errorMsg[1] = (packageName != null && packageName.trim().length() > 0) ? (" in " + packageName) : "";
        this.errorMsg[2] = className+methodName;
    }

    public String getFilename() {
        return violation.getFilename();
    }

    public int getBeginLine() {
        return violation.getBeginLine();
    }

    public int getBeginColumn() {
        return violation.getBeginColumn();
    }

    public int getEndLine() {
        return violation.getEndLine();
    }

    public int getEndColumn() {
        return violation.getEndColumn();
    }

    public Rule getRule() {
        return violation.getRule();
    }

    public String getDescription() {
        return violation.getDescription();
    }

    public String getPackageName() {
        return violation.getPackageName();
    }

    public String getMethodName() {
        return violation.getMethodName();
    }

    public String getClassName() {
        return violation.getClassName();
    }

    public boolean isSuppressed() {
        return violation.isSuppressed();
    }

    public String getVariableName() {
        return violation.getVariableName();
    }

    public String getErrorPosition() {
        return this.errorPosition;
    }

    private String[] getErrorMsg() {
        return errorMsg;
    }

    public String getExternalUrl() {
        return getRule().getExternalInfoUrl();
    }

    public String toString() {
        return getPackageName()+"."+getClassName() + "." + getMethodName() + " at (" + getBeginLine() + "," + getBeginColumn() + ")";
    }

    public String getToolTip() {
        return getDescription();
    }

    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.setIcon(attrs.get(getRule().getPriority().getName()));
        //Show error position greyed, like idea shows.
        cellRenderer.append(getErrorPosition(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
        //cellRenderer.append(getErrorMsg()[0], attrs.get(getErrorMsg()[0]));
        cellRenderer.append(getErrorMsg()[2], SimpleTextAttributes.REGULAR_ATTRIBUTES);
        cellRenderer.append(getErrorMsg()[1], SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }
    private static final Map<String, Icon> attrs = new HashMap<String, Icon>();

    static {
        attrs.put("Medium", PMDCellRenderer.WARN);
        attrs.put("Medium High", PMDCellRenderer.WARN);
        attrs.put("High", PMDCellRenderer.ERROR);
        attrs.put("Medium Low", PMDCellRenderer.INFO);
        attrs.put("Low", PMDCellRenderer.INFO);
    }
}
