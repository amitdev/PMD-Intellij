package com.intellij.plugins.bodhi.pmd.annotator.langversion;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LanguageVersionResolverService {
    private final ExtensionPointName<LanguageVersionResolver> ep =
            ExtensionPointName.create("PMDPlugin.languageResolver");

    private List<LanguageVersionResolver> lastSeenExtensions;
    private List<LanguageVersionResolver> cachedOrderedResolvers;

    private List<LanguageVersionResolver> orderedResolvers()
    {
        final List<LanguageVersionResolver> extensions = ep.getExtensionList();
        if(cachedOrderedResolvers == null || extensions != lastSeenExtensions)
        {
            cachedOrderedResolvers = extensions
                    .stream()
                    .sorted(Comparator.comparingInt(LanguageVersionResolver::order))
                    .toList();
            lastSeenExtensions = extensions;
        }
        return cachedOrderedResolvers;
    }

    public Optional<Language> resolveLanguage(@NotNull PsiFile file)
    {
        return orderedResolvers()
                .stream()
                .map(r -> r.resolveLanguage(file))
                .filter(Objects::nonNull)
                .findFirst();
    }

    public Optional<LanguageVersion> resolveVersion(@NotNull Language language, @NotNull PsiFile file)
    {
        return ApplicationManager.getApplication().runReadAction((Computable<Optional<LanguageVersion>>) () -> orderedResolvers()
                .stream()
                .map(r -> r.resolveVersion(language, file))
                .filter(Objects::nonNull)
                .findFirst());
    }
}
