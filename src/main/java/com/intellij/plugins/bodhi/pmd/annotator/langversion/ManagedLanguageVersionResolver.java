package com.intellij.plugins.bodhi.pmd.annotator.langversion;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.bodhi.pmd.ConfigOption;
import com.intellij.plugins.bodhi.pmd.PMDLanguageIds;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ManagedLanguageVersionResolver {
    private final Map<Language, Optional<LanguageVersion>> languageConfigVersionsCache = new HashMap<>();
    private final LanguageVersionResolverService resolverService =
            ApplicationManager.getApplication().getService(LanguageVersionResolverService.class);

    public Optional<LanguageVersion> resolveLanguage(final PsiFile file) {
        return resolverService.resolveLanguage(file)
                .or(() -> {
                    final String name = file.getName();
                    final String fileExtension = name.substring(name.lastIndexOf('.') + 1).toLowerCase();

                    final String langId = switch (fileExtension) {
                        case "java" -> PMDLanguageIds.JAVA;
                        case "kt", "kts" -> PMDLanguageIds.KOTLIN;
                        default -> null;
                    };
                    if(langId == null) {
                        return Optional.empty();
                    }

                    return Optional.ofNullable(LanguageRegistry.PMD.getLanguageById(langId));
                })
                .map(lang -> resolveWithLang(lang, file));
    }

    @NotNull
    public LanguageVersion resolveWithLang(@NotNull final Language language, @NotNull final PsiFile file) {
        return languageConfigVersionsCache.computeIfAbsent(language, lang -> {
                    final ConfigOption configOption = switch (language.getId()) {
                        case PMDLanguageIds.JAVA -> ConfigOption.TARGET_JDK;
                        case PMDLanguageIds.KOTLIN -> ConfigOption.TARGET_KOTLIN_VERSION;
                        default -> null;
                    };

                    return Optional.ofNullable(configOption)
                            .map(opt -> language.getVersion(
                                    file.getProject()
                                            .getService(PMDProjectComponent.class)
                                            .getOptionToValue()
                                            .get(opt)));
                })
                .orElseGet(() -> resolverService.resolveVersion(language, file)
                        // Fallback to latest version
                        .orElseGet(language::getLatestVersion));
    }
}
