package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.wm.WindowManager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.util.Disposer;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

public class PMDProjectSwitchListener implements ProjectActivity {

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        setupWindowFocusListener(project);
        return Unit.INSTANCE;
    }

    private void setupWindowFocusListener(@NotNull Project project) {
        if (project.isDisposed()) {
            return;
        }

        try {
            JFrame frame = WindowManager.getInstance().getFrame(project);
            if (frame != null) {
                WindowFocusListener listener = new WindowFocusListener() {
                    @Override
                    public void windowGainedFocus(WindowEvent e) {
                        if (!project.isDisposed()) {
                            PMDProjectComponent pmdComponent = project.getService(PMDProjectComponent.class);
                            if (pmdComponent != null) {
                                pmdComponent.updateCustomMenuFromProject();
                            }
                        }
                    }

                    @Override
                    public void windowLostFocus(WindowEvent e) {
                        // Not used
                    }
                };

                frame.addWindowFocusListener(listener);

                PMDProjectComponent pmdComponent = project.getService(PMDProjectComponent.class);
                if (pmdComponent != null) {
                    Disposer.register(pmdComponent, () -> frame.removeWindowFocusListener(listener));
                }
            }
        } catch (Exception e) {
            // Ignore any startup-related exceptions
            // Next time the project gets focus, it will work properly
        }
    }
}



