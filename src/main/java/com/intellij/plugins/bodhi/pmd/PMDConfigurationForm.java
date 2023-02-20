package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.bodhi.pmd.core.PMDJsonExportingRenderer;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * This class represents the UI for settings.
 *
 * @author bodhi
 * @version 1.1
 */
public class PMDConfigurationForm {
    private JPanel rootPanel;
    private JList ruleList;
    private JPanel buttonPanel;
    private JTabbedPane tabbedPane1;
    private JTable table1;
    private JPanel mainPanel;
    private JCheckBox skipTestsCheckBox;

    private boolean isModified;
    private final Project project;

    public static final String STATISTICS_URL = "Statistics URL";
    private static final String[] columnNames = new String[] {"Option", "Value"};
    private static final String[] optionNames = new String[] {"Target JDK", STATISTICS_URL};
    private static final String[] defaultValues = new String[] {"17", ""};
    private static final String STAT_URL_MSG = "Fill in Statistics URL endpoint to export anonymous PMD-Plugin usage statistics";
    private static final String STAT_URL_MSG_SUCCESS = "Could connect, will use Statistics URL endpoint to export anonymous PMD-Plugin usage statistics";

    public PMDConfigurationForm(final Project project) {
        this.project = project;
        //Get the action group defined
        DefaultActionGroup actionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("PMDSettingsEdit");
        //Remove toolbar actions associated to previous form
        actionGroup.removeAll();
        //Add the toolbar actions associated to this form to it
        actionGroup.add(new AddRuleSetAction("Add", "Add a custom ruleset", PlatformIcons.ADD_ICON));
        actionGroup.add(new EditRuleSetAction("Edit", "Edit selected ruleset", IconLoader.getIcon("/actions/editSource.png", PMDConfigurationForm.class)));
        actionGroup.add(new DeleteRuleSetAction("Delete", "Remove selected ruleset", IconLoader.getIcon("/general/remove.png", PMDConfigurationForm.class)));
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("modify actions", actionGroup, true);
        toolbar.getComponent().setVisible(true);
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(toolbar.getComponent(), BorderLayout.CENTER);

        table1.putClientProperty("terminateEditOnFocusLost", true); // fixes issue #45
        ruleList.setModel(new MyListModel(new ArrayList<String>()));
        skipTestsCheckBox.addChangeListener(new CheckBoxChangeListener());

        table1.setToolTipText(STAT_URL_MSG);
    }

    /**
     * Returns the rootpanel
     * @return the root panel
     */
    public JPanel getRootPanel() {
        return mainPanel;
    }

    /**
     * Populate the UI from the list.
     * @param dataProjComp the data provider
     */
    public void setDataOnUI(PMDProjectComponent dataProjComp) {
        ruleList.setModel(new MyListModel(dataProjComp.getCustomRuleSetPaths()));
        if (dataProjComp.getOptions().isEmpty()) {
            String[][] dat = new String[optionNames.length][2];
            for (int i = 0; i < optionNames.length; i++) {
                dat[i][0] = optionNames[i];
                dat[i][1] = defaultValues[i];
            }
            table1.setModel(new MyTableModel(dat, columnNames));
            return;
        }
        table1.setModel(new MyTableModel(toArray(dataProjComp.getOptions()), columnNames));
        skipTestsCheckBox.setSelected(dataProjComp.isSkipTestSources());
        isModified = false;
    }

    private Object[][] toArray(Map<String, String> options) {
        String[][] res = new String[optionNames.length][2];
        for (int i = 0; i < optionNames.length; i++) {
            res[i][0] = optionNames[i];
            res[i][1] = options.get(optionNames[i]);
        }
        return res;
    }

    /**
     * Get the data from ui and return.
     * @param data_ProjComp the data provider
     */
    public void getDataFromUi(PMDProjectComponent data_ProjComp) {
        data_ProjComp.setCustomRuleSets(((MyListModel) ruleList.getModel()).getData());
        data_ProjComp.setOptions( toMap(table1.getModel()) );
        data_ProjComp.skipTestSources(skipTestsCheckBox.isSelected());
        isModified = false;
    }

