package com.intellij.plugins.bodhi.pmd.core;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.RuleViolation;

import java.io.File;

/**
 * Represents the actual violation node user data. This will be data for leaf
 * nodes of the tree and encapsulates the PMD RuleViolation.
 * Only this and PMDResultCollector classes (in core package) are coupled
 * with the PMD Library.
 *
 * @author bodhi
 * @version 1.2
 */
public class PMDViolation implements HasPositionInFile {

    private final RuleViolation ruleViolation;
    private final String positionText;
    private final String classAndMethodMsg;
    private final String packageMsg;

    /**
     * Creates a PMDViolation which wraps the IRuleViolation given.
     *
     * @param violation the violation
     */
    public PMDViolation(RuleViolation violation) {
        this.ruleViolation = violation;
        this.positionText = "(" + violation.getBeginLine() + ", " + violation.getBeginColumn() + ") ";
        String fileName = violation.getFilename();
        int startIndex = fileName.lastIndexOf(File.separatorChar);
        String className = violation.getClassName();
        if (className == null || className.length() == 0) {
            if (startIndex != -1) {
                className = fileName.substring(startIndex + 1, fileName.indexOf('.', startIndex));
            }
        }
        String methodName = violation.getMethodName();
        if (methodName == null) {
            methodName = "";
        }
        if (methodName.length() > 0) {
            methodName = "." + methodName + "()";
        }
        String packageName = violation.getPackageName();
        this.packageMsg = (packageName != null && packageName.trim().length() > 0) ? (" in " + packageName) : "";
        this.classAndMethodMsg = className + methodName;
    }

    @Override
    public String getFilename() {
        return ruleViolation.getFilename();
    }

    @Override
    public int getBeginLine() {
        return ruleViolation.getBeginLine();
    }

    @Override
    public int getBeginColumn() {
        return ruleViolation.getBeginColumn();
    }

    public int getEndLine() {
        return ruleViolation.getEndLine();
    }

    public int getEndColumn() {
        return ruleViolation.getEndColumn();
    }

    private Rule getRule() {
        return ruleViolation.getRule();
    }

    public String getDescription() {
        return ruleViolation.getDescription();
    }

    public String getPackageName() {
        return ruleViolation.getPackageName();
    }

    public String getMethodName() {
        return ruleViolation.getMethodName();
    }

    public String getClassName() {
        return ruleViolation.getClassName();
    }

    public boolean isSuppressed() {
        return ruleViolation.isSuppressed();
    }

    public String getVariableName() {
        return ruleViolation.getVariableName();
    }

    public String getPositionText() {
        return this.positionText;
    }

    public String getExternalUrl() {
        return getRule().getExternalInfoUrl();
    }

    public String getClassAndMethodMsg() {
        return classAndMethodMsg;
    }

    public String getPackageMsg() {
        return packageMsg;
    }

    public String getToolTip() {
        return getDescription();
    }

    public String getRuleName() {
        return getRule().getName();
    }

    public String getRulePriorityName() {
        return getRule().getPriority().getName();
    }

    public String toString() {
        return getPackageName() + "." + getClassName() + "." + getMethodName() + " at (" + getBeginLine() + "," + getBeginColumn() + ")";
    }

    public RulePriority getRulePriority() {
        return getRule().getPriority();
    }
}
