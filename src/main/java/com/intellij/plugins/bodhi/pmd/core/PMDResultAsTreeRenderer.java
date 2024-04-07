package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.plugins.bodhi.pmd.tree.PMDErrorBranchNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleSetEntryNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDSuppressedBranchNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDTreeNodeFactory;
import com.intellij.plugins.bodhi.pmd.tree.PMDUselessSuppressionBranchNode;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.renderers.AbstractIncrementingRenderer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Represents the renderer for the PMD results in a tree.
 * Only core package classes are coupled with the PMD Library.
 *
 * @author jborgers
 */
public class PMDResultAsTreeRenderer extends AbstractIncrementingRenderer {

    private static final Log log = LogFactory.getLog(PMDResultAsTreeRenderer.class);
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
                PMDResultCollector.getReport().addRuleViolation(ruleViolation);
                Rule rule = ruleViolation.getRule();
                RuleKey key = new RuleKey(rule);
                PMDRuleNode ruleNode = ruleKeyToNodeMap.get(key);
                if (ruleNode == null) {
                    ruleNode = nodeFactory.createRuleNode(rule);
                    ruleNode.setToolTip(rule.getDescription());
                    ruleKeyToNodeMap.put(key, ruleNode);
                }
                ruleNode.add(nodeFactory.createViolationLeafNode(new PMDViolation(ruleViolation)));
                uselessSupHelper.storeRuleNameForMethod(ruleViolation);
            }
            catch(Exception e) {
                // report and swallow so following violations will still be rendered
                log.error("Exception caught and swallowed: ", e);
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
                    if (!processingErrorsNode.hasFile(error.getFile())) {
                        processingErrorsNode.add(nodeFactory.createErrorLeafNode(new PMDProcessingError(error)));
                        processingErrorsNode.registerFile(error.getFile());
                    }
                }
                catch(Exception e) {
                    // report and swallow so following processing error will still be rendered
                    log.error("Exception caught and swallowed: ", e);
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
                    if (suppressed.suppressedByAnnotation()) {
                        suppressedByAnnotationNode.add(nodeFactory.createSuppressedLeafNode(new PMDSuppressedViolation(suppressed)));
                        uselessSupHelper.storeRuleNameForMethod(suppressed);
                    } else {
                        suppressedByNoPmdNode.add(nodeFactory.createSuppressedLeafNode(new PMDSuppressedViolation(suppressed)));
                    }
                }
                catch(Exception e) {
                    // report and swallow so following suppressed violations will still be rendered
                    log.error("Exception caught and swallowed: ", e);
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
                    log.error("Exception caught and swallowed: ", e);
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
