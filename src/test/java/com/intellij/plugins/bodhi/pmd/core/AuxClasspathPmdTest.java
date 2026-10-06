package com.intellij.plugins.bodhi.pmd.core;

import net.sourceforge.pmd.PMDConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
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
}
