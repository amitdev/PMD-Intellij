package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.core.PMDResultAsTreeRenderer;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.psi.PsiFile;
import net.sourceforge.pmd.RuleViolation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Display PMD violations in the editor and in the problem view
 */
public class PMDExternalAnnotator extends ExternalAnnotator<FileInfo, PMDAnnotations> {
    private static final Log log = LogFactory.getLog(PMDExternalAnnotator.class);

    @Override
    public FileInfo collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        var virtualFile = file.getVirtualFile();
        // Can we get a real file for this virtual file?
        if (virtualFile.getFileSystem().getNioPath(virtualFile) == null) {
            return null;
        } else {
            return new FileInfo(file, editor.getDocument());
        }
    }

    @Override
    public @Nullable PMDAnnotations doAnnotate(FileInfo info) {
        PMDProjectComponent projectComponent = info.getProject().getComponent(PMDProjectComponent.class);
        if (projectComponent.getInEditorAnnotationRuleSets().isEmpty()) {
            return null;
        }

        PMDResultCollector collector = new PMDResultCollector();
        PMDAnnotationRenderer renderer = new PMDAnnotationRenderer();
        for (String ruleSetPath : projectComponent.getInEditorAnnotationRuleSets()) {
            collector.runPMDAndGetResults(List.of(info.getFile()), ruleSetPath, projectComponent, renderer);
        }

        return renderer.getResult(info.getDocument());
    }

    @Override
    public void apply(@NotNull PsiFile file, PMDAnnotations annotationResult, @NotNull AnnotationHolder holder) {
        if (annotationResult == null) {
            return;
        }

        Document document = annotationResult.getDocument();
        for (RuleViolation violation : annotationResult.getReport().getViolations()) {
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
                log.warn("Error while annotating file with PMD warnings: " + e.getMessage());
            }
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
