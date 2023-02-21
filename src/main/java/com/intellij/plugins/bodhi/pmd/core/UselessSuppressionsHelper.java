package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.bodhi.pmd.tree.PMDRuleNode;
import com.intellij.plugins.bodhi.pmd.tree.PMDViolationNode;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleViolation;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a helper for the PMDResultAsTreeRenderer dealing with useless suppressions.
 * Suppressions with @SuppressWarnings are considered useless if no actual violations are suppressed by the annotation.
 * Only core package classes are coupled with the PMD Library.
 *
 * @author jborgers
 */
public class UselessSuppressionsHelper {
    static final Pattern NEXT_METHOD_NAME_PATTERN =
            Pattern.compile("\\R*\\s*(\\/\\/.*\\R|\\/\\*[.\\n\\r]*\\*\\/)*\\R*\\s*[\\w\\s]+\\s+([\\w]+)\\(");
    // two types of comments: \R*\s*(\/\/.*\R|\/\*[.\n\r]*\*\/)*\R*\s*
    static final Pattern NEXT_FIELD_NAME_PATTERN = Pattern.compile("\\R*\\s*\\R*\\s*(\\/\\/.*\\R|\\/\\*[.\\n\\r]*\\*\\/)*\\R*\\s*[\\w\\s<>,?]+\\s+([\\w]+)\\s*[=;]");
    static final String NO_METHOD = "<nom>";
    final Map<String, Set<String>> classMethodToRuleNameOfSuppressedViolationsMap = new HashMap<>();
    final Map<String, Set<String>> classMethodToRuleNameOfViolationsMap = new HashMap<>();
    static final RuleKey USING_SUPPRESS_KEY = new RuleKey("UsingSuppressWarnings", 5);
    private final String ruleSetPath;

    /**
     * the rule names of the rule set, lazily initialized, only when needed
     */
    private Set<String> ruleNames;
    private volatile ViolatingAnnotationHolder annotationContextResult;

    UselessSuppressionsHelper(String ruleSetPath) {
        this.ruleSetPath = ruleSetPath;
    }

    void storeRuleNameForMethod(Report.SuppressedViolation suppressed) {
        RuleViolation violation = suppressed.getRuleViolation();
        if (!violation.getMethodName().isEmpty()) {
            // store for method
            String methodKey = violation.getPackageName() + "-" + violation.getClassName() + "-" + violation.getMethodName();
            Set<String> suppressedMethodRuleNames = classMethodToRuleNameOfSuppressedViolationsMap.get(methodKey);
            if (suppressedMethodRuleNames == null) {
                suppressedMethodRuleNames = new HashSet<>();
            }
            suppressedMethodRuleNames.add(violation.getRule().getName());
            classMethodToRuleNameOfSuppressedViolationsMap.put(methodKey, suppressedMethodRuleNames);
        }
        // store for class and fields
        String classKey = violation.getPackageName() + "-" + violation.getClassName() + "-" + UselessSuppressionsHelper.NO_METHOD;
        Set<String> suppressedClassRuleNames = classMethodToRuleNameOfSuppressedViolationsMap.get(classKey);
        if (suppressedClassRuleNames == null) {
            suppressedClassRuleNames = new HashSet<>();
        }
        suppressedClassRuleNames.add(violation.getRule().getName());
        classMethodToRuleNameOfSuppressedViolationsMap.put(classKey, suppressedClassRuleNames);
    }

    void storeRuleNameForMethod(RuleViolation violation) {
        String methodName = violation.getMethodName();
        if (methodName != null && !methodName.isEmpty()) {
            // store for method
            String methodKey = violation.getPackageName() + "-" + violation.getClassName() + "-" + methodName;
            Set<String> violationMethodRuleNames = classMethodToRuleNameOfViolationsMap.get(methodKey);
            if (violationMethodRuleNames == null) {
                violationMethodRuleNames = new HashSet<>();
            }
            violationMethodRuleNames.add(violation.getRule().getName());
            classMethodToRuleNameOfViolationsMap.put(methodKey, violationMethodRuleNames);
        }
        //String fieldName = violation.getVariableName(); - BUG in PMD, returns "VariableDeclaratorId"
        // because this is missing, we map field annotations on the class and lose field resolution

        // store for class
        String classKey = violation.getPackageName() + "-" + violation.getClassName() + "-" + NO_METHOD;
        Set<String> violationClassRuleNames = classMethodToRuleNameOfViolationsMap.get(classKey);
        if (violationClassRuleNames == null) {
            violationClassRuleNames = new HashSet<>();
        }
        violationClassRuleNames.add(violation.getRule().getName());
        classMethodToRuleNameOfViolationsMap.put(classKey, violationClassRuleNames);
    }

    List<PMDUselessSuppression> findUselessSuppressions(Map<RuleKey, PMDRuleNode> ruleKeyToNodeMap) {
        List<PMDUselessSuppression> uselessSuppressions = Collections.emptyList();
        if (ruleKeyToNodeMap.containsKey(USING_SUPPRESS_KEY)) {
            uselessSuppressions = new ArrayList<>();
            PMDRuleNode ruleNode = ruleKeyToNodeMap.get(USING_SUPPRESS_KEY);
            List<TreeNode> list = Collections.list(ruleNode.children());
            for (TreeNode node : list) {
                addNodeIfUseless(uselessSuppressions, (PMDViolationNode) node);
            }
        }
        return uselessSuppressions;
    }

