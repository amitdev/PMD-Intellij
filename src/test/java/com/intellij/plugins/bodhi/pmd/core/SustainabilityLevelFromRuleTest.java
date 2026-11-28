package com.intellij.plugins.bodhi.pmd.core;

import net.sourceforge.pmd.lang.rule.Rule;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * Reads the sustainability level from the tags property of a real rule, as jPinpoint rules define it.
 */
public class SustainabilityLevelFromRuleTest {

    @Test
    public void levelFromTagsPropertyOfXPathRule() throws Exception {
        Path xml = Files.createTempFile("tagged-ruleset", ".xml");
        Files.writeString(xml, """
                <?xml version="1.0"?>
                <ruleset name="tagged"
                         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
                    <description>Tagged rules</description>
                    <rule name="Tagged" language="java" message="tagged"
                          class="net.sourceforge.pmd.lang.rule.xpath.XPathRule">
                        <description>tagged</description>
                        <priority>3</priority>
                        <properties>
                            <property name="tags" type="String" value="jpinpoint-rule,memory,sustainability-medium" description="tags"/>
                            <property name="xpath"><value><![CDATA[//ClassDeclaration[false()]]]></value></property>
                        </properties>
                    </rule>
                    <rule name="Untagged" language="java" message="untagged"
                          class="net.sourceforge.pmd.lang.rule.xpath.XPathRule">
                        <description>untagged</description>
                        <priority>3</priority>
                        <properties>
                            <property name="xpath"><value><![CDATA[//ClassDeclaration[false()]]]></value></property>
                        </properties>
                    </rule>
                </ruleset>
                """);
        try {
            var ruleSet = PMDResultCollector.loadRuleSet(xml.toString());
            Rule tagged = ruleSet.getRuleByName("Tagged");
            Rule untagged = ruleSet.getRuleByName("Untagged");
            assertEquals(Optional.of(SustainabilityLevel.MEDIUM), SustainabilityLevel.of(tagged));
            assertEquals(Optional.empty(), SustainabilityLevel.of(untagged));
        } finally {
            Files.deleteIfExists(xml);
        }
    }
}
