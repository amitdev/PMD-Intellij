package com.intellij.plugins.bodhi.pmd.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.plugins.bodhi.pmd.PMDUtil;
import com.intellij.plugins.bodhi.pmd.PMDInvoker;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Properties;
import java.io.IOException;

import net.sourceforge.pmd.util.ResourceLoader;
import net.sourceforge.pmd.RuleSetNotFoundException;

import javax.swing.*;

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
    private AnAction[] children;

    private PMDProjectComponent component;

    //The ruleset property file which lists all the predefined rulesets
    private static final String RULESETS_PROPERTY_FILE = "rulesets/java/rulesets.properties";
    private static final String RULESETS_FILENAMES = "rulesets.filenames";

    /**
     * Loads all the predefined rulesets in PMD and create actions for them.
     */
    public PreDefinedMenuGroup() {
        AnAction action = new AnAction("All") {
            public void actionPerformed(AnActionEvent e) {
                PMDInvoker.getInstance().runPMD(e, allRules, false);
                getComponent().setLastRunRules(allRules);
            }
        };
        Properties props = new Properties();
        try {
            //Load the property file which has all the rulesets.
            props.load(ResourceLoader.loadResourceAsStream(RULESETS_PROPERTY_FILE));
            String[] rulesetFilenames = props.getProperty(RULESETS_FILENAMES).split(PMDInvoker.RULE_DELIMITER);

            //We have 'All' rules in addition to the rulesets
            children = new AnAction[rulesetFilenames.length+1];
            //First one is 'All'
            children[0] = action;

            for (int i=0; i < rulesetFilenames.length; ++i) {
                int start = rulesetFilenames[i].indexOf('/');
                int end = rulesetFilenames[i].indexOf('.');
                final String name = rulesetFilenames[i].substring(start+1, end);
                allRules += name;
                allRules += (i == rulesetFilenames.length - 1) ? "" : PMDInvoker.RULE_DELIMITER;
                AnAction ruleAction = new AnAction(name) {
                    public void actionPerformed(AnActionEvent e) {
                        PMDInvoker.getInstance().runPMD(e, name, false);
                        getComponent().setLastRunRules(name);
                    }
                };
                children[i+1] = ruleAction;
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
        return this.children;
    }

    public void setComponent(PMDProjectComponent component) {
        this.component = component;
    }

    public PMDProjectComponent getComponent() {
        return component;
    }
}
