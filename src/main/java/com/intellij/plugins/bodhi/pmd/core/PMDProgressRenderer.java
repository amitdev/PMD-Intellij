package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.progress.ProgressIndicator;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.renderers.AbstractRenderer;
import net.sourceforge.pmd.util.datasource.DataSource;

import java.io.IOException;

public class PMDProgressRenderer extends AbstractRenderer {
    private final ProgressIndicator progress;
    private final int totalFiles;
    private int processedFiles = 0;

    public PMDProgressRenderer(ProgressIndicator progress, int totalFiles) {
        super("Progress", "Reports progress to IntelliJ");

        this.progress = progress;
        this.totalFiles = totalFiles;
    }

    @Override
    public String defaultFileExtension() {
        return null;
    }

    @Override
    public void start() throws IOException {
    }

    @Override
    public void startFileAnalysis(DataSource dataSource) {
        processedFiles++;
        progress.setFraction(processedFiles / (double) totalFiles);
        progress.setText2(dataSource.getNiceFileName(true, null));
    }

    @Override
    public void renderFileReport(Report report) throws IOException {

    }

    @Override
    public void end() throws IOException {

    }
}
