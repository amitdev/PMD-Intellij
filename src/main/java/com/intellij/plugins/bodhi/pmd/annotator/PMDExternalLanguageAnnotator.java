package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.annotator.langversion.ManagedLanguageVersionResolver;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Display PMD violations in the editor and in the problem view
 */
public abstract class PMDExternalLanguageAnnotator extends ExternalAnnotator<FileInfo, PMDAnnotations> {

    protected final Language language;
    protected final Logger logger;

    protected PMDExternalLanguageAnnotator(String languageId) {
        this.language = LanguageRegistry.PMD.getLanguageById(languageId);
        this.logger = Logger.getInstance(getClass());
    }

    @Override
    public FileInfo collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return new FileInfo(
                file,
                editor.getDocument(),
                new ManagedLanguageVersionResolver().resolveWithLang(language, file));
    }

    @Override
    public @Nullable PMDAnnotations doAnnotate(FileInfo info) {
        PMDProjectComponent projectComponent = info.getProject().getService(PMDProjectComponent.class);

        Set<String> inEditorAnnotationActiveRuleSets = projectComponent.getInEditorAnnotationRuleSets().stream()
                .filter(ruleSetPath -> isRuleSetForGivenFile(info, ruleSetPath))
                .collect(Collectors.toSet());

        if (inEditorAnnotationActiveRuleSets.isEmpty()) {
            return null;
        }

        PMDResultCollector collector = new PMDResultCollector();
        PMDAnnotationRenderer renderer = new PMDAnnotationRenderer();
        for (String ruleSetPath : inEditorAnnotationActiveRuleSets) {
            if (isRuleSetForGivenFile(info, ruleSetPath)) {
                collector.runPMDAndGetResultsForSingleFileNew(
                        info.file(),
                        info.languageVersion(),
                        ruleSetPath,
                        projectComponent,
                        renderer);
            }
        }

        return renderer.getResult(info.document());
    }

    private static boolean isRuleSetForGivenFile(FileInfo info, String ruleSetPath) {
        // This is a very basic check to see if RuleSet applies to the file:
        // it assumes the language id (e.g. "java" or "kotlin") is exclusively part of rule set path
        // (e.g. /category/java/bestpractices.xml or /home/user/jpinpoint-java-rules.xml).
        // This can fail if for (unexpected?) paths like: /home/user/kotlin/jpinpoint-java-rules.xml
        return ruleSetPath.contains(info.languageVersion().getLanguage().getId());
    }

    @Override
    public void apply(@NotNull PsiFile file, PMDAnnotations annotationResult, @NotNull AnnotationHolder holder) {
        if (annotationResult == null) {
            return;
        }

        Document document = annotationResult.document();
        for (RuleViolation violation : annotationResult.report().getViolations()) {
            int startLineOffset = document.getLineStartOffset(violation.getBeginLine()-1);
            int endOffset = violation.getEndLine() - violation.getBeginLine() > 5 // Only mark first line for long violations
                    ? document.getLineEndOffset(violation.getBeginLine()-1)
                    : document.getLineStartOffset(violation.getEndLine()-1) + violation.getEndColumn();

            try {
                var textRange = TextRange.create(startLineOffset + violation.getBeginColumn() - 1, endOffset);
                holder.newAnnotation(getSeverity(violation), "PMD: " + violation.getDescription())
                        .tooltip("PMD: " + violation.getRule().getName() +
                                "<p>" + violation.getDescription() +
                                "</p><p>" + violation.getRule().getDescription() + "</p>")
                        .range(textRange)
                        .needsUpdateOnTyping(true)
                        .withFix(new SupressIntentionAction(violation))
                        .create();
            } catch(IllegalArgumentException e) {
                // Catching "Invalid range specified" from TextRange.create thrown when file has been updated while analyzing
                logger.warn("Error while annotating file with PMD warnings", e);
            }
        }
    }

    private static HighlightSeverity getSeverity(RuleViolation violation) {
        return switch (violation.getRule().getPriority()) {
            case HIGH -> HighlightSeverity.ERROR;
            case MEDIUM_HIGH, MEDIUM -> HighlightSeverity.WARNING;
            case MEDIUM_LOW -> HighlightSeverity.WEAK_WARNING;
            case LOW -> HighlightSeverity.INFORMATION;
        };
    }
}
