package com.intellij.plugins.bodhi.pmd.core;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.reporting.Report;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Verifies the behaviour of PMD (7.27+) that {@link AuxClasspath} works around.
 */
public class AuxClasspathPmdTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void pmdRejectsMissingJarOnAuxClasspath() {
        String missingJar = tmp.getRoot().toPath().resolve("missing.jar").toString();

        assertThrows(IllegalArgumentException.class, () -> new PMDConfiguration().prependAuxClasspath(missingJar));
    }

    @Test
    public void pmdAcceptsBuiltClasspathWithCurrentJdk() {
        String missingJar = tmp.getRoot().toPath().resolve("missing.jar").toString();
        String javaHome = System.getProperty("java.home");

        AuxClasspath cp = AuxClasspath.build(List.of(missingJar), javaHome);
        PMDConfiguration config = new PMDConfiguration();
        config.prependAuxClasspath(cp.getClasspath());

        assertEquals(List.of(Path.of(javaHome, "lib", "jrt-fs.jar").toString()), cp.getEntries());
        assertTrue(config.getAuxClasspath().endsWith("jrt-fs.jar"));
        assertTrue(!config.getAuxClasspath().contains(File.pathSeparator));
    }

    @Test
    public void pmdAnalysisCacheFailsOnInvalidJar() throws IOException {
        // issue #322: ZipFileFingerprinter does not handle invalid archives (PMD 7.28.0)
        Path invalidJar = Files.writeString(tmp.newFile("invalid.jar").toPath(), "not a zip");

        PMDConfiguration config = createConfigWithAnalysisCache();
        config.prependAuxClasspath(invalidJar.toString());

        assertThrows(RuntimeException.class, () -> runAnalysis(config));
    }

    @Test
    public void pmdAnalysisSucceedsWithBuiltClasspathAndInvalidJar() throws IOException {
        Path invalidJar = Files.writeString(tmp.newFile("invalid.jar").toPath(), "not a zip");

        PMDConfiguration config = createConfigWithAnalysisCache();
        AuxClasspath cp = AuxClasspath.build(List.of(invalidJar.toString()), System.getProperty("java.home"));
        config.prependAuxClasspath(cp.getClasspath());

        assertEquals(1, runAnalysis(config).getViolations().size());
    }

    private PMDConfiguration createConfigWithAnalysisCache() throws IOException {
        Path source = Files.writeString(tmp.newFile("Foo.java").toPath(),
                "public class Foo { private int unused; }");
        PMDConfiguration config = new PMDConfiguration();
        config.setAnalysisCacheLocation(tmp.getRoot().toPath().resolve("pmd.cache").toString());
        config.addRuleSet("category/java/bestpractices.xml/UnusedPrivateField");
        config.addInputPath(source);
        return config;
    }

    private static Report runAnalysis(PMDConfiguration config) {
        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            return pmd.performAnalysisAndCollectReport();
        }
    }
}
