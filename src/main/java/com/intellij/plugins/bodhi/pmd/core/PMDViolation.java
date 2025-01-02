package com.intellij.plugins.bodhi.pmd.core;


import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.reporting.RuleViolation;

import static net.sourceforge.pmd.reporting.RuleViolation.*;

/**
 * Represents the actual violation node user data. This will be data for leaf
 * nodes of the tree and encapsulates the PMD RuleViolation.
 * Only this and PMDResultCollector classes (in core package) are coupled
 * with the PMD Library.
 *
 * @author bodhi
 * @version 1.2
 */
public class PMDViolation implements HasPositionInFile, HasRule, HasMessage {

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
        String fileName = violation.getFileId().getFileName();
        String className = violation.getAdditionalInfo().get(CLASS_NAME);
        if (className == null || className.isEmpty()) {
            var endIndex = fileName.indexOf('.');
            if (endIndex != -1) {
                className = fileName.substring(0, endIndex);
            }
        }
        String methodName = violation.getAdditionalInfo().get(METHOD_NAME);
        if (methodName == null) {
            methodName = "";
        }
        if (!methodName.isEmpty()) {
            methodName = "." + methodName + "()";
        }
        String packageName = violation.getAdditionalInfo().get(PACKAGE_NAME);
        this.packageMsg = (packageName != null && !packageName.trim().isEmpty()) ? (" in " + packageName) : "";
        this.classAndMethodMsg = className + methodName;
    }

    @Override
    public String getFilePath() {
        return ruleViolation.getFileId().getOriginalPath();
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

    public Rule getRule() {
        return ruleViolation.getRule();
    }

    public String getDescription() {
        return ruleViolation.getDescription();
    }

    @Override
    public String getMessage() {
        return getDescription();
    }

    public String getPackageName() {
        return ruleViolation.getAdditionalInfo().get(PACKAGE_NAME);
    }

    public String getMethodName() {
        return ruleViolation.getAdditionalInfo().get(METHOD_NAME);
    }

    public String getClassName() {
        return ruleViolation.getAdditionalInfo().get(CLASS_NAME);
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
