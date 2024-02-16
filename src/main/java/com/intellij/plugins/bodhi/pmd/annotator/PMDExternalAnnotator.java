package com.intellij.plugins.bodhi.pmd.annotator;

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
import net.sourceforge.pmd.internal.util.AssertionUtil;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.document.TextFile;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.ReaderDataSource;
import net.sourceforge.pmd.util.datasource.internal.LanguageAwareDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;

/**
 * Display PMD violations in the editor and in the problem view
 */
public class PMDExternalAnnotator extends ExternalAnnotator<FileInfo, PMDAnnotations> {
    @Override
    public FileInfo collectInformation(@NotNull PsiFile file, @NotNull Editor editor, boolean hasErrors) {
        var language = LanguageRegistry.findLanguageByTerseName("java");

        PMDProjectComponent projectComponent = file.getProject().getComponent(PMDProjectComponent.class);
        String type = projectComponent.getOptions().get("Target JDK");
        LanguageVersion version = type != null ? language.getVersion(type) : null;

        return new FileInfo(file, editor.getDocument(), version != null ? version : language.getDefaultVersion());
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
            collector.runPMDAndGetResults(List.of(), List.of(asTextFile(info)), ruleSetPath, projectComponent, renderer);
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

    private TextFile asTextFile(FileInfo info) {
        return new StringTextFile(
                info.getDocument().getText(),
                info.getFileName(),
                info.getFileName(),
                info.getLanguageVersion()
        );
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

    // Copied from PMD's StringTextFile since it was not public
    private static class StringTextFile implements TextFile {
        private final String content;
        private final String pathId;
        private final String displayName;
        private final LanguageVersion languageVersion;

        StringTextFile(String content, String pathId, String displayName, LanguageVersion languageVersion) {
            AssertionUtil.requireParamNotNull("source text", content);
            AssertionUtil.requireParamNotNull("file name", displayName);
            AssertionUtil.requireParamNotNull("file ID", pathId);
            AssertionUtil.requireParamNotNull("language version", languageVersion);
            this.languageVersion = languageVersion;
            this.content = content;
            this.pathId = pathId;
            this.displayName = displayName;
        }

        public LanguageVersion getLanguageVersion() {
            return this.languageVersion;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getPathId() {
            return this.pathId;
        }

        public String readContents() {
            return this.content;
        }

        public DataSource toDataSourceCompat() {
            return new LanguageAwareDataSource(new ReaderDataSource(new StringReader(this.content), this.pathId), this.languageVersion);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o != null && this.getClass() == o.getClass()) {
                var that = (StringTextFile)o;
                return Objects.equals(pathId, that.pathId);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return Objects.hash(new Object[]{pathId});
        }

        public String toString() {
            return this.getPathId();
        }
    }
}
