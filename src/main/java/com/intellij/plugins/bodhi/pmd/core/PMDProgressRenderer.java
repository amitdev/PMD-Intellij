package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.progress.ProgressIndicator;
import net.sourceforge.pmd.lang.document.TextFile;
import net.sourceforge.pmd.renderers.AbstractRenderer;
import net.sourceforge.pmd.reporting.Report;

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
    public void startFileAnalysis(TextFile dataSource) {
        processedFiles++;
        progress.setFraction(processedFiles / (double) totalFiles);
        progress.setText2(dataSource.getFileId().getOriginalPath());
    }

    @Override
    public void flush() {
    }

    @Override
    public void start() {
    }

    @Override
    public void renderFileReport(Report report) {
    }

    @Override
    public void end() {
    }

}
