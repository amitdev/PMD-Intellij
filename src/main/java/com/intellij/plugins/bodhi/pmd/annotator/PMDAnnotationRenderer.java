package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.openapi.editor.Document;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.renderers.AbstractRenderer;
import net.sourceforge.pmd.util.datasource.DataSource;

import java.io.IOException;

class PMDAnnotationRenderer extends AbstractRenderer {

    private Report report;

    PMDAnnotationRenderer() {
        super("Annotations", "Gathers data for annotating IntelliJ editor");
    }

    @Override
    public String defaultFileExtension() {
        return null;
    }

    @Override
    public void start() {
    }

    @Override
    public void startFileAnalysis(DataSource dataSource) {

    }

    @Override
    public void renderFileReport(Report report) throws IOException {
        this.report = report;
    }

    @Override
    public void end() throws IOException {
    }

    @Override
    public void flush() {
    }

    public PMDAnnotations getResult(Document document) {
        return new PMDAnnotations(report, document);
    }
}
