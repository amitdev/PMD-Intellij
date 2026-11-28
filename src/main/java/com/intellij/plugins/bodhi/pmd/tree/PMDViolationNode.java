package com.intellij.plugins.bodhi.pmd.tree;

import com.intellij.plugins.bodhi.pmd.core.HasMessage;
import com.intellij.plugins.bodhi.pmd.core.HasRule;
import com.intellij.plugins.bodhi.pmd.core.PMDViolation;
import com.intellij.plugins.bodhi.pmd.core.SustainabilityLevel;
import net.sourceforge.pmd.lang.rule.Rule;

import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;

/**
 * This is a tree leaf node that will encapsulate a PMD Violation. It implements
 * Navigatable, so that the user can navigate to the source of the problem.
 *
 * @author bodhi
 * @version 1.2
 */
public class PMDViolationNode extends PMDLeafNode implements HasRule, HasMessage {

    /** Lazily computed sustainability level of the violated rule; null when the rule has none. */
    private SustainabilityLevel sustainabilityLevel;
    private boolean sustainabilityLevelComputed;


    private final PMDViolation pmdViolation;

    /**
     * Create a node with the given pmd violation.
     *
     * @param pmdViolation The violation to encapsulate
     */
    public PMDViolationNode(PMDViolation pmdViolation) {
        this.pmdViolation = pmdViolation;
    }

    public PMDViolation getPmdViolation() {
        return pmdViolation;
    }


    /**
     * Open editor and select/navigate to the correct line and column.
     *
     * @param requestFocus Focus the editor.
     */
    public void navigate(boolean requestFocus) {
        highlightFindingInEditor(pmdViolation);
    }

    public String getToolTip() {
        return pmdViolation.getDescription();
    }

    public void render(PMDCellRenderer cellRenderer, boolean expanded) {
        cellRenderer.setIconForRulePriority(pmdViolation.getRulePriority());
        //Show violation position greyed, like idea shows.
        cellRenderer.append(pmdViolation.getPositionText(), GRAYED_ATTRIBUTES);
        cellRenderer.append(pmdViolation.getClassAndMethodMsg());
        cellRenderer.append(pmdViolation.getPackageMsg(), GRAYED_ATTRIBUTES);
    }

    @Override
    public int getViolationCount() {
        return 1;
    }

    @Override
    public int getSevViolationCount(Severity sev) {
        return (sev.getRulePriority() == pmdViolation.getRulePriority()) ? 1 : 0;
    }

    @Override
    public int getSustainabilityViolationCount(SustainabilityLevel level) {
        if (!sustainabilityLevelComputed) {
            // the rule doesn't change; computed once, as the counts are recalculated for every level and rule set
            sustainabilityLevel = SustainabilityLevel.of(getRule()).orElse(null);
            sustainabilityLevelComputed = true;
        }
        return level == sustainabilityLevel ? 1 : 0;
    }

    @Override
    public Rule getRule() {
        return pmdViolation.getRule();
    }

    @Override
    public String getMessage() {
        return pmdViolation.getMessage();
    }
}
