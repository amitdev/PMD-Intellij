package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.application.ApplicationManager;
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
        ApplicationManager.getApplication().invokeLater(() -> {
            JFrame frame = WindowManager.getInstance().getFrame(project);
            if (frame != null) {
                WindowFocusListener listener = new WindowFocusListener() {
                    @Override
                    public void windowGainedFocus(WindowEvent e) {
                        if (!project.isDisposed()) {
                            try {
                                PMDProjectComponent pmdComponent = project.getService(PMDProjectComponent.class);
                                if (pmdComponent != null) {
                                    pmdComponent.updateCustomMenuFromProject();
                                }
                            } catch (Exception ignored) {
                                // Project may be disposed during call
                            }
                        }
                    }

                    @Override
                    public void windowLostFocus(WindowEvent e) {
                    }
                };

                frame.addWindowFocusListener(listener);

                // Cleanup listener on project dispose
                PMDProjectComponent pmdProjComponent = project.getService(PMDProjectComponent.class);
                if (pmdProjComponent != null) {
                    Disposer.register(pmdProjComponent, () -> frame.removeWindowFocusListener(listener));
                }
            }
        });
        return Unit.INSTANCE;
    }
}


