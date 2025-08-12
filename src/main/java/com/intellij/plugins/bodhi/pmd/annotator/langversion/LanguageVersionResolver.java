package com.intellij.plugins.bodhi.pmd.annotator.langversion;

import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LanguageVersionResolver {

    default int order() {
        return 1000;
    }

    @Nullable
    Language resolveLanguage(@NotNull PsiFile file);

    @Nullable
    LanguageVersion resolveVersion(@NotNull Language language, @NotNull PsiFile file);
}
