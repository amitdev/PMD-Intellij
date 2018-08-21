package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.plugins.bodhi.pmd.tree.PMDErrorNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDTreeNodeFactory;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import net.sourceforge.pmd.RuleSetNotFoundException;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.RulesetsFactoryUtils;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.util.IOUtil;
import net.sourceforge.pmd.util.ResourceLoader;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        map = new HashMap<>();
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
        List<DataSource> fileDataSources = new ArrayList<>(files.size());
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
                pmdConfig.getLanguageVersionDiscoverer().setDefaultLanguageVersion(srcType);
            }
        }
        final List<DefaultMutableTreeNode> pmdResults = new ArrayList<>();
        try {
            RuleSetFactory ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(pmdConfig, new ResourceLoader());
            pmdConfig.setRuleSets(rule);
            pmdConfig.setReportFile(File.createTempFile("pmd", "report").getAbsolutePath());
            PMDResultRenderer renderer = new PMDResultRenderer(pmdResults);

            List<Renderer> renderers = new LinkedList<>();
            renderers.add(renderer);

            renderer.setWriter(IOUtil.createWriter(pmdConfig.getReportFile()));
            renderer.start();

            RuleContext ctx = new RuleContext();

            pmdConfig.setThreads(0);
            PMD.processFiles(pmdConfig, ruleSetFactory, files, ctx, renderers);

            renderer.end();
            renderer.flush();

            renderer.renderErrors();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return pmdResults;
    }

    private String shortMessage(String message)
    {
        String shortMessage = message;
        if (shortMessage.length() > 180)
        {
            shortMessage = message.substring(0, 180) + "...";
        }
        return shortMessage;
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

    private class PMDResultRenderer extends AbstractIncrementingRenderer {

        private final List<DefaultMutableTreeNode> pmdResults;

        public PMDResultRenderer(List<DefaultMutableTreeNode> pmdResults) {
            super("pmdplugin", "PMD plugin renderer");
            this.pmdResults = pmdResults;
        }

        @Override
        public void renderFileViolations(Iterator<RuleViolation> violations) throws IOException {
            PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
            if (PMDResultCollector.report == null) {
                PMDResultCollector.report = new Report();
            }
            for (; violations.hasNext();) {
                RuleViolation iRuleViolation = violations.next();
                PMDResultCollector.report.addRuleViolation(iRuleViolation);
                String message = iRuleViolation.getRule().getName() + ". " + iRuleViolation.getRule().getDescription();
                DefaultMutableTreeNode node = map.get(message);
                if (node == null) {
                    node = nodeFactory.createNode(shortMessage(message));
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

        public void renderErrors() {
            if (!errors.isEmpty()) {
                if (PMDResultCollector.report == null) {
                    PMDResultCollector.report = new Report();
                }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(new PMDErrorNode(errors.get(0).getMsg()));
                pmdResults.add(node);
            }
        }

        public String defaultFileExtension() {
            return "txt";
        }
    }
}
