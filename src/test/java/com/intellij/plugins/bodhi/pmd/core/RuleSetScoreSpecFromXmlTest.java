package com.intellij.plugins.bodhi.pmd.core;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/**
 * Verifies that the score marker survives PMD's parsing of a real rule set XML description.
 */
public class RuleSetScoreSpecFromXmlTest {

    @Test
    public void markerOnSecondLineOfRealRuleSetDescription() throws Exception {
        Path xml = Files.createTempFile("score-ruleset", ".xml");
        Files.writeString(xml, """
                <?xml version="1.0"?>
                <ruleset name="scored"
                         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
                    <description>
                        Some scored rules.
                        pmd-score: name=Test-score; weights=27,9,3,1,0; thresholds=0,5,10,15,25
                    </description>
                    <rule ref="category/java/bestpractices.xml/UnusedLocalVariable"/>
                </ruleset>
                """);
        try {
            String description = PMDResultCollector.loadRuleSet(xml.toString()).getDescription();
            RuleSetScoreSpec spec = RuleSetScoreSpec.parse(description).orElseThrow();
            assertEquals("Test-score", spec.name());
            assertArrayEquals(new int[]{27, 9, 3, 1, 0}, spec.weights());
            assertEquals("Some scored rules.", RuleSetScoreSpec.stripMarker(description));
        } finally {
            Files.deleteIfExists(xml);
        }
    }
}
