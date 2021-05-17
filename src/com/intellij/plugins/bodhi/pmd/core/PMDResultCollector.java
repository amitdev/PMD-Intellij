package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.bodhi.pmd.PMDConfigurationForm;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.PMDUtil;
import com.intellij.plugins.bodhi.pmd.tree.PMDBranchNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDTreeNodeFactory;
import net.sourceforge.pmd.*;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.util.IOUtil;
import net.sourceforge.pmd.util.ResourceLoader;
import net.sourceforge.pmd.util.datasource.DataSource;
import net.sourceforge.pmd.util.datasource.FileDataSource;

import java.io.File;
import java.io.IOException;
import java.util.*;



/**
 * Responsible for running PMD and collecting the results which can be represented in
 * tree format.
 *
 * @author bodhi
 * @version 1.3
 */
public class PMDResultCollector {

    private static Report report = new Report();

    /**
     * Creates an instance of PMDResultCollector.
     */
    public PMDResultCollector() {}

    /**
     * Clears the pmd results Report by assigning a new one
     */
    public static void clearReport() {
        report = new Report();
    }

    /**
     * Returns the report with pmd results
     * @return the report with pmd results
     */
    public static Report getReport() {
        return report;
    }
    /**
     * Runs the given ruleSet(s) on given set of files and returns the result.
     *
     * @param files The files to run PMD on
     * @param ruleSets The ruleSet(s) to run
     * @return list of results
     */
    public List<PMDBranchNode> runPMDAndGetResults(List<File> files, String ruleSets, PMDProjectComponent comp) {
        List<DataSource> fileDataSources = new ArrayList<>(files.size());
        for (File file : files) {
            fileDataSources.add(new FileDataSource(file));
        }
        return runPMDAndGenerateReport(fileDataSources, ruleSets, comp);
    }

