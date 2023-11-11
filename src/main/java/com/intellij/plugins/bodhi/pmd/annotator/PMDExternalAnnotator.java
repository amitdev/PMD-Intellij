package com.intellij.plugins.bodhi.pmd.annotator;

import com.ibm.icu.impl.coll.Collation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.RuleViolation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Display PMD violations in the editor and in the problem view
 */
public class PMDExternalAnnotator extends ExternalAnnotator<FileInfo, PMDAnnotations> {
    @Override
    public FileInfo collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        return new FileInfo(file, editor.getDocument());
    }

    @Override
    public @Nullable PMDAnnotations doAnnotate(FileInfo info) {
        PMDProjectComponent projectComponent = info.getProject().getComponent(PMDProjectComponent.class);

        Set<String> ruleSets = projectComponent.getInEditorAnnotationRuleSets();

        if (ruleSets.isEmpty()) {
            return null;
        }

        PMDResultCollector collector = new PMDResultCollector();
        PMDAnnotationRenderer renderer = new PMDAnnotationRenderer();
        collector.runPMDAndGetResults(List.of(info.getFile()), List.copyOf(ruleSets), projectComponent, renderer);

        return renderer.getResult(info.getDocument());
    }

    @Override
    public void apply(@NotNull PsiFile file, PMDAnnotations annotationResult, @NotNull AnnotationHolder holder) {
        if (annotationResult == null) {
            return;
        }

        Document document = annotationResult.getDocument();
        for (RuleViolation violation : annotationResult.getReport().getViolations()) {
            int startLineOffset = document.getLineStartOffset(violation.getBeginLine() - 1);
            int endOffset = violation.getEndLine() - violation.getBeginLine() > 5 // Only mark first line for long violations
                    ? document.getLineEndOffset(violation.getBeginLine() - 1)
                    : document.getLineStartOffset(violation.getEndLine() - 1) + violation.getEndColumn();

            holder.newAnnotation(getSeverity(violation), "PMD: " + violation.getDescription())
                    .tooltip("PMD: " + violation.getRule().getName() +
                            "<p>" + violation.getDescription() +
                            "</p><p>" + violation.getRule().getDescription() + "</p>")
                    .range(TextRange.create(startLineOffset + violation.getBeginColumn() - 1, endOffset))
                    .needsUpdateOnTyping(true)
                    .withFix(new SupressIntentionAction(violation))
                    .create();
        }
    }

    private static HighlightSeverity getSeverity(RuleViolation violation) {
        switch (violation.getRule().getPriority()) {
            case HIGH:
                return HighlightSeverity.ERROR;
            case MEDIUM_HIGH:
            case MEDIUM:
                return HighlightSeverity.WARNING;
            case MEDIUM_LOW:
                return HighlightSeverity.WEAK_WARNING;
            case LOW:
                return HighlightSeverity.INFORMATION;
            default:
                throw new IllegalArgumentException();
        }
    }

}
