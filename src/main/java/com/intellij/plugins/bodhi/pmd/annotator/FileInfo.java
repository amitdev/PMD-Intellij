package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import java.io.File;

public class FileInfo {

    private final PsiFile file;
    private final Document document;

    public FileInfo(PsiFile file, Document document) {
        this.file = file;
        this.document = document;
    }

    public Project getProject() {
        return file.getProject();
    }

    public File getFile() {
        return file.getVirtualFile().toNioPath().toFile();
    }

    public Document getDocument() {
        return document;
    }
}
