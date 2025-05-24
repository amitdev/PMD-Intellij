package com.intellij.plugins.bodhi.pmd.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.pmd.lang.ast.FileAnalysisException;
import net.sourceforge.pmd.lang.ast.LexException;
import net.sourceforge.pmd.reporting.Report;

/**
 * Represents the actual error node user data. This will be data for leaf
 * nodes of the tree and encapsulates the PMD Report.ProcessingError.
 * Only core package classes are coupled with the PMD Library.
 *
 * @author jborgers
 */
public class PMDProcessingError implements HasPositionInFile {

    private final Report.ProcessingError processingError;
    private int beginLine = 0;
    private int beginColumn = 0;
    private final String positionText;

    private static final Pattern LOCATION_PATTERN = Pattern.compile("line (?<line>\\d+), column (?<column>\\d+)");

    public PMDProcessingError(Report.ProcessingError error) {
        processingError = error;
        if (error.getError() instanceof LexException) {
            beginLine = ((LexException) error.getError()).getLine();
            beginColumn = ((LexException) error.getError()).getColumn();
        } else if (error.getError() != null) {
            Matcher matcher = LOCATION_PATTERN.matcher(error.getDetail());
            if (matcher.find()) {
                beginLine = Integer.parseInt(matcher.group("line"));
                beginColumn = Integer.parseInt(matcher.group("column"));
            }
        }
        positionText = "(" + beginLine + ", " + beginColumn + ") ";
    }

    /**
     * Returns the simple class name and the throwable detail message.
     * @return the simple class name and the throwable detail message.
     */
    public String getMsg() {
        Throwable error = processingError.getError();
        if (error instanceof FileAnalysisException) {
            // a proper PMDException indicating for instance wrong java version
            return processingError.getMsg();
        }
        // error in PMD, for instance a NullPointerException, build our own message
        return error.getClass().getSimpleName() + ": Error while parsing " + processingError.getFileId();
    }

    /**
     * Returns the detail message string of the throwable.
     *
     * @return  the detail message string of this {@code Throwable} instance
     *          (which may be {@code null}).
     */
    public String getErrorMsg() {
        return processingError.getError().getMessage();
    }

    /**
     * Returns the Throwable.
     * @return the Throwable.
     */
    public Throwable getError() {
        return processingError.getError();
    }

    /**
     * Returns the detail message (stacktrace) including the cause of this throwable
     * (if any)
     * @return the detail message of throwable.
     */
    public String getCauseMsg() {
        return processingError.getDetail();
    }

    /**
     * Returns the position text to render.
     * @return the position text to render.
     */
    public String getPositionText() {
        return positionText;
    }

    /**
     * Returns the file during which the error occurred.
     * @return the file during which the error occurred.
     */
    @Override
    public String getFilePath() {
        return processingError.getFileId().getOriginalPath();
    }

    @Override
    public int getBeginLine() {
        return beginLine;
    }

    @Override
    public int getBeginColumn() {
        return beginColumn;
    }
}
