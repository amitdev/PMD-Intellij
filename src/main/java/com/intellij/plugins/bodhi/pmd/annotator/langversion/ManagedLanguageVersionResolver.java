package com.intellij.plugins.bodhi.pmd.annotator.langversion;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.bodhi.pmd.ConfigOption;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ManagedLanguageVersionResolver {
    private final Map<Language, Optional<LanguageVersion>> languageConfigVersionsCache = new HashMap<>();
    private final LanguageVersionResolverService resolverService =
            ApplicationManager.getApplication().getService(LanguageVersionResolverService.class);

    public LanguageVersion resolve(final PsiFile file) {
        final Language language = Objects.requireNonNull(resolverService.resolveLanguage(file)
                .orElseGet(() -> {
                    final String name = file.getName();
                    final String fileExtension = name.substring(name.lastIndexOf('.') + 1);
                    return LanguageRegistry.PMD.getLanguageById(
                            "kt".equalsIgnoreCase(fileExtension) || "kts".equalsIgnoreCase(fileExtension)
                                    ? "kotlin"
                                    : "java");
                }));

        return resolveWithLang(language, file);
    }

    public LanguageVersion resolveWithLang(final Language language, final PsiFile file) {
        return languageConfigVersionsCache.computeIfAbsent(language, lang -> {
                    final ConfigOption configOption = switch (language.getId()) {
                        case "java" -> ConfigOption.TARGET_JDK;
                        case "kotlin" -> ConfigOption.TARGET_KOTLIN_VERSION;
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
