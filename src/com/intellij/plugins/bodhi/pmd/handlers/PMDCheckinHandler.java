package com.intellij.plugins.bodhi.pmd.handlers;

import com.intellij.CommonBundle;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class PMDCheckinHandler extends CheckinHandler {

    private static final String BUNDLE = "com.intellij.plugins.bodhi.pmd.PMD-Intellij";

    private final CheckinProjectPanel checkinProjectPanel;

    public PMDCheckinHandler(CheckinProjectPanel checkinProjectPanel) {
        this.checkinProjectPanel = checkinProjectPanel;
    }

    @Nullable
    public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
        JCheckBox checkBox = new JCheckBox(CommonBundle.message(ResourceBundle.getBundle(BUNDLE),
                "handler.before.checkin.checkbox"));

        return new RefreshableOnComponent() {
            @Override
            public JComponent getComponent() {
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(checkBox);
                return panel;
            }

            @Override
            public void refresh() {
            }

            @Override
            public void saveState() {
            }

            @Override
            public void restoreState() {
            }
        };
    }
}
