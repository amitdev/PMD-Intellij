package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.bodhi.pmd.ConfigOption;
import com.intellij.plugins.bodhi.pmd.PMDProjectComponent;
import com.intellij.plugins.bodhi.pmd.PMDUtil;
import com.intellij.plugins.bodhi.pmd.tree.PMDErrorBranchNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleSetEntryNode;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.internal.util.IOUtil;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.document.TextFile;
import net.sourceforge.pmd.lang.rule.RuleSet;
import net.sourceforge.pmd.lang.rule.RuleSetLoadException;
import net.sourceforge.pmd.lang.rule.RuleSetLoader;
import net.sourceforge.pmd.renderers.Renderer;
import net.sourceforge.pmd.reporting.Report;
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

    private static Report report;

    /**
     * lazily loaded path to ruleset map, should only contain valid rule sets
     */
    private static final Map<String, RuleSet> pathToRuleSet = new HashMap<>();

    /**
     * Creates an instance of PMDResultCollector.
     */
    public PMDResultCollector() {}

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
     * @param files            The files to run PMD on
     * @param ruleSetPath      The path of the ruleSet to run
     * @return list of results
     */
    public List<PMDRuleSetEntryNode> runPMDAndGetResults(List<File> files, String ruleSetPath, PMDProjectComponent comp) {
        return this.runPMDAndGetResults(files, ruleSetPath, comp, null);
    }

    /**
     * Runs the given ruleSet(s) on given set of files and returns the result.
     *
     * @param files       The files to run PMD on
     * @param ruleSetPath The path of the ruleSet to run
     * @return list of results
     */
    public List<PMDRuleSetEntryNode> runPMDAndGetResults(List<File> files, String ruleSetPath, PMDProjectComponent comp, Renderer extraRenderer) {
        return runPMDAndGetResults(files,List.of(), ruleSetPath, comp, extraRenderer);
    }

    /**
     * Runs the given ruleSet(s) on given set of files and returns the result.
     *
     * @param files       The files to run PMD on
     * @param ruleSetPath The path of the ruleSet to run
     * @return list of results
     */
    public List<PMDRuleSetEntryNode> runPMDAndGetResults(List<File> files, List<TextFile> textFiles, String ruleSetPath, PMDProjectComponent comp, Renderer extraRenderer) {
        Map<ConfigOption, String> options = comp.getOptionToValue();
        Project project = comp.getCurrentProject();

        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        final List<PMDRuleSetEntryNode> pmdRuleSetResults = new ArrayList<>();
        try {
            PMDConfiguration pmdConfig = getPmdConfig(ruleSetPath, options, project);

            PMDErrorBranchNode errorsNode = comp.getResultPanel().getProcessingErrorsNode();
            PMDResultAsTreeRenderer treeRenderer = new PMDResultAsTreeRenderer(pmdRuleSetResults, errorsNode, ruleSetPath);
            treeRenderer.setWriter(IOUtil.createWriter(pmdConfig.getReportFilePath().toString()));
            treeRenderer.start();

            List<Renderer> renderers = new LinkedList<>();
            renderers.add(treeRenderer);

            PMDJsonExportingRenderer exportingRenderer = addExportRenderer(options);
            if (exportingRenderer != null) renderers.add(exportingRenderer);
            if (extraRenderer != null) renderers.add(extraRenderer);

            try (PmdAnalysis pmd = PmdAnalysis.create(pmdConfig)) {
                files.forEach(file -> pmd.files().addFile(file.toPath()));
                textFiles.forEach(pmd.files()::addFile);
                pmd.addRenderers(renderers);
                report = pmd.performAnalysisAndCollectReport();
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

    private PMDJsonExportingRenderer addExportRenderer(Map<ConfigOption, String> options) {
        PMDJsonExportingRenderer exportingRenderer = null;
        String exportUrlFromForm = options.get(ConfigOption.STATISTICS_URL);
        boolean exportStats = (PMDUtil.isValidUrl(exportUrlFromForm));
        String exportUrl = exportUrlFromForm;
        if (!exportStats || exportUrl.contains("localhost")) { // cmdline arg overrides localhost from form for testing
            exportUrl = System.getProperty("pmdStatisticsUrl", exportUrl);
            exportStats = (PMDUtil.isValidUrl(exportUrl));
        }
        if (exportStats) {
            exportingRenderer = new PMDJsonExportingRenderer(exportUrl);
            // exportingRenderer.start(); is already called from PmdAnalysis for all renderers, issue #114
        }
        return exportingRenderer;
    }

    @NotNull
    private PMDConfiguration getPmdConfig(String ruleSets, Map<ConfigOption, String> options, Project project) throws IOException {
        PMDConfiguration pmdConfig = new PMDConfiguration();
        String configVersion = options.get(ConfigOption.TARGET_JDK);
        if (configVersion != null) {
            LanguageVersion version = LanguageRegistry.PMD.getLanguageVersionById("java", configVersion);
            if (version != null)
                pmdConfig.setDefaultLanguageVersion(version);
        }
        pmdConfig.prependAuxClasspath(PMDUtil.getFullClassPathForAllModules(project));

        pmdConfig.addRuleSet(ruleSets);
        pmdConfig.setReportFile(File.createTempFile("pmd", "report").toPath());
        pmdConfig.setShowSuppressedViolations(true);

        String threads = options.get(ConfigOption.THREADS);
        if (threads == null || threads.isEmpty()) {
            pmdConfig.setThreads(PMDUtil.AVAILABLE_PROCESSORS);
        }
        else if (threads.equals("1")) {
            pmdConfig.setThreads(0); // 0 is a special value invoking in single thread mood
        } else {
            pmdConfig.setThreads(Integer.parseInt(threads));
        }
        return pmdConfig;
    }

    /**
     * Verifies whether the rule set specified at the path is a valid PMD rule set. Always loads from file/URL.
     *
     * @param path path of the rule set
     * @return empty String for valid, an error message for invalid.
     */
    public static String isValidRuleSet(String path) {
        Thread.currentThread().setContextClassLoader(PMDResultCollector.class.getClassLoader());

        try {
            RuleSet rs = new RuleSetLoader().loadFromResource(path);
            if (!rs.getRules().isEmpty()) {
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

}
