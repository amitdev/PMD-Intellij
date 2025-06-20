package com.intellij.plugins.bodhi.pmd;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.bodhi.pmd.actions.AnEDTAction;
import com.intellij.plugins.bodhi.pmd.core.PMDJsonExportingRenderer;
import com.intellij.plugins.bodhi.pmd.core.PMDResultCollector;
import com.intellij.util.PlatformIcons;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;
import java.util.List;

import static com.intellij.plugins.bodhi.pmd.actions.PreDefinedJavaMenuGroup.RULESETS_JAVA_PROPERTY_FILE;
import static com.intellij.plugins.bodhi.pmd.actions.PreDefinedKotlinMenuGroup.RULESETS_KOTLIN_PROPERTY_FILE;


/**
 * This class represents the UI for settings.
 *
 * @author bodhi
 * @version 1.1
 */
public class PMDConfigurationForm {
    private static final Logger LOG = Logger.getInstance(PMDConfigurationForm.class); // IntelliJ supported logger

    private JPanel rootPanel;
    private JList<String> ruleSetPathJList;
    private JPanel buttonPanel;
    private JTabbedPane tabbedPane1;
    private JTable optionsTable;
    private JPanel mainPanel;
    private JCheckBox skipTestsCheckBox;
    private JList<String> inEditorAnnotationRuleSets;
    private final List<String> deletedRuleSetPaths = new ArrayList<>();
    private boolean isModified;
    private final Project project;
    private volatile Map<String, String> validKnownCustomRules;

    private static final List<String> columnNames = List.of("Option", "Value");
    private static final String STAT_URL_MSG_SUCCESS = "Connection success; will use Statistics URL to export anonymous usage statistics";

    public PMDConfigurationForm(final Project project) {
        this.project = project;

        optionsTable.putClientProperty("terminateEditOnFocusLost", true); // fixes issue #45
        optionsTable.setRowHeight(optionsTable.getRowHeight() + 5); // increase space around text
        ruleSetPathJList.setModel(new RuleSetListModel(new ArrayList<>()));
        inEditorAnnotationRuleSets.setModel(new RuleSetListModel(new ArrayList<>()));
        inEditorAnnotationRuleSets.getSelectionModel().addListSelectionListener(new SelectionChangeListener());
        skipTestsCheckBox.addChangeListener(new CheckBoxChangeListener());

        // Timed retry-based initialization, to make sure intelliJ services are initialized before
        scheduleActionManagerInit(0);

        validKnownCustomRules = Collections.emptyMap(); // Lazy load later
    }

