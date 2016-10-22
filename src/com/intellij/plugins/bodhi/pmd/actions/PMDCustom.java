package com.intellij.plugins.bodhi.pmd.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PMDCustom extends ActionGroup {

    DefaultActionGroup children = new DefaultActionGroup("Custom Rules", true);

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        return new AnAction[] {children};
    }


    public void add(AnAction action) {
        children.add(action);
    }


    public void remove(AnAction action) {
        children.remove(action);
    }
}
