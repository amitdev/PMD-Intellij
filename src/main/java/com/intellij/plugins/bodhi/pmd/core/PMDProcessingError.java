package com.intellij.plugins.bodhi.pmd.core;

import net.sourceforge.pmd.Report;

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

    public PMDProcessingError(Report.ProcessingError error) {
        processingError = error;
        Throwable cause = error.getError().getCause(); // fix #137
        if (cause != null) {
            String causeDetailMsg = cause.getMessage();
            // assumes format by PMD: 'Line \d+, Column \d+:'
            int atLinePos = causeDetailMsg.indexOf("Line ");
            int columnPos = causeDetailMsg.indexOf(", Column ");
            int colonPos = causeDetailMsg.indexOf(":", columnPos);
            if (atLinePos > -1 && columnPos > atLinePos && colonPos > columnPos) {
                try {
                    String line = causeDetailMsg.substring(atLinePos + 5, columnPos);
                    String col = causeDetailMsg.substring(columnPos + 9, colonPos);
                    beginLine = Integer.parseInt(line);
                    beginColumn = Integer.parseInt(col);
                }
                catch(NumberFormatException | StringIndexOutOfBoundsException e) { // no beginLine, beginColumn
                }
            }
        }
        positionText = "(" + beginLine + ", " + beginColumn + ") ";
    }

    /**
     * Returns the simple class name and the throwable detail message.
     * @return the simple class name and the throwable detail message.
     */
    public String getMsg() {
        // PMDException (deprecated) has a cause
        Throwable error = processingError.getError();
        if (error.getCause() != null) {
            // a proper PMDException indicating for instance wrong java version
            return processingError.getMsg(); // error class simple name and message
        }
        // error in PMD, for instance a NullPointerException, build our own message
        return error.getClass().getSimpleName() + ": Error while parsing " + processingError.getFile();
    }

    /**
     * Returns the detail message string of the throwable.
     *
     * @return  the detail message string of this {@code Throwable} instance
     *          (which may be {@code null}).
     */public String getErrorMsg() {
        return processingError.getError().getMessage();
    }

    /**
     * Returns the file during which the error occurred.
     * @return the file during which the error occurred.
     */
    public String getFile() {
        return processingError.getFile();
    }

    /**
     * Returns the Throwable.
     * @return the Throwable.
     */
    public Throwable getError() {
        return processingError.getError();
    }

    /**
     * Returns the detail message of the cause of this throwable or {@code null} if the
     * cause is nonexistent or unknown.  (The cause is the throwable that
     * caused this throwable to get thrown.)
     * When cause == null, it returns throw location of the throwable itself.
     * @return the detail message of the cause throwable.
     */
    public String getCauseMsg() {
        Throwable error = processingError.getError();
        Throwable cause = error.getCause();
        if (cause != null) {
            return cause.getMessage();
        }
        else {
            // if no cause, it may be a bug in PMD, get first stack element
            return error.getStackTrace()[0].toString();
        }
      }

    /**
     * Returns the position text to render.
     * @return the position text to render.
     */
    public String getPositionText() {
        return positionText;
    }

    @Override
    public String getFilename() {
        return processingError.getFile();
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
