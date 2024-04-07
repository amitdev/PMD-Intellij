package com.intellij.plugins.bodhi.pmd.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * The main menu group for the PMD Plugin. This will contain the Predefined
 * as well as custom rules menu.
 *
 * @author bodhi
 * @version 1.0
 */
public class PMDMenuGroup extends DefaultActionGroup {

    public void update(AnActionEvent e) {
        super.update(e);
        Presentation presentation = e.getPresentation();
        Project project = e.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            //If no project defined, disable the menu item
            presentation.setEnabled(false);
            presentation.setVisible(false);
            return;
        } else {
            presentation.setVisible(true);
        }
        if (e.getPlace().equals(ActionPlaces.MAIN_MENU)) {
            //Enabled only if some files are selected.
            VirtualFile[] selectedFiles = FileEditorManager.getInstance(project).getSelectedFiles();
            presentation.setEnabled(selectedFiles.length != 0);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