    private Map<String, String> toMap(TableModel tm) {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < tm.getRowCount(); i++) {
            m.put(optionNames[i], (String) tm.getValueAt(i,1));
        }
        return m;
    }

    /**
     * To detect if the ui is modified or not.
     * @param data the data provider
     * @return true if modified false otherwise
     */
    public boolean isModified(PMDProjectComponent data) {
        return isModified;
    }

    private void modifyRuleSet(final String defaultValue, AnActionEvent e) {
        DialogBuilder db = new DialogBuilder(PMDUtil.getProjectComponent(e).getCurrentProject());
        db.addOkAction();
        db.addCancelAction();
        db.setTitle("Select Custom RuleSet File or type URL");
        final BrowsePanel panel = new BrowsePanel(defaultValue, db, project);
        db.setOkActionEnabled(defaultValue != null && defaultValue.trim().length() > 0);
        db.show();
        //If ok is selected add the selected ruleset
        if (db.getDialogWrapper().getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            String fileName = panel.getText();
            String err;
            if ( (err = PMDResultCollector.isValidRuleSet(fileName)).length() > 0) {
                // make sense of error
                int lastPartToShow = err.indexOf("valid file or URL");
                int lastPos = (lastPartToShow > 0) ? lastPartToShow + 17 : Math.min(err.length(), 170);
                String errTxt = err.substring(0, lastPos); // prevent excessive useless length
                JOptionPane.showMessageDialog(panel, "The selected file/URL is not valid. PMD: " + errTxt,
                        "Invalid File/URL", JOptionPane.ERROR_MESSAGE);
                return;
            }
            MyListModel listModel = (MyListModel) ruleList.getModel();
            if (listModel.data.contains(fileName)) {
                return;
            }
            if (defaultValue != null && defaultValue.trim().length() > 0) {
                listModel.set(ruleList.getSelectedIndex(), fileName);
                return;
            }
            int index = listModel.getSize();
            listModel.add(index, fileName);
            ruleList.setSelectedIndex(index);
            ruleList.repaint();
        }
    }

    /**
     * Inner class for 'Add' action
     */
    private class AddRuleSetAction extends AnAction {
        public AddRuleSetAction(String text, String description, Icon icon) {
            super(text, description, icon);
            registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.ALT_DOWN_MASK)), rootPanel);
        }

        public void actionPerformed(@NotNull AnActionEvent e) {
            String defaultValue = "";
            modifyRuleSet(defaultValue, e);
        }
    }

    /**
     * Inner class for 'Edit' action
     */
    private class EditRuleSetAction extends AnAction {
        public EditRuleSetAction(String text, String description, Icon icon) {
            super(text, description, icon);
            registerCustomShortcutSet(CommonShortcuts.ALT_ENTER, rootPanel);
        }

        public void actionPerformed(@NotNull AnActionEvent e) {
            String defaultValue;
            defaultValue = (String) ruleList.getSelectedValue();
            modifyRuleSet(defaultValue, e);
        }

        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(!ruleList.getSelectionModel().isSelectionEmpty());
        }
    }

    /**
     * Inner class for 'Delete' action
     */
    private class DeleteRuleSetAction extends AnAction {
        public DeleteRuleSetAction(String text, String description, Icon icon) {
            super(text, description, icon);
            registerCustomShortcutSet(CommonShortcuts.getDelete(), rootPanel);
        }

        public void actionPerformed(@NotNull AnActionEvent e) {
            int index = ruleList.getSelectedIndex();
            if (index != -1) {
                ((MyListModel)ruleList.getModel()).remove(index);
                ruleList.setSelectedIndex(index);
            }
            ruleList.repaint();
        }

        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(ruleList.getSelectedIndex() != -1);
        }
    }

    private class MyTableModel extends DefaultTableModel {
        public MyTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        public boolean isCellEditable(int row, int column) {
            return column == 1;
        }

        public void setValueAt(Object aValue, int row, int column) {
            Object orig = getValueAt(row, column);
            super.setValueAt(aValue, row, column);
            boolean origIsMod = isModified;
            isModified = isModified || !orig.equals(aValue);
            // row 1: statistics URL
            if (row == 1) {
                validateStatUrl((String) aValue, row, column, orig, origIsMod);
            }
        }

        /**
         * Validate that statistics URL input is a valid URL and can be connected to.
         * If so, it is accepted.
         * If not, the change will be reverted and a tool tip message will show the reason.
         * Better solution might be to have a modal dialog to enter the URL,
         * however then the table setup should be quite changed.
         */
        private void validateStatUrl(String url, int row, int column, Object orig, boolean origIsMod) {
            String statUrlMsg;
            if (url.length() > 0) {
                if (!PMDUtil.isValidUrl(url)) {
                    statUrlMsg = "Previous input - Invalid URL: '" + url + "'";
                    super.setValueAt(orig, row, column);
                    isModified = origIsMod;
                }
                else {
                    String content = "{\"test connection\"}\n";
                    String exportMsg = PMDJsonExportingRenderer.tryJsonExport(content, url);
                    if (exportMsg.length() > 0) {
                        statUrlMsg = "Previous input - Failure for '" + url + "': " + exportMsg;
                        super.setValueAt(orig, row, column);
                        isModified = origIsMod;
                    }
                    else {
                        statUrlMsg = STAT_URL_MSG_SUCCESS;
                    }
                }
            }
            else {
                statUrlMsg = STAT_URL_MSG;
            }
            table1.setToolTipText(statUrlMsg);
        }
    }

    private class MyListModel extends AbstractListModel {

        private final List<String> data;

        public MyListModel(List<String> data) {
            this.data = data;
        }

        public synchronized int getSize() {
            return data.size();
        }

        public synchronized void add(int index, Object item) {
            data.add(index, (String)item);
            fireIntervalAdded(this, index, index);
            isModified = true;
        }

        public synchronized Object getElementAt(int index) {
            return data.get(index);
        }

        public synchronized List<String> getData() {
            return data;
        }

        public synchronized void remove(int index) {
            data.remove(index);
            fireIntervalRemoved(this, index, index);
            isModified = true;
        }

        public synchronized void set(int selIndex, String fileName) {
            data.set(selIndex, fileName);
            fireContentsChanged(this, selIndex, selIndex);
            isModified = true;
        }
    }

    /**
     * Helper class that shows the dialog for the user to browse and
     * select a ruleset file.
     */
    static class BrowsePanel extends JPanel {
        private final JTextField path;

        public BrowsePanel(String defaultValue, final DialogBuilder db, final Project project) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            JLabel label = new JLabel("Choose RuleSet: ");
            label.setMinimumSize(new Dimension(100, 20));
            label.setMaximumSize(new Dimension(120, 20));
            label.setPreferredSize(new Dimension(100, 20));
            add(label);
            path = new JTextField(defaultValue);
            label.setMinimumSize(new Dimension(200, 20));
            label.setMaximumSize(new Dimension(250, 20));
            path.setPreferredSize(new Dimension(200, 20));
            add(path);
            add(Box.createHorizontalStrut(5));
            JButton open = new JButton("Browse");
            label.setMinimumSize(new Dimension(50, 20));
            label.setMaximumSize(new Dimension(150, 20));
            open.setPreferredSize(new Dimension(80, 20));
            open.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final VirtualFile toSelect = project.getBaseDir();

                    final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
                    descriptor.withFileFilter(virtualFile -> virtualFile.getName().endsWith(".xml"));

                    final VirtualFile chosen = FileChooser.chooseFile(descriptor, BrowsePanel.this, project, toSelect);
                    if (chosen != null) {
                        final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
                        path.setText(newConfigFile.getAbsolutePath());
                    }
                }
            });
            add(open);
            add(Box.createVerticalGlue());
            db.setCenterPanel(this);

            path.getDocument().addDocumentListener(new DocumentAdapter() {
                protected void textChanged(@NotNull DocumentEvent e) {
                    try {
                        Document doc = e.getDocument();
                        db.setOkActionEnabled(doc.getText(0, doc.getLength()).trim().length() > 0);
                    } catch (BadLocationException e1) {
                    }
                }
            });
        }

        public String getText() {
            return path.getText();
        }
    }

    private class CheckBoxChangeListener implements ChangeListener
    {
        public void stateChanged(ChangeEvent e)
        {
            isModified = true;
        }
    }
}
