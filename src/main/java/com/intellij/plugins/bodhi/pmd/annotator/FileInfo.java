package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.lang.LanguageVersion;

public record FileInfo(PsiFile file, Document document, LanguageVersion languageVersion) {

    public Project getProject() {
        return file.getProject();
    }
}
