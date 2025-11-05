package com.intellij.plugins.bodhi.pmd.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.bodhi.pmd.PMDResultPanel.PMD_SUPPRESSION;

public class SupressIntentionAction implements IntentionAction, PriorityAction {
    @SafeFieldForPreview
    private final RuleViolation violation;

    public SupressIntentionAction(RuleViolation violation) {
        this.violation = violation;
    }

    @Override
    public @IntentionName @NotNull String getText() {
        return "Suppress PMD " + violation.getRule().getName();
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return "PMD";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        int offset = editor.getDocument().getLineEndOffset(violation.getBeginLine()-1);
        //Append PMD special comment to end of line.
        editor.getDocument().insertString(offset, " " + PMD_SUPPRESSION + " - suppressed " + violation.getRule().getName() + " - TODO explain reason for suppression");
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @NotNull
    @Override
    public Priority getPriority() {
        // Slightly lower priority so that the other "Suppress for member" or similar is prioritized and not this one
        // See DefaultIntentionsOrderProvider for more details
        return Priority.LOW;
    }
}
