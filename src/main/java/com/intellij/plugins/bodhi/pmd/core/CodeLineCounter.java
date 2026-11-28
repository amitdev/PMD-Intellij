package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.plugins.bodhi.pmd.annotator.langversion.ManagedLanguageVersionResolver;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.SyntaxTraverser;
import com.intellij.psi.util.PsiTreeUtil;
import net.sourceforge.pmd.lang.Language;
import org.jetbrains.annotations.NotNull;

import java.util.BitSet;
import java.util.List;
import java.util.Set;

/**
 * Counts the non-comment, non-blank lines of a file, language independent, based on the PSI tree.
 */
public final class CodeLineCounter {

    private CodeLineCounter() {
    }

    /**
     * Counts the code lines of the files whose PMD language is one of the given languages.
     * Takes a read action per file, so write actions are not blocked for long; skips null (unresolved) and invalid files.
     *
     * @param indicator checked for cancellation between files
     * @return the total number of code lines
     * @throws com.intellij.openapi.progress.ProcessCanceledException when the indicator is cancelled
     */
    public static int countCodeLines(@NotNull List<PsiFile> files, @NotNull Set<Language> languages,
                                     @NotNull ProgressIndicator indicator) {
        if (languages.isEmpty()) {
            return 0;
        }
        ManagedLanguageVersionResolver resolver = new ManagedLanguageVersionResolver();
        int total = 0;
        for (PsiFile file : files) {
            indicator.checkCanceled();
            if (file != null) {
                total += ReadAction.compute(() -> countIfOfLanguage(file, languages, resolver));
            }
        }
        return total;
    }

    private static int countIfOfLanguage(PsiFile file, Set<Language> languages, ManagedLanguageVersionResolver resolver) {
        try {
            if (!file.isValid() || resolver.resolveLanguage(file)
                    .map(version -> !languages.contains(version.getLanguage()))
                    .orElse(true)) {
                return 0;
            }
            return countCodeLines(file);
        } catch (PsiInvalidElementAccessException e) {
            return 0; // file deleted or changed while counting
        }
    }

    /**
     * A line counts when at least one PSI leaf on it is neither whitespace nor (part of) a comment.
     * Uses the in-memory content of the file. The caller must hold read access.
     *
     * @return the number of code lines
     */
    public static int countCodeLines(@NotNull PsiFile file) {
        CharSequence text = file.getViewProvider().getContents();
        BitSet codeLines = new BitSet();
        for (PsiElement leaf : SyntaxTraverser.psiTraverser(file).filter(e -> e.getFirstChild() == null)) {
            TextRange range = leaf.getTextRange();
            if (range.isEmpty() || leaf instanceof PsiWhiteSpace || PsiTreeUtil.getParentOfType(leaf, PsiComment.class, false) != null) {
                continue;
            }
            int firstLine = StringUtil.offsetToLineNumber(text, range.getStartOffset());
            int lastLine = StringUtil.offsetToLineNumber(text, range.getEndOffset() - 1);
            codeLines.set(firstLine, lastLine + 1);
        }
        return codeLines.cardinality();
    }
}
