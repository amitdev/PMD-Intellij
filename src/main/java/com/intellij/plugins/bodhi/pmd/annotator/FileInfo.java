package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.lang.LanguageVersion;

public class FileInfo {

    private final PsiFile file;
    private final Document document;
    private final LanguageVersion languageVersion;

    public FileInfo(PsiFile file, Document document, LanguageVersion languageVersion) {
        this.file = file;
        this.document = document;
        this.languageVersion = languageVersion;
    }

    public Project getProject() {
        return file.getProject();
    }

    public String getFileName() {
        return file.getVirtualFile().getName();
    }

    public LanguageVersion getLanguageVersion() {
        return languageVersion;
    }

    public Document getDocument() {
        return document;
    }
}
