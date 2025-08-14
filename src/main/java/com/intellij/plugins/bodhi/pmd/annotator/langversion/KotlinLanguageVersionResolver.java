package com.intellij.plugins.bodhi.pmd.annotator.langversion;

import com.intellij.plugins.bodhi.pmd.PMDLanguageIds;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.base.projectStructure.LanguageVersionSettingsProviderUtils;
import org.jetbrains.kotlin.psi.KtFile;

public class KotlinLanguageVersionResolver implements LanguageVersionResolver {
    @Override
    public @Nullable Language resolveLanguage(@NotNull PsiFile file) {
        return file instanceof KtFile
                ? LanguageRegistry.PMD.getLanguageById(PMDLanguageIds.KOTLIN)
                : null;
    }

    @Override
    public @Nullable LanguageVersion resolveVersion(@NotNull Language language, @NotNull PsiFile file) {
        return file instanceof KtFile ktFile
                ? language.getVersion(LanguageVersionSettingsProviderUtils.getLanguageVersionSettings(ktFile)
                .getLanguageVersion()
                .getVersionString())
                : null;
    }
}
