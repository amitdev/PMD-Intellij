package com.intellij.plugins.bodhi.pmd.core;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.io.IOException;
import java.io.File;

import com.intellij.plugins.bodhi.pmd.tree.PMDTreeNodeFactory;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleNode;
import net.sourceforge.pmd.*;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.util.IOUtil;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;

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
        return generateReport(fileDataSources, rule, options);
    }

    /**
     * Runs PMD on given set of files and generates the Report.
     *
     * @param files The list of files to run PMD
     * @param rule The rule(s) to run
     * @return The pmd Report
     */
    private List<DefaultMutableTreeNode> generateReport(List<DataSource> files, String rule, Map<String, String> options) {
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        PMDConfiguration pmdConfig = new PMDConfiguration();
        Language lang = LanguageRegistry.getLanguage("Java");
        String type;
        if ( (type = options.get("Target JDK")) != null) {
            LanguageVersion srcType;
            if ( (srcType = lang.getVersion(type)) != null) {
                //pmdConfig.setDefaultLanguageVersion(srcType);
                pmdConfig.getLanguageVersionDiscoverer().setDefaultLanguageVersion(srcType);
            }
        }
        final List<DefaultMutableTreeNode> pmdResults = new ArrayList<DefaultMutableTreeNode>();
        try {
            RuleSetFactory ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(pmdConfig);
            if (!isCustomRuleSet)
                rule = rule.replace("/", "-");
            pmdConfig.setRuleSets(rule);
            pmdConfig.setReportFile(File.createTempFile("pmd", "report").getAbsolutePath());
            //RulesetsFactoryUtils.getRuleSets(rule, ruleSetFactory, System.nanoTime());
            Renderer renderer = new AbstractIncrementingRenderer("pmdplugin", "PMD plugin renderer") {
                @Override
                public void renderFileViolations(Iterator<RuleViolation> violations) throws IOException {
                    PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
                    if (PMDResultCollector.report == null) {
                        PMDResultCollector.report = new Report();
                    }
                    for (; violations.hasNext();) {
                        RuleViolation iRuleViolation = violations.next();
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
                    for (DefaultMutableTreeNode node : map.values()) {
                        if (node.getChildCount() > 0 && !pmdResults.contains(node)) {
                            pmdResults.add(node);
                        }
                    }
                }

                public String defaultFileExtension() {
                    return "txt";
                }
            };

            List<Renderer> renderers = new LinkedList<Renderer>();
            renderers.add(renderer);

            renderer.setWriter(IOUtil.createWriter(pmdConfig.getReportFile()));
            renderer.start();

            RuleContext ctx = new RuleContext();

            pmdConfig.setThreads(0);
            PMD.processFiles(pmdConfig, ruleSetFactory, files, ctx, renderers);

            renderer.end();
            renderer.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return pmdResults;
    }

    /**
     * Verifies whether the ruleset specified at the path is a valid PMD ruleset.
     *
     * @param path path of the rule set
     * @return true if valid ruleset, false otherwise.
     */
    public static String isValidRuleSet(String path) {
        Thread.currentThread().setContextClassLoader(PMDResultCollector.class.getClassLoader());
        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        try {
            RuleSet rs = ruleSetFactory.createRuleSet(path);
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
