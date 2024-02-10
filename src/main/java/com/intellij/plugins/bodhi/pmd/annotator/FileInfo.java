package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.lang.LanguageVersion;

import java.io.File;

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

    public File getFile() {
        return file.getVirtualFile().toNioPath().toFile();
    }

    public LanguageVersion getLanguageVersion() {
        return languageVersion;
    }

    public Document getDocument() {
        return document;
    }
}