    /**
     * Runs PMD on given set of files and generates the Report.
     *
     * @param files The list of files to run PMD
     * @param ruleSets The ruleSets(s) to run
     * @return The pmd Report
     */
    private List<PMDBranchNode> runPMDAndGenerateReport(List<DataSource> files, String ruleSets, PMDProjectComponent comp) {
        Map<String, String> options = comp.getOptions();
        Project project = comp.getCurrentProject();

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

        final List<PMDBranchNode> pmdRuleSetResults = new ArrayList<>();
        try {
            pmdConfig.prependClasspath(PMDUtil.getFullClassPathForAllModules(project));

            RuleSetFactory ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(pmdConfig, new ResourceLoader());
            pmdConfig.setRuleSets(ruleSets);
            pmdConfig.setReportFile(File.createTempFile("pmd", "report").getAbsolutePath());

            pmdConfig.setShowSuppressedViolations(true);
            //AnalysisCache cache = new FileAnalysisCache(File.createTempFile("pmd-analysis", "cache"));
            //pmdConfig.setAnalysisCache(cache);

            PMDBranchNode errorsNode = comp.getResultPanel().getNewProcessingErrorsNode();
            PMDResultAsTreeRenderer treeRenderer = new PMDResultAsTreeRenderer(pmdRuleSetResults, errorsNode);

            String exportUrlFromForm = options.get(PMDConfigurationForm.STATISTICS_URL);
            boolean exportStats = (PMDUtil.isValidUrl(exportUrlFromForm));
            String exportUrl = exportUrlFromForm;
            if (!exportStats || exportUrl.contains("localhost")) { // cmdline arg overrides localhost from form for testing
                exportUrl = System.getProperty("pmdStatisticsUrl", exportUrl);
                exportStats = (PMDUtil.isValidUrl(exportUrl));
            }

            List<Renderer> renderers = new LinkedList<>();
            renderers.add(treeRenderer);
            treeRenderer.setWriter(IOUtil.createWriter(pmdConfig.getReportFile()));
            treeRenderer.start();
            PMDJsonExportingRenderer exportingRenderer = null;
            if (exportStats) {
                exportingRenderer = new PMDJsonExportingRenderer(exportUrl);
                renderers.add(exportingRenderer);
                exportingRenderer.start();
            }

            RuleContext ctx = new RuleContext();

            pmdConfig.setThreads(0); // threads == 0 : single threaded
            PMD.processFiles(pmdConfig, ruleSetFactory, files, ctx, renderers);

            treeRenderer.end();
            treeRenderer.flush();
            if (exportStats) {
                exportingRenderer.end();
                exportingRenderer.flush();
                String exportErrMsg = exportingRenderer.exportJsonData();
                comp.getResultPanel().getRootNode().setExportErrorMsg(exportErrMsg);
                //treeRenderer.showExportMsg(exportErrMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pmdRuleSetResults;
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
     * Verifies whether the rule set specified at the path is a valid PMD rule set.
     *
     * @param path path of the rule set
     * @return true if valid rule set, false otherwise.
     */
    public static String isValidRuleSet(String path) {
        Thread.currentThread().setContextClassLoader(PMDResultCollector.class.getClassLoader());
        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        try {
            RuleSet rs = ruleSetFactory.createRuleSet(path);
            if (rs.getRules().size() != 0) {
                return "";
            }
        } catch (RuleSetNotFoundException | RuntimeException e) {
            return e.getMessage();
        }
        return "Invalid File";
    }

    public static RuleSet loadRuleSet(String path) throws InvalidRuleSetException {
        Thread.currentThread().setContextClassLoader(PMDResultCollector.class.getClassLoader());
        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        try {
            RuleSet rs = ruleSetFactory.createRuleSet(path);
            if (rs.getRules().size() != 0) {
                return rs;
            }
        } catch (RuntimeException | RuleSetNotFoundException e) {
            throw  new InvalidRuleSetException(e);
        }
        throw new InvalidRuleSetException("Invalid File");
    }

    public static class InvalidRuleSetException extends Exception {

        public InvalidRuleSetException(final String message) {
            super(message);
        }

        public InvalidRuleSetException(final Throwable cause) {
            super(cause);
        }

    }

    private class PMDResultAsTreeRenderer extends AbstractIncrementingRenderer {

        private final Map<String, PMDBranchNode> ruleNameToNodeMap;
        private final List<PMDBranchNode> pmdRuleResultNodes;
        private final PMDBranchNode processingErrorsNode;
        private final Set<String> filesWithError = new HashSet<>();

        public PMDResultAsTreeRenderer(List<PMDBranchNode> pmdRuleSetResults, PMDBranchNode errorsNode) {
            super("pmdplugin", "PMD plugin renderer");
            this.pmdRuleResultNodes = pmdRuleSetResults;
            processingErrorsNode = errorsNode;
            ruleNameToNodeMap = new LinkedHashMap<>(); // linked to keep insertion order
        }

        @Override
        public void renderFileViolations(Iterator<RuleViolation> violations) throws IOException {
            PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
            while (violations.hasNext()) {
                RuleViolation iRuleViolation = violations.next();
                PMDResultCollector.report.addRuleViolation(iRuleViolation);
                String name = iRuleViolation.getRule().getName();
                PMDBranchNode ruleNode = ruleNameToNodeMap.get(name);
                if (ruleNode == null) {
                    ruleNode = nodeFactory.createBranchNode(name);
                    ruleNode.setToolTip(iRuleViolation.getRule().getDescription());
                    ruleNameToNodeMap.put(name, ruleNode);
                }
                ruleNode.add(nodeFactory.createViolationLeafNode(new PMDViolation(iRuleViolation)));
            }
            for (PMDBranchNode ruleNode : ruleNameToNodeMap.values()) {
                if (ruleNode.getChildCount() > 0 && !pmdRuleResultNodes.contains(ruleNode)) {
                    pmdRuleResultNodes.add(ruleNode);
                }
            }
        }

        private void renderErrors() {
            if (!errors.isEmpty()) {
                PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
                for (Report.ProcessingError error : errors) {
                    if (!filesWithError.contains(error.getFile())) {
                        processingErrorsNode.add(nodeFactory.createErrorLeafNode(new PMDProcessingError(error)));
                        filesWithError.add(error.getFile());
                    }
                }
            }
        }

        @Override
        public void end() throws IOException {
            renderSuppressedViolations();
            renderErrors();
        }

        private void renderSuppressedViolations() {
            if (!suppressed.isEmpty()) {
                PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
                PMDBranchNode suppressedByNoPmdNode = nodeFactory.createBranchNode("Suppressed violations by //NOPMD");
                PMDBranchNode suppressedByAnnotationNode = nodeFactory.createBranchNode("Suppressed violations by Annotation");
                for (Report.SuppressedViolation suppressed : suppressed) {
                    if (suppressed.suppressedByAnnotation()) {
                        suppressedByAnnotationNode.add(nodeFactory.createSuppressedLeafNode(new PMDSuppressedViolation(suppressed)));
                    }
                    else {
                        suppressedByNoPmdNode.add(nodeFactory.createSuppressedLeafNode(new PMDSuppressedViolation(suppressed)));
                    }
                }
                suppressedByAnnotationNode.calculateCounts();
                if (suppressedByAnnotationNode.getSuppressedCount() > 0) {
                    pmdRuleResultNodes.add(suppressedByAnnotationNode);
                }
                suppressedByNoPmdNode.calculateCounts();
                if (suppressedByNoPmdNode.getSuppressedCount() > 0) {
                    pmdRuleResultNodes.add(suppressedByNoPmdNode);
                }
            }
        }

        public String defaultFileExtension() {
            return "txt";
        }

    }
}
