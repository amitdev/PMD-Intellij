package com.intellij.plugins.bodhi.pmd.annotator.langversion;

import com.intellij.plugins.bodhi.pmd.PMDLanguageIds;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaLanguageVersionResolver implements LanguageVersionResolver {
    @Override
    public @Nullable Language resolveLanguage(@NotNull PsiFile file) {
        return file instanceof PsiJavaFile
            ? LanguageRegistry.PMD.getLanguageById(PMDLanguageIds.JAVA)
            : null;
    }

    @Override
    public @Nullable LanguageVersion resolveVersion(@NotNull Language language, @NotNull PsiFile file) {
        return file instanceof PsiJavaFile psiJavaFile
            ? language.getVersion(psiJavaFile.getLanguageLevel().toJavaVersion().toString())
            : null;
    }
}
