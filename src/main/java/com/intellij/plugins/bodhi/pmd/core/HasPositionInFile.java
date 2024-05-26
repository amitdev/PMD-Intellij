package com.intellij.plugins.bodhi.pmd.core;

/**
 * Implementing classes have a position in a file.
 * @author jborgers
 */
public interface HasPositionInFile {
    /**
     * Returns the file name.
     * @return the file name.
     */
    String getFilePath();

    /**
     * Returns the begin line of the position.
     * @return the begin line of the position.
     */
    int getBeginLine();

    /**
     * Returns the begin column of the position.
     * @return the begin column of the position.
     */
    int getBeginColumn();
}