    private void scheduleActionManagerInit(int attemptCount) {
        if (attemptCount == 0) {
            // First attempt immediately (later)
            tryInitActionManager(attemptCount);
        } else {
            // Subsequent attempts with delay: 100ms, 400ms, 900ms
            int delay = 100 * attemptCount * attemptCount;
            javax.swing.Timer timer = new javax.swing.Timer(delay, e -> tryInitActionManager(attemptCount));
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void tryInitActionManager(int attemptCount) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                initializeActionManager();
                validKnownCustomRules = PMDUtil.getValidKnownCustomRules();
            } catch (Exception e) {
                if (attemptCount < 3) {
                    scheduleActionManagerInit(attemptCount + 1);
                } else {
                    LOG.warn("ActionManager initialization failed after retries", e);
                }

            }
        });
    }

    private void initializeActionManager() {
        //Get the action group defined
        DefaultActionGroup actionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("PMDSettingsEdit");
        //Remove toolbar actions associated to previous form
        actionGroup.removeAll();
        //Add the toolbar actions associated to this form to it
        actionGroup.add(new AddRuleSetAction("Add", "Add a custom ruleset", PlatformIcons.ADD_ICON));
        actionGroup.add(new EditRuleSetAction("Edit", "Edit selected ruleset", PlatformIcons.EDIT));
        actionGroup.add(new DeleteRuleSetAction("Delete", "Remove selected ruleset", PlatformIcons.DELETE_ICON));
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("modify actions", actionGroup, true);
        toolbar.setTargetComponent(toolbar.getComponent()); // prevent warning
        toolbar.getComponent().setVisible(true);
        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(toolbar.getComponent(), BorderLayout.CENTER);
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
        List<String> customRuleSetPaths = dataProjComp.getCustomRuleSetPaths();
        ruleSetPathJList.setModel(new RuleSetListModel(customRuleSetPaths));
        if (dataProjComp.getOptionToValue().isEmpty()) {
            final int numOptions = ConfigOption.size();
            String[][] optionDescsDefaultValues = new String[numOptions][2];
            for (int i = 0; i < numOptions; i++) {
                ConfigOption option = ConfigOption.values()[i];
                optionDescsDefaultValues[i][0] = option.getDescription();
                optionDescsDefaultValues[i][1] = option.getDefaultValue();
            }
            optionsTable.setModel(new MyTableModel(optionDescsDefaultValues, columnNames.toArray()));
        }
        else {
            optionsTable.setModel(new MyTableModel(toDescValueArray2d(dataProjComp.getOptionToValue()), columnNames.toArray()));
        }
        skipTestsCheckBox.setSelected(dataProjComp.isSkipTestSources());

        List<String> javaRules = PMDUtil.loadRules(RULESETS_JAVA_PROPERTY_FILE);
        List<String> kotlinRules = PMDUtil.loadRules(RULESETS_KOTLIN_PROPERTY_FILE);

        List<String> allRules = new ArrayList<>(javaRules);
        allRules.addAll(kotlinRules);
        allRules.addAll(customRuleSetPaths);

        RuleSetListModel inEditorAnnotationModel = new RuleSetListModel(allRules);
        inEditorAnnotationRuleSets.setModel(inEditorAnnotationModel);
        inEditorAnnotationRuleSets.setSelectedIndices(inEditorAnnotationModel.getIndexes(dataProjComp.getInEditorAnnotationRuleSets()));

        isModified = false;
    }

    private Object[][] toDescValueArray2d(Map<ConfigOption, String> optionToValue) {
        String[][] result = new String[ConfigOption.size()][2];
        for (int i = 0; i < ConfigOption.size(); i++) {
            ConfigOption option = ConfigOption.values()[i];
            result[i][0] = option.getDescription();
            String value = optionToValue.get(option);
            result[i][1] = (value != null) ? value : option.getDefaultValue();
        }
        return result;
    }

    /**
     * Get the data from ui and return.
     * @param dataProjComp the data provider
     */
    public void getDataFromUi(PMDProjectComponent dataProjComp) {
        dataProjComp.setCustomRuleSetPaths(((RuleSetListModel) ruleSetPathJList.getModel()).getList());
        dataProjComp.setDeletedRuleSetPaths(deletedRuleSetPaths);
        dataProjComp.setOptionToValue(toOptionToValue(optionsTable.getModel()));
        dataProjComp.skipTestSources(skipTestsCheckBox.isSelected());
        dataProjComp.setInEditorAnnotationRuleSets(inEditorAnnotationRuleSets.getSelectedValuesList());

        isModified = false;
    }

    private Map<ConfigOption, String> toOptionToValue(TableModel tm) {
        Map<ConfigOption, String> optionToValue = new EnumMap<>(ConfigOption.class);
        for (int i = 0; i < tm.getRowCount(); i++) {
            ConfigOption option = ConfigOption.fromDescription((String)tm.getValueAt(i, 0));
            optionToValue.put(option, (String) tm.getValueAt(i,1));
        }
        return optionToValue;
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
        db.setTitle("Choose Custom RuleSet from Drop-down, File or Paste URL");
        final BrowsePanel panel = new BrowsePanel(defaultValue, db, project);
        db.show();
        //If ok is selected add the selected ruleset
        if (db.getDialogWrapper().getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            String rulesRef = panel.getText().trim();
            if (!rulesRef.startsWith("Warn")) { // warnings are just to notify the user, ignore as rules reference
                RuleSetListModel listModel = (RuleSetListModel) ruleSetPathJList.getModel();
                // if ruleSet referenced by name, change to the URL
                String rulesPath = rulesRef;
                if (validKnownCustomRules.containsKey(rulesRef)) {
                    rulesPath = validKnownCustomRules.get(rulesRef);
                    ruleSetPathJList.setSelectedIndex(listModel.getSize());
                }
                String err;
                if (!(err = PMDResultCollector.isValidRuleSet(rulesPath)).isEmpty()) {
                    String message = "The selected file/URL is not valid for PMD 7.";
                    if (err.contains("XML validation errors occurred")) {
                        message += " XML validation errors occurred.";
                    }
                    // make sense of error
                    int lastPartToShow = err.indexOf("valid file or URL");
                    int lastPos = (lastPartToShow > 0) ? lastPartToShow + 17 : Math.min(err.length(), 170);
                    String errTxt = err.substring(0, lastPos); // prevent excessive useless length
                    JOptionPane.showMessageDialog(panel, message + " PMD: " + errTxt,
                            "Invalid File/URL", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int selectedIndex = ruleSetPathJList.getSelectedIndex();
                if (listModel.list.contains(rulesPath.trim())) {
                    selectedIndex = listModel.list.indexOf(rulesPath.trim());
                    ruleSetPathJList.setSelectedIndex(selectedIndex);
                    listModel.set(selectedIndex, rulesPath.trim()); // trigger menu update
                    return;
                }
                if (defaultValue != null && !defaultValue.trim().isEmpty() && selectedIndex >= 0) {
                    listModel.set(selectedIndex, rulesPath);
                    return;
                }

                int index = listModel.getSize();
                listModel.add(index, rulesPath);
                ruleSetPathJList.setSelectedIndex(index);
                deletedRuleSetPaths.remove(rulesPath);

                RuleSetListModel inEditorAnnotationRuleSetsModel = (RuleSetListModel) inEditorAnnotationRuleSets.getModel();
                inEditorAnnotationRuleSetsModel.add(inEditorAnnotationRuleSetsModel.getSize(), rulesPath);
            }
            ruleSetPathJList.repaint();
        }
    }

    /**
     * Inner class for 'Add' action
     */
    private class AddRuleSetAction extends AnEDTAction {
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
    private class EditRuleSetAction extends AnEDTAction {
        public EditRuleSetAction(String text, String description, Icon icon) {
            super(text, description, icon);
            registerCustomShortcutSet(CommonShortcuts.ALT_ENTER, rootPanel);
        }

        public void actionPerformed(@NotNull AnActionEvent e) {
            String defaultValue = ruleSetPathJList.getSelectedValue();
            modifyRuleSet(defaultValue, e);
        }

        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(!ruleSetPathJList.getSelectionModel().isSelectionEmpty());
        }

    }

    /**
     * Inner class for 'Delete' action
     */
    private class DeleteRuleSetAction extends AnEDTAction {
        public DeleteRuleSetAction(String text, String description, Icon icon) {
            super(text, description, icon);
            registerCustomShortcutSet(CommonShortcuts.getDelete(), rootPanel);
        }

        public void actionPerformed(@NotNull AnActionEvent e) {
            int index = ruleSetPathJList.getSelectedIndex();
            if (index != -1) {
                String toRemove = ruleSetPathJList.getModel().getElementAt(index);
                ((RuleSetListModel) ruleSetPathJList.getModel()).remove(index);
                ruleSetPathJList.setSelectedIndex(index);
                deletedRuleSetPaths.add(toRemove);

                ((RuleSetListModel) inEditorAnnotationRuleSets.getModel()).remove(toRemove);
            }
            ruleSetPathJList.repaint();
        }

        public void update(@NotNull AnActionEvent e) {
            super.update(e);
            e.getPresentation().setEnabled(ruleSetPathJList.getSelectedIndex() != -1);
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
            isModified = isModified || orig == null || !orig.equals(aValue);
            switch (row) {
                // row 0: Target JDK
                case 0: validateLanguageVersion((String) aValue, row, column, orig, origIsMod, "java");
                break;
                // row 1: Target Kotlin Version
                case 1: validateLanguageVersion((String) aValue, row, column, orig, origIsMod, "kotlin");
                break;
                // row 2: statistics URL
                case 2: validateStatUrl((String) aValue, row, column, orig, origIsMod);
                break;
                // row 3: threads
                case 3: validateThreads((String) aValue, row, column, orig, origIsMod);
                break;
            }
        }

        private void validateLanguageVersion(String versionInput, int row, int column, Object orig, boolean origIsMod, String langId) {
            if (versionInput.equals(orig)) {
                return;
            }
            Language language = Objects.requireNonNull(LanguageRegistry.PMD.getLanguageById(langId));
            boolean isRegistered = language.hasVersion(versionInput);
            if (isRegistered) {
                String registeredVersion = Objects.requireNonNull(language.getVersion(versionInput)).getVersion();
                optionsTable.setToolTipText(langId + " version " + registeredVersion);
            }
            else {
                super.setValueAt(orig, row, column);
                List<LanguageVersion> langVersions = language.getVersions();
                List<String> versions = new ArrayList<>();
                for (LanguageVersion langVersion : langVersions) {
                    versions.add(langVersion.getVersion());
                }
                String maxTenMostRecentVersions = String.join(",", versions.subList(Math.max(versions.size() - 10, 0), versions.size()));
                String tipText = "For " + langId + " version take one of: " + maxTenMostRecentVersions;
                optionsTable.setToolTipText(tipText);
                isModified = origIsMod;
            }
        }

        /**
         * Validate that statistics URL input is a valid URL and can be connected to.
         * If so, it is accepted.
         * If not, the change will be reverted and a tool tip message will show the reason.
         * Better solution might be to have a modal dialog to enter the URL,
         * however then the table setup should be quite changed.
         */
        private void validateStatUrl(String urlInput, int row, int column, Object orig, boolean origIsMod) {
            if (urlInput.equals(orig)) {
                return;
            }
            if (!urlInput.isEmpty()) {
                if (PMDUtil.isValidUrl(urlInput)) {
                    String content = "{\"test connection\"}\n";
                    String exportMsg = PMDJsonExportingRenderer.tryJsonExport(content, urlInput);
                    if (!exportMsg.isEmpty()) {
                        optionsTable.setToolTipText("Previous input - Failure for '" + urlInput + "': " + exportMsg);
                        super.setValueAt(orig, row, column);
                        isModified = origIsMod;
                    } else {
                        isModified = true;
                        optionsTable.setToolTipText(STAT_URL_MSG_SUCCESS);
                    }
                } else {
                    optionsTable.setToolTipText("Previous input - Invalid URL: '" + urlInput + "'");
                    super.setValueAt(orig, row, column);
                    isModified = origIsMod;
                }
            }
        }

        private void validateThreads(String threadsInput, int row, int column, Object orig, boolean origIsMod) {
            if (threadsInput.equals(orig)) {
                return;
            }
            boolean ok = true;
            try {
                int asInt = Integer.parseInt(threadsInput);
                if (asInt < 1 || asInt > PMDUtil.AVAILABLE_PROCESSORS) {
                    ok = false;
                }
            } catch (NumberFormatException ne) {
                ok = false;
            }
            if (ok) {
                optionsTable.setToolTipText(threadsInput + " threads");
            }
            else {
                super.setValueAt(orig, row, column);
                optionsTable.setToolTipText("Must be an positive integer less than or equal to " + PMDUtil.AVAILABLE_PROCESSORS);
                isModified = origIsMod;
            }
        }
    }

    private class RuleSetListModel extends AbstractListModel<String> {

        private final List<String> list;

        public RuleSetListModel(List<String> list) {
            // make sure the rules are trimmed and unique
            Set<String> set = new LinkedHashSet<>();
            for (String s : list) {
                set.add(s.trim());
            }
            this.list = new ArrayList<>(set);
        }

        public synchronized int getSize() {
            return list.size();
        }

        public synchronized void add(int index, Object item) {
            String trimmed = ((String) item).trim();
            if (!list.contains(trimmed)) {
                list.add(index, trimmed);
                fireIntervalAdded(this, index, index);
                isModified = true;
            }
        }

        public synchronized String getElementAt(int index) {
            return list.get(index);
        }

        public synchronized List<String> getList() {
            return list;
        }

        public synchronized void remove(String objectToRemove) {
            remove(list.indexOf(objectToRemove));
        }

        public synchronized void remove(int index) {
            list.remove(index);
            fireIntervalRemoved(this, index, index);
            isModified = true;
        }

        public synchronized void set(int selIndex, String fileName) {
            list.set(selIndex, fileName.trim());
            fireContentsChanged(this, selIndex, selIndex);
            isModified = true;
        }

        public synchronized int[] getIndexes(Set<String> selectedObjects) {
            int[] selected = new int[selectedObjects.size()];
            List<String> options = getList();
            int i = 0;
            for (String selectedOption : selectedObjects) {
                selected[i++] = options.indexOf(selectedOption);
            }
            return selected;
        }
    }

    /**
     * Helper class that shows the dialog for the user to browse and
     * select a ruleset file.
     */
    static class BrowsePanel extends JPanel {
        private final JComboBox<String> pathComboBox;

        public BrowsePanel(String defaultValue, final DialogBuilder db, final Project project) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            JLabel label = new JLabel("Choose RuleSet: ");
            label.setMinimumSize(new Dimension(120, 20));
            label.setMaximumSize(new Dimension(150, 20));
            label.setPreferredSize(new Dimension(130, 20));
            add(label);
            final Vector<String> elements = new Vector<>();
            elements.add(defaultValue);
            Set<String> ruleSetNames = PMDUtil.getValidKnownCustomRules().keySet();
            elements.addAll(ruleSetNames);

            ComboBoxModel<String> model = new DefaultComboBoxModel<>(elements);
            model.setSelectedItem(defaultValue);
            pathComboBox = new ComboBox<>(model);
            pathComboBox.setEditable(true);
            pathComboBox.setMinimumSize(new Dimension(200, 26));
            pathComboBox.setMaximumSize(new Dimension(800, 28));
            pathComboBox.setPreferredSize(new Dimension(230, 28));
            add(pathComboBox);
            add(Box.createHorizontalStrut(5));
            JButton open = getJButton(project);
            add(open);
            add(Box.createVerticalGlue());
            db.setCenterPanel(this);
        }

        private @NotNull JButton getJButton(Project project) {
            JButton open = new JButton("Browse");
            open.setPreferredSize(new Dimension(80, 20));
            open.addActionListener(e -> {
                final VirtualFile toSelect = ProjectUtil.guessProjectDir(project);
                // file system access takes some time, IntelliJ sometimes gives an exception that
                // and EDT thread should not take long. Should be solved by using a BGT thread, but how?
                final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false);
                descriptor.withFileFilter(virtualFile -> virtualFile.getName().endsWith(".xml"));

                final VirtualFile chosen = FileChooser.chooseFile(descriptor, this, project, toSelect);
                if (chosen != null) {
                    final File newConfigFile = VfsUtilCore.virtualToIoFile(chosen);
                    String ioFile = newConfigFile.getAbsolutePath();
                    final Vector<String> elem = new Vector<>();
                    elem.add(ioFile);
                    ComboBoxModel<String> newModel = new DefaultComboBoxModel<>(elem);
                    pathComboBox.setModel(newModel);
                    pathComboBox.setSelectedItem(ioFile);
                    pathComboBox.setEditable(false);
                }
            });
            return open;
        }

        public String getText() {
            return (String) pathComboBox.getSelectedItem();
        }
    }

    private class CheckBoxChangeListener implements ChangeListener
    {
        public void stateChanged(ChangeEvent e)
        {
            isModified = true;
        }
    }

    private class SelectionChangeListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            isModified = true;
        }
    }
}
