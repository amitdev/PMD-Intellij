package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.actionSystem.*;
import com.intellij.util.PlatformIcons;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.ui.DocumentAdapter;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.util.*;
import java.util.List;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.*;
import java.io.File;

/**
 * This class represents the UI for settings.
 *
 * @author bodhi
 * @version 1.0
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

    private static final Object[] columnNames = new String[] {"Option", "Value"};
    private static final String[] optionNames = new String[] {"Target JDK", "Encoding"};
    private static final String[] defaultValues = new String[] {"1.8", ""};

    public PMDConfigurationForm() {
        //Get the action group defined
        DefaultActionGroup actionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("PMDSettingsEdit");
        //Add the toolbar actions to it
        if (actionGroup.getChildrenCount() == 0) {
            actionGroup.add(new AddRuleSetAction("Add", "Add a custom ruleset", PlatformIcons.ADD_ICON));
            actionGroup.add(new EditRuleSetAction("Edit", "Edit selected ruleset", IconLoader.getIcon("/actions/editSource.png")));
            actionGroup.add(new DeleteRuleSetAction("Delete", "Remove selected ruleset", IconLoader.getIcon("/general/remove.png")));
        }
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("test", actionGroup, true);
        toolbar.getComponent().setVisible(true);
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(toolbar.getComponent(), BorderLayout.CENTER);

        table1.putClientProperty("terminateEditOnFocusLost", true); // fixes issue #45
        ruleList.setModel(new MyListModel(new ArrayList<String>()));
        skipTestsCheckBox.addChangeListener(new CheckBoxChangeListener());
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
     * @param data the data provider
     */
    public void setData(PMDProjectComponent data) {
        ruleList.setModel(new MyListModel(data.getCustomRuleSets()));
        if (data.getOptions().isEmpty()) {
            Object[][] dat = new Object[optionNames.length][2];
            for (int i = 0; i < optionNames.length; i++) {
                dat[i][0] = optionNames[i];
                dat[i][1] = defaultValues[i];
            }
            table1.setModel(new MyTableModel(dat, columnNames));
            return;
        }
        table1.setModel(new MyTableModel(toArray(data.getOptions()), columnNames));
        skipTestsCheckBox.setSelected(data.isSkipTestSources());
        isModified = false;
    }

    private Object[][] toArray(Map<String, String> options) {
        Object[][] res = new Object[options.size()][2];
        int i = 0;
        for (Iterator<String> iterator = options.values().iterator(); iterator.hasNext();) {
            res[i][0] = optionNames[i];
            res[i++][1] = iterator.next();
        }
        return res;
    }

    /**
     * Get the data from ui and return.
     * @param data the data provider
     */
    public void getData(PMDProjectComponent data) {
        data.setCustomRuleSets(((MyListModel) ruleList.getModel()).getData());
        data.setOptions( toMap(table1.getModel()) );
        data.skipTestSources(skipTestsCheckBox.isSelected());
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
        db.setTitle("Select Custom RuleSet");
        final BrowsePanel panel = new BrowsePanel(defaultValue, db);
        db.setOkActionEnabled(defaultValue != null && defaultValue.trim().length() > 0);
        db.show();
        //If ok is selected add the selected ruleset
        if (db.getDialogWrapper().getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            String fileName = panel.getText();
            String err;
            if ( (err = PMDResultCollector.isValidRuleSet(fileName)).length() > 0) {
                JOptionPane.showMessageDialog(panel, "The selected file is not a valid PMD ruleset : "+err,
                        "Invalid File",JOptionPane.ERROR_MESSAGE);
                return;
            }
            MyListModel listModel = (MyListModel) ruleList.getModel();
            if (listModel.data.contains(fileName)) {
                return;
            }
            if (defaultValue.length() > 0) {
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

        public void actionPerformed(AnActionEvent e) {
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

        public void actionPerformed(AnActionEvent e) {
            String defaultValue;
            defaultValue = (String) ruleList.getSelectedValue();
            modifyRuleSet(defaultValue, e);
        }

        public void update(AnActionEvent e) {
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

        public void actionPerformed(AnActionEvent e) {
            int index = ruleList.getSelectedIndex();
            if (index != -1) {
                ((MyListModel)ruleList.getModel()).remove(index);
                ruleList.setSelectedIndex(index);
            }
            ruleList.repaint();
        }

        public void update(AnActionEvent e) {
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
            isModified = isModified || !orig.equals(aValue);
        }
    }
    private class MyListModel extends AbstractListModel {

        private List<String> data;

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
        private JLabel label;
        private JTextField path;
        private JButton open;

        public BrowsePanel(String defaultValue, final DialogBuilder db) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            label = new JLabel("Choose RuleSet: ");
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
            open = new JButton("Browse");
            label.setMinimumSize(new Dimension(50, 20));
            label.setMaximumSize(new Dimension(150, 20));
            open.setPreferredSize(new Dimension(80, 20));
            open.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(PMDUtil.createFileExtensionFilter("xml", "XML Files"));
                    final Component parent = SwingUtilities.getRoot(path);
                    fc.showDialog(parent, "Open");
                    File selected = fc.getSelectedFile();
                    if (selected != null) {
                        String newLocation = selected.getPath();
                        path.setText(newLocation);
                    }
                }
            });
            add(open);
            add(Box.createVerticalGlue());
            db.setCenterPanel(this);

            path.getDocument().addDocumentListener(new DocumentAdapter() {
                protected void textChanged(DocumentEvent e) {
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
