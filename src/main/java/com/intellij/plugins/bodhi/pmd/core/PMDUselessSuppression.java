package com.intellij.plugins.bodhi.pmd.core;

/**
 * Represents the useless suppression of violations node user data. This will be data for leaf
 * nodes of the tree and encapsulates the PMDViolation named "UsingSuppressWarnings".
 * Only core package classes are coupled with the PMD Library.
 *
 * @author jborgers
 */
public class PMDUselessSuppression implements HasPositionInFile {

    private final String suppressedRuleName;
    private final PMDViolation pmdViolation; //UsingSuppressWarnings

    public PMDUselessSuppression(PMDViolation pmdViolation, String ruleName) {
        this.suppressedRuleName = ruleName;
        this.pmdViolation = pmdViolation;
    }

    /**
     * Returns the name of the useless suppressed rule.
     * @return the name of the useless suppressed rule.
     */
    public String getSuppressedRuleName() {
        return suppressedRuleName;
    }

    @Override
    public String getFilePath() {
        return pmdViolation.getFilePath();
    }

    @Override
    public int getBeginLine() {
        return pmdViolation.getBeginLine();
    }

    @Override
    public int getBeginColumn() {
        return pmdViolation.getBeginColumn();
    }

    public String getPositionText() {
        return pmdViolation.getPositionText();
    }

    public String getClassAndMethodMsg() {
        return pmdViolation.getClassAndMethodMsg();
    }

    public String getPackageMsg() {
        return pmdViolation.getPackageMsg();
    }
}
