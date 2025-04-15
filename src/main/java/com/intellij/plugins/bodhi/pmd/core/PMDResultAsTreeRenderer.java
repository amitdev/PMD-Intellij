package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.plugins.bodhi.pmd.tree.*;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import net.sourceforge.pmd.reporting.ViolationSuppressor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Represents the renderer for the PMD results in a tree.
 * Only core package classes are coupled with the PMD Library.
 *
 * @author jborgers
 */
public class PMDResultAsTreeRenderer extends AbstractIncrementingRenderer {

    private static final Log log = LogFactory.getLog(PMDResultAsTreeRenderer.class);
    private static final String EXCEPTION_SWALLOWED = "Exception caught and swallowed: ";
    private final List<PMDRuleSetEntryNode> pmdRuleResultNodes;
    private final PMDErrorBranchNode processingErrorsNode;
    private final UselessSuppressionsHelper uselessSupHelper;
    private final Map<RuleKey, PMDRuleNode> ruleKeyToNodeMap = new TreeMap<>(); // order by priority and then name

    public PMDResultAsTreeRenderer(List<PMDRuleSetEntryNode> pmdRuleSetResults, PMDErrorBranchNode errorsNode, String ruleSetPath) {
        super("pmdplugin", "PMD plugin renderer");
        this.pmdRuleResultNodes = pmdRuleSetResults;
        processingErrorsNode = errorsNode;
        uselessSupHelper = new UselessSuppressionsHelper(ruleSetPath);
    }

    @Override
    public void renderFileViolations(Iterator<RuleViolation> violations) {
        PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
        while (violations.hasNext()) {
            try {
                RuleViolation ruleViolation = violations.next();
                Rule rule = ruleViolation.getRule();
                RuleKey key = new RuleKey(rule);
                PMDRuleNode ruleNode = ruleKeyToNodeMap.get(key);
                if (ruleNode == null) {
                    ruleNode = nodeFactory.createRuleNode(rule);
                    ruleKeyToNodeMap.put(key, ruleNode);
                }
                ruleNode.add(nodeFactory.createViolationLeafNode(new PMDViolation(ruleViolation)));
                uselessSupHelper.storeRuleNameForMethod(ruleViolation);
            }
            catch(Exception e) {
                // report and swallow so following violations will still be rendered
                log.error(EXCEPTION_SWALLOWED, e);
            }
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
                try {
                    if (!processingErrorsNode.hasFile(error.getFileId().getOriginalPath())) {
                        processingErrorsNode.add(nodeFactory.createErrorLeafNode(new PMDProcessingError(error)));
                        processingErrorsNode.registerFile(error.getFileId().getOriginalPath());
                    }
                }
                catch(Exception e) {
                    // report and swallow so following processing error will still be rendered
                    log.error(EXCEPTION_SWALLOWED, e);
                }
            }
        }
    }

    @Override
    public void end() {
        renderSuppressedViolations();
        renderUselessSuppressions();
        renderErrors();
    }

    private void renderSuppressedViolations() {
        if (!suppressed.isEmpty()) {
            PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
            PMDSuppressedBranchNode suppressedByNoPmdNode = nodeFactory.createSuppressedBranchNode("Suppressed violations by //NOPMD");
            PMDSuppressedBranchNode suppressedByAnnotationNode = nodeFactory.createSuppressedBranchNode("Suppressed violations by Annotation");
            for (Report.SuppressedViolation suppressed : suppressed) {
                try {
                    if (suppressed.getSuppressor() == ViolationSuppressor.NOPMD_COMMENT_SUPPRESSOR) {
                        suppressedByNoPmdNode.add(nodeFactory.createSuppressedLeafNode(new PMDSuppressedViolation(suppressed)));
                    } else {
                        suppressedByAnnotationNode.add(nodeFactory.createSuppressedLeafNode(new PMDSuppressedViolation(suppressed)));
                        uselessSupHelper.storeRuleNameForMethod(suppressed);
                    }
                }
                catch(Exception e) {
                    // report and swallow so following suppressed violations will still be rendered
                    log.error(EXCEPTION_SWALLOWED, e);
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

    private void renderUselessSuppressions() {
        List<PMDUselessSuppression> uselessSuppressions = uselessSupHelper.findUselessSuppressions(ruleKeyToNodeMap);
        if (!uselessSuppressions.isEmpty()) {
            PMDTreeNodeFactory nodeFactory = PMDTreeNodeFactory.getInstance();
            PMDUselessSuppressionBranchNode uselessSuppressionNode = nodeFactory.createUselessSuppressionBranchNode("Useless suppressions");
            for (PMDUselessSuppression uselessSuppression : uselessSuppressions) {
                try {
                    uselessSuppressionNode.add(nodeFactory.createUselessSuppressionLeafNode(uselessSuppression));
                }
                catch(Exception e) {
                    // report and swallow so following useless suppressions will still be rendered
                    log.error(EXCEPTION_SWALLOWED, e);
                }
            }
            uselessSuppressionNode.calculateCounts();
            if (uselessSuppressionNode.getUselessSuppressionCount() > 0) {
                pmdRuleResultNodes.add(uselessSuppressionNode);
            }
        }
    }

    public String defaultFileExtension() {
        return "txt";
    }

    @Override
    public void flush() {
    }
}
