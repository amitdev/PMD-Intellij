package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowType;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class PMDToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        PMDProjectComponent pmdComponent = project.getService(PMDProjectComponent.class);
        PMDResultPanel resultPanel = pmdComponent.getResultPanel();
        Content content = ContentFactory.getInstance().createContent(resultPanel, "", false);
        toolWindow.getContentManager().addContent(content);
        toolWindow.setType(ToolWindowType.DOCKED, null);
    }
}
