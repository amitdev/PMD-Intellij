package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.HasRule;
import com.intellij.plugins.bodhi.pmd.core.RuleKey;
import net.sourceforge.pmd.lang.rule.Rule;
import net.sourceforge.pmd.lang.rule.RulePriority;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * PMD branch tree node for rules. It has a Severity icon based on rule priority and is sortable based on first priority and then name.
 *
 * @author jborgers
 */
public class PMDRuleNode extends PMDRuleSetEntryNode implements HasRule {

    private final RulePriority priority;
    private final RuleKey ruleKey;
    private final Rule rule;

    /**
     * Create a node with the given value as rule
     *
     * @param rule    The PMD rule to set.
     */
    public PMDRuleNode(Rule rule) {
        super(rule.getName());
        this.rule = rule;
        priority = rule.getPriority();
        this.ruleKey = new RuleKey(rule);
    }

    @Override
    public Rule getRule() {
        return rule;
    }

    public String getRuleExternalInfoUrl() {
        return rule.getExternalInfoUrl();
    }

    @Override
    public synchronized void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.setIconForRulePriority(priority);
        super.render(cellRenderer, expanded);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PMDRuleNode that = (PMDRuleNode) o;
        return Objects.equals(ruleKey, that.ruleKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleKey);
    }

    @Override
    public int compareTo(@NotNull PMDRuleSetEntryNode o) {
        if (o instanceof PMDRuleNode) {
            return ruleKey.compareTo(((PMDRuleNode) o).ruleKey);
        }
        return -1; // always before suppressed
    }
}
