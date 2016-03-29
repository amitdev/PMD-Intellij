package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PMDConfigurable implements Configurable {
    private PMDConfigurationForm form;
    private PMDProjectComponent component;

    public PMDConfigurable(Project project) {
        component = project.getComponent(PMDProjectComponent.class);
    }

    public String getDisplayName() {
        return "PMD";
    }

    @Nullable
    @NonNls
    public String getHelpTopic() {
        return null;
    }

    public JComponent createComponent() {
        if (form == null) {
            form = new PMDConfigurationForm();
        }
        return form.getRootPanel();
    }

    public boolean isModified() {
        return form != null && form.isModified(component);
    }

    public void apply() throws ConfigurationException {
        if (form != null) {
            form.getData(component);
        }
        component.updateCustomRulesMenu();
    }

    public void reset() {
        if (form != null) {
            form.setData(component);
        }
    }

    public void disposeUIResources() {
        form = null;
    }

}
