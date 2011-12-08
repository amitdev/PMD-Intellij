package com.intellij.plugins.bodhi.pmd.core;

import net.sourceforge.pmd.*;
import net.sourceforge.pmd.cpd.Renderer;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;

import com.intellij.plugins.bodhi.pmd.tree.PMDTreeNodeFactory;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleNode;

/**
 * Responsible for running PMD and collecting the results which can be represeted in
 * tree format.
 *
 * @author bodhi
 * @version 1.2
 */
public class PMDResultCollector {

    private Map<String, DefaultMutableTreeNode> map;
    public static Report report;

    //Whether we are using custom ruleset or not
    private boolean isCustomRuleSet;

    /**
     * Creates an instance of PMDResultCollector.
     *
     * @param isCustomRuleSet Whether a custom rule has to be run
     */
    public PMDResultCollector(boolean isCustomRuleSet) {
        map = new HashMap<String, DefaultMutableTreeNode>();
        this.isCustomRuleSet = isCustomRuleSet;
    }

    /**
     * Runs the given rule on given set of files and returns the result.
     *
     * @param files The files to run PMD on
     * @param rule The rule to run
     * @return list of results
     */
    public List<DefaultMutableTreeNode> getResults(List<File> files, String rule, Map<String, String> options) {
        List<DataSource> fileDataSources = new ArrayList<DataSource>(files.size());
        for (File file : files) {
            fileDataSources.add(new FileDataSource(file));
        }
        return parse(generateReport(fileDataSources, rule, options));
    }

    /**
     * Parse the pmd report and return the results as a list of tree nodes.
     * The user object in tree node will be a PMDViolation object.
     *
     * @param report The PMD report
     * @return List of tree nodes
     */
    private List<DefaultMutableTreeNode> parse(Report report) {
        PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
        if (PMDResultCollector.report == null) {
            PMDResultCollector.report = new Report();
        }        
        for (Iterator iterator = report.iterator(); iterator.hasNext();) {
            IRuleViolation iRuleViolation = (IRuleViolation) iterator.next();
            PMDResultCollector.report.addRuleViolation(iRuleViolation);
            String message = iRuleViolation.getRule().getDescription();
            if (message.length() > 80) {
                message = message.substring(0, 80) + "...";
            }
            DefaultMutableTreeNode node = map.get(message);
            if (node == null) {
                node = nodeFactory.createNode(message);
                ((PMDRuleNode)node.getUserObject()).setToolTip(iRuleViolation.getRule().getDescription());
                map.put(message, node);
            }
            node.add(nodeFactory.createNode(new PMDViolation(iRuleViolation)));
            //Add one violation
            ((PMDRuleNode)node.getUserObject()).addChildren(1);
        }
        List<DefaultMutableTreeNode> pmdResults = new ArrayList<DefaultMutableTreeNode>(report.size());
        for (Iterator<DefaultMutableTreeNode> iterator = map.values().iterator(); iterator.hasNext();) {
            DefaultMutableTreeNode node = iterator.next();
            if (node.getChildCount() > 0) {
                pmdResults.add(node);
            }
        }
        return pmdResults;
    }

    /**
     * Runs PMD on given set of files and generates the Report.
     *
     * @param files The list of files to run PMD
     * @param rule The rule(s) to run
     * @return The pmd Report
     */
    private Report generateReport(List<DataSource> files, String rule, Map<String, String> options) {
        RuleContext context = new RuleContext();
        Report report = new Report();
        context.setReport(report);
        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        PMD pmd = new PMD();
        Map<String, SourceType> types = new HashMap<String, SourceType>();
        types.put("1.3", SourceType.JAVA_13);
        types.put("1.4", SourceType.JAVA_14);
        types.put("1.5", SourceType.JAVA_15);
        types.put("1.6", SourceType.JAVA_16);
        String type;
        if ( (type = options.get("Target JDK")) != null) {
            SourceType srcType;
            if ( (srcType = types.get(type)) != null) {
                pmd.setJavaVersion(srcType);
            }
        }
        try {
            RuleSets ruleSets;
            if (isCustomRuleSet) {
                ruleSets = ruleSetFactory.createRuleSets(rule);
            } else {
                ruleSets = ruleSetFactory.createRuleSets(new SimpleRuleSetNameMapper(rule).getRuleSets());
            }
            report.start();
            pmd.processFiles(files, context, ruleSets,
                    false, false, "", new InputStreamReader(System.in).getEncoding());
        } catch (RuleSetNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        report.end();        
        return context.getReport();
    }

    /**
     * Verifies whether the ruleset specified at the path is a valid PMD ruleset.
     *
     * @param path path of the rule set
     * @return true if valid ruleset, false otherwise.
     */
    public static String isValidRuleSet(String path) {
        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        try {
            RuleSet rs = ruleSetFactory.createSingleRuleSet(path);
            if (rs.getRules().size() != 0) {
                return "";
            }
        } catch (RuleSetNotFoundException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            return e.getMessage();
        }
        return "Invalid File";
    }
}