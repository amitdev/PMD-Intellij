package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.openapi.editor.Document;
import net.sourceforge.pmd.Report;

public class PMDAnnotations {
    private final Report report;
    private final Document document;

    public PMDAnnotations(Report report, Document document) {
        this.report = report;
        this.document = document;
    }

    public Report getReport() {
        return report;
    }

    public Document getDocument() {
        return document;
    }
}
