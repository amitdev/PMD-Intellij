package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.bodhi.pmd.PMDConfigurationForm;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.PMDUtil;
import com.intellij.plugins.bodhi.pmd.tree.*;
import net.sourceforge.pmd.*;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.util.IOUtil;
import org.jetbrains.annotations.NotNull;

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
     * lazily loaded path to ruleset map, should only contain valid rule sets
     */
    private static final Map<String, RuleSet> pathToRuleSet = new HashMap<>();

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
    public List<PMDRuleSetEntryNode> runPMDAndGetResults(List<File> files, String ruleSets, PMDProjectComponent comp) {
        Map<String, String> options = comp.getOptions();
        Project project = comp.getCurrentProject();

        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        final List<PMDRuleSetEntryNode> pmdRuleSetResults = new ArrayList<>();
        try {
            PMDConfiguration pmdConfig = getPmdConfig(ruleSets, options, project);

            PMDErrorBranchNode errorsNode = comp.getResultPanel().getNewProcessingErrorsNode();
            PMDResultAsTreeRenderer treeRenderer = new PMDResultAsTreeRenderer(pmdRuleSetResults, errorsNode);
            treeRenderer.setWriter(IOUtil.createWriter(pmdConfig.getReportFile()));
            treeRenderer.start();

            List<Renderer> renderers = new LinkedList<>();
            renderers.add(treeRenderer);

            PMDJsonExportingRenderer exportingRenderer = addExportRenderer(options);
            if (exportingRenderer != null) renderers.add(exportingRenderer);

            try (PmdAnalysis pmd = PmdAnalysis.create(pmdConfig)) {
                files.forEach(file -> pmd.files().addFile(file.toPath()));
                pmd.addRenderers(renderers);
                pmd.performAnalysis();
            }

            if (exportingRenderer != null) {
                String exportErrMsg = exportingRenderer.exportJsonData();
                comp.getResultPanel().getRootNode().setExportErrorMsg(exportErrMsg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pmdRuleSetResults;
    }

    private PMDJsonExportingRenderer addExportRenderer(Map<String, String> options) throws IOException {
        PMDJsonExportingRenderer exportingRenderer = null;
        String exportUrlFromForm = options.get(PMDConfigurationForm.STATISTICS_URL);
        boolean exportStats = (PMDUtil.isValidUrl(exportUrlFromForm));
        String exportUrl = exportUrlFromForm;
        if (!exportStats || exportUrl.contains("localhost")) { // cmdline arg overrides localhost from form for testing
            exportUrl = System.getProperty("pmdStatisticsUrl", exportUrl);
            exportStats = (PMDUtil.isValidUrl(exportUrl));
        }
        if (exportStats) {
            exportingRenderer = new PMDJsonExportingRenderer(exportUrl);
            exportingRenderer.start();
        }
        return exportingRenderer;
    }

    @NotNull
    private PMDConfiguration getPmdConfig(String ruleSets, Map<String, String> options, Project project) throws IOException {
        PMDConfiguration pmdConfig = new PMDConfiguration();
        String type = options.get("Target JDK");
        if (type != null) {
            LanguageVersion version = LanguageRegistry.findLanguageByTerseName("java").getVersion(type);
            if (version != null)
                pmdConfig.setDefaultLanguageVersion(version);
        }
        pmdConfig.prependAuxClasspath(PMDUtil.getFullClassPathForAllModules(project));

        pmdConfig.addRuleSet(ruleSets);
        pmdConfig.setReportFile(File.createTempFile("pmd", "report").getAbsolutePath());
        pmdConfig.setShowSuppressedViolations(true);
        pmdConfig.setThreads(0);
        return pmdConfig;
    }

    /**
     * Verifies whether the rule set specified at the path is a valid PMD rule set. Always loads from file.
     *
     * @param path path of the rule set
     * @return empty String for valid, an error message for invalid.
     */
    public static String isValidRuleSet(String path) {
        Thread.currentThread().setContextClassLoader(PMDResultCollector.class.getClassLoader());

        try {
            RuleSet rs = new RuleSetLoader().loadFromResource(path);
            if (rs.getRules().size() != 0) {
                pathToRuleSet.put(path, rs);
                return "";
            }
        } catch (RuleSetLoadException e) {
            return e.getMessage();
        }
        return "No rules found";
    }

    /**
     * Return the name of the RuleSet or an error message when the RuleSet is not valid
     * @param ruleSetPath the path of the rule set
     * @return the name of the RuleSet or an error message when the RuleSet is not valid
     */
    public static String getRuleSetName(String ruleSetPath) {
        String ruleSetName;
        try {
            ruleSetName = PMDResultCollector.getRuleSet(ruleSetPath).getName(); // from the xml
        } catch (InvalidRuleSetException e) {
            String msg = (e.getCause() == null) ? e.getMessage(): e.getCause().getMessage();
            ruleSetName = msg.substring(0, Math.min(25, msg.length()));
        }
        return ruleSetName;
    }

    /**
     * Return the description of the RuleSet or "<invalid>" message when the RuleSet is not valid
     * @param ruleSetPath the path of the rule set
     * @return the description of the RuleSet or "<invalid>" message when the RuleSet is not valid
     */
    public static String getRuleSetDescription(String ruleSetPath) {
        String ruleSetDesc;
        try {
            ruleSetDesc = PMDResultCollector.getRuleSet(ruleSetPath).getDescription(); // from the xml
        } catch (InvalidRuleSetException e) {
            ruleSetDesc = "<invalid>";
        }
        return ruleSetDesc;
    }

    /**
     * Get a ruleSet from memory, or load it from resource when not loaded yet
     * @param path the path of the ruleSet
     */
    public static RuleSet getRuleSet(String path) throws InvalidRuleSetException {
        RuleSet rs = pathToRuleSet.get(path);
        if (rs == null) {
            rs = loadRuleSet(path);
            // no exception, loading succeeds
            pathToRuleSet.put(path, rs);
        }
        return rs;
    }

    public static RuleSet loadRuleSet(String path) throws InvalidRuleSetException {
        Thread.currentThread().setContextClassLoader(PMDResultCollector.class.getClassLoader());
        try {
            RuleSet rs = new RuleSetLoader().loadFromResource(path);
            if (!rs.getRules().isEmpty()) {
                return rs;
            }
        } catch (RuleSetLoadException e) {
            throw new InvalidRuleSetException(e);
        }
        throw new InvalidRuleSetException("No rules found");
    }

    public static class InvalidRuleSetException extends Exception {

        public InvalidRuleSetException(final String message) {
            super(message);
        }

        public InvalidRuleSetException(final Throwable cause) {
            super(cause);
        }

    }

    private static class PMDResultAsTreeRenderer extends AbstractIncrementingRenderer {

        private final Map<RuleKey, PMDRuleNode> ruleKeyToNodeMap;
        private final List<PMDRuleSetEntryNode> pmdRuleResultNodes;
        private final PMDErrorBranchNode processingErrorsNode;
        private final Set<String> filesWithError = new HashSet<>();

        public PMDResultAsTreeRenderer(List<PMDRuleSetEntryNode> pmdRuleSetResults, PMDErrorBranchNode errorsNode) {
            super("pmdplugin", "PMD plugin renderer");
            this.pmdRuleResultNodes = pmdRuleSetResults;
            processingErrorsNode = errorsNode;
            ruleKeyToNodeMap = new TreeMap<>(); // order by priority and then name
        }

        @Override
        public void renderFileViolations(Iterator<RuleViolation> violations) {
            PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
            while (violations.hasNext()) {
                RuleViolation iRuleViolation = violations.next();
                PMDResultCollector.report.addRuleViolation(iRuleViolation);
                Rule rule = iRuleViolation.getRule();
                RuleKey key = new RuleKey(rule);
                PMDRuleNode ruleNode = ruleKeyToNodeMap.get(key);
                if (ruleNode == null) {
                    ruleNode = nodeFactory.createRuleNode(rule);
                    ruleNode.setToolTip(rule.getDescription());
                    ruleKeyToNodeMap.put(key, ruleNode);
                }
                ruleNode.add(nodeFactory.createViolationLeafNode(new PMDViolation(iRuleViolation)));
            }
            for (PMDRuleNode ruleNode : ruleKeyToNodeMap.values()) {
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
        public void end() {
            renderSuppressedViolations();
            renderErrors();
        }

        private void renderSuppressedViolations() {
            if (!suppressed.isEmpty()) {
                PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
                PMDSuppressedBranchNode suppressedByNoPmdNode = nodeFactory.createSuppressedBranchNode("Suppressed violations by //NOPMD");
                PMDSuppressedBranchNode suppressedByAnnotationNode = nodeFactory.createSuppressedBranchNode("Suppressed violations by Annotation");
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
