package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ProjectActivity to initialize PMD plugin when a project is opened.
 * This replaces the deprecated StartupActivity approach.
 */
public class PMDProjectActivity implements ProjectActivity
{
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        PMDProjectComponent pmdComponent = project.getService(PMDProjectComponent.class);
        if (pmdComponent != null) {
            pmdComponent.updateCustomMenuFromProject();
        }
        return Unit.INSTANCE;
    }
}