    private void addNodeIfUseless(List<PMDUselessSuppression> uselessSuppressions, PMDViolationNode node) {
        PMDViolation pmdViolation = node.getPmdViolation();
        ViolatingAnnotationHolder annotationContext = getAnnotationContext(pmdViolation);
        if (annotationContext != null) {
            String annotationValue = annotationContext.annotationValue;
            String annotatedRuleName;
            if (annotationValue.startsWith("PMD.") || annotationValue.startsWith("pmd:")) { // PMD and Sonar resp.
                // for PMD, they may appear in suppressed, for Sonar suppression, violations suppressed in PMD
                // for PMD. - find if this suppressed occurs in the method, if not: useless
                // for pmd: - find if this violation or a suppressed occurs in the method, if not: useless
                annotatedRuleName = annotationValue.substring(4);
                // if rule in list of rules of ruleset
                if (ruleSetContains(annotatedRuleName)) {
                    String methodKey = createMethodKey(pmdViolation, annotationContext);
                    Set<String> suppressedRuleNames = classMethodToRuleNameOfSuppressedViolationsMap.get(methodKey);
                    Set<String> violationRuleNames = classMethodToRuleNameOfViolationsMap.get(methodKey);
                    boolean actuallySuppressing = suppressedRuleNames != null && suppressedRuleNames.contains(annotatedRuleName);
                    boolean actuallyViolating = violationRuleNames != null && violationRuleNames.contains(annotatedRuleName);
                    if (!actuallySuppressing && !actuallyViolating) {
                        // add UselessSuppression
                        uselessSuppressions.add(new PMDUselessSuppression(pmdViolation, annotatedRuleName));
                    }
                }
            } else {
                // cannot deal with other, non-pmd suppressions
            }
        }
    }

    @NotNull String createMethodKey(PMDViolation pmdViolation, ViolatingAnnotationHolder annotationContext) {
        String packageName = pmdViolation.getPackageName();
        String className = pmdViolation.getClassName();
        String methodName = annotationContext.method;
        return packageName + "-" + className + "-" + methodName;
    }

    boolean ruleSetContains(String ruleName) {
        if (ruleNames == null) {
            try {
                Collection<Rule> rules = PMDResultCollector.getRuleSet(ruleSetPath).getRules();
                ruleNames = new HashSet<>(rules.size(), 1);
                for (Rule rule : rules) {
                    ruleNames.add(rule.getName());
                }
            } catch (PMDResultCollector.InvalidRuleSetException e) {
                throw new RuntimeException(e);
            }
        }
        return ruleNames.contains(ruleName); // O(1) access time
    }

    /**
     * Find out context of annotation from the document. Implemented with text matching.
     * Limitation: Cannot deal with all cases, best effort.
     * TODO use proper parsing with PSIDocumentManager
     *
     * @param annotationViolation the annotation found as violation
     * @return the annotation context result
     */
    ViolatingAnnotationHolder getAnnotationContext(PMDViolation annotationViolation) {
        final VirtualFile virtualFile = LocalFileSystem.getInstance().findFileByPath(
                annotationViolation.getFilename().replace(File.separatorChar, '/'));
        annotationContextResult = null;
        if (virtualFile != null) {
            ApplicationManager.getApplication().runReadAction(() -> {
                Document doc = FileDocumentManager.getInstance().getDocument(virtualFile);
                int startOffset = doc.getLineStartOffset(annotationViolation.getBeginLine() - 1) + annotationViolation.getBeginColumn();
                int endOffset = doc.getLineStartOffset(annotationViolation.getEndLine() - 1) + annotationViolation.getEndColumn() - 1;
                String violatingAnnotation = doc.getText(new TextRange(startOffset, endOffset));
                String methodName;
                if (!annotationViolation.getMethodName().isEmpty()) { // an annotation inside a method
                    methodName = annotationViolation.getMethodName();
                } else {
                    int startAfter = doc.getLineStartOffset(annotationViolation.getEndLine());
                    String after = doc.getText(new TextRange(startAfter, doc.getTextLength() - 1));
                    methodName = findMethodName(after);
                }
                annotationContextResult = new ViolatingAnnotationHolder(violatingAnnotation, methodName);
            });
        }
        return annotationContextResult;
    }

    /**
     * Find out name of method following annotation, from the document. Implemented with text matching.
     * Limitation: Cannot deal with comments containing code, and not with all cases.
     * TODO use proper parsing with PSIDocumentManager
     *
     * @param afterAnno document after the annotation
     * @return name of the method, or NO_METHOD when annotation is on class or field
     */
    String findMethodName(String afterAnno) {
        int classIndex = afterAnno.indexOf(" class ");
        if (classIndex > 0) { // on class or has a subclass, remove afterAnno it
            afterAnno = afterAnno.substring(0, classIndex);
        }
        String methodName = NO_METHOD; // for class level annotations
        Matcher methodMatcher = NEXT_METHOD_NAME_PATTERN.matcher(afterAnno);
        if (methodMatcher.find()) {
            if (methodMatcher.groupCount() > 1) {
                methodName = methodMatcher.group(2);
                // it may be on a field
                String afterAnnoBeforeMethod = "";
                int methodPos = afterAnno.indexOf(methodName + "(");
                if (methodPos >= 0) {
                    afterAnnoBeforeMethod = afterAnno.substring(0, methodPos);
                }
                Matcher fieldMatcher = NEXT_FIELD_NAME_PATTERN.matcher(afterAnnoBeforeMethod);
                if (fieldMatcher.find()) {
                    methodName = NO_METHOD; // on field, map to class for now
                }
            }
        }
        return methodName;
    }

    static class ViolatingAnnotationHolder {
        ViolatingAnnotationHolder(String annotationValue, String method) {
            this.annotationValue = annotationValue;
            this.method = method;
        }
        private final String annotationValue;
        private final String method;
    }
}