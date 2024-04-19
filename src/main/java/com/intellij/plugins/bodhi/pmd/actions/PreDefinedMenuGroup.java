package com.intellij.plugins.bodhi.pmd.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.plugins.bodhi.pmd.PMDInvoker;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.PMDUtil;
import net.sourceforge.pmd.util.ResourceLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Properties;

/**
 * This ActionGroup defines the actions for pre defined rulesets that
 * comes with PMD. The actions are created dynamically.
 *
 * @author bodhi
 * @version 1.0
 */
public class PreDefinedMenuGroup extends ActionGroup {

    // A string that represents all the rulesets as comma separated value.
    private static String allRules = "";

    //All the children of this group
    private DefaultActionGroup children = new DefaultActionGroup("Pre Defined", true);

    private PMDProjectComponent component;

    //The ruleset property file which lists all the predefined rulesets
    public static final String RULESETS_PROPERTY_FILE = "rulesets/java/rulesets.properties";
    public static final String RULESETS_FILENAMES_KEY = "rulesets.filenames";

    /**
     * Loads all the predefined rulesets in PMD and create actions for them.
     */
    public PreDefinedMenuGroup() {
        AnAction action = new AnAction("All") {
            public void actionPerformed(AnActionEvent e) {
                PMDInvoker.getInstance().runPMD(e, allRules, false);
                getComponent().setLastRunActionAndRules(e, allRules, false);
            }
        };
        Properties props = new Properties();
        try {
            //Load the property file which has all the rulesets.
            props.load(new ResourceLoader(getClass().getClassLoader()).loadResourceAsStream(RULESETS_PROPERTY_FILE));
            String[] rulesetFilenames = props.getProperty(RULESETS_FILENAMES_KEY).split(PMDInvoker.RULE_DELIMITER);

            //We have 'All' rules in addition to the rulesets
            //First one is 'All'
            children.removeAll();
            children.add(action);

            for (int i=0; i < rulesetFilenames.length; ++i) {
                final String ruleFileName = rulesetFilenames[i];
                final String ruleName = PMDUtil.getBareFileNameFromPath(ruleFileName);
                allRules += ruleFileName;
                allRules += (i == rulesetFilenames.length - 1) ? "" : PMDInvoker.RULE_DELIMITER;
                AnAction ruleAction = new AnAction(ruleName) {
                    public void actionPerformed(AnActionEvent e) {
                        PMDInvoker.getInstance().runPMD(e, ruleFileName, false);
                        getComponent().setLastRunActionAndRules(e, ruleFileName, false);
                    }
                };
                children.add(ruleAction);
            }
        } catch (IOException e) {
            //Should not happen
            //e.printStackTrace();
        } catch (Exception e) {
            //Should not happen
            //e.printStackTrace();
        }
    }

    public AnAction[] getChildren(@Nullable AnActionEvent event) {
        return new AnAction[] { this.children };
    }

    public void setComponent(PMDProjectComponent component) {
        this.component = component;
    }

    public PMDProjectComponent getComponent() {
        return component;
    }
}
