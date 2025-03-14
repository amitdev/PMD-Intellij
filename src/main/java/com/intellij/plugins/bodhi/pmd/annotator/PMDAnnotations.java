package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.openapi.editor.Document;
import net.sourceforge.pmd.reporting.Report;

public record PMDAnnotations(Report report,
                             Document document) {
}
