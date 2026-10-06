package com.intellij.plugins.bodhi.pmd.core;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AuxClasspathTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path root;

    @Before
    public void setUp() {
        root = tmp.getRoot().toPath();
    }

    @Test
    public void keepsExistingJarsAndNonEmptyDirsInOrder() throws IOException {
        Path jar = createFile("libs/a.jar");
        Path classes = createFile("module/target/classes/p/A.class").getParent().getParent();

        AuxClasspath cp = AuxClasspath.build(List.of(classes.toString(), jar.toString()), null);

        assertEquals(List.of(classes.toString(), jar.toString()), cp.getEntries());
    }

    @Test
    public void removesDuplicates() throws IOException {
        Path jar = createFile("libs/a.jar");

        AuxClasspath cp = AuxClasspath.build(List.of(jar.toString(), jar.toString()), null);

        assertEquals(List.of(jar.toString()), cp.getEntries());
    }

    @Test
    public void skipsMissingJar() throws IOException {
        // PMD's prependAuxClasspath throws IllegalArgumentException on a missing jar
        Path jar = createFile("libs/a.jar");
        String missingJar = root.resolve("libs/missing.jar").toString();

        AuxClasspath cp = AuxClasspath.build(List.of(missingJar, jar.toString()), null);

        assertEquals(List.of(jar.toString()), cp.getEntries());
    }

    @Test
    public void skipsMissingAndEmptyDirs() throws IOException {
        String missingDir = root.resolve("module/target/classes").toString();
        Path emptyDir = Files.createDirectories(root.resolve("module/target/test-classes"));

        AuxClasspath cp = AuxClasspath.build(List.of(missingDir, emptyDir.toString()), null);

        assertEquals(List.of(), cp.getEntries());
    }

    @Test
    public void skipsJrtStyleSdkEntries() throws IOException {
        // IntelliJ renders JDK 9+ module roots like "/path/to/jdk!/java.base"
        Path jdk = createJdk11Plus("jdk");
        String jrtEntry = jdk + "!/java.base";

        AuxClasspath cp = AuxClasspath.build(List.of(jrtEntry), null);

        assertEquals(List.of(), cp.getEntries());
    }

    @Test
    public void addsJrtFsJarOfSdkHome() throws IOException {
        Path jar = createFile("libs/a.jar");
        Path jdk = createJdk11Plus("jdk");

        AuxClasspath cp = AuxClasspath.build(List.of(jar.toString()), jdk.toString());

        assertEquals(List.of(jar.toString(), jdk.resolve("lib/jrt-fs.jar").toString()), cp.getEntries());
    }

    @Test
    public void addsRtJarOfJdk8SdkHome() throws IOException {
        Path rtJar = createFile("jdk8/jre/lib/rt.jar");

        AuxClasspath cp = AuxClasspath.build(List.of(), root.resolve("jdk8").toString());

        assertEquals(List.of(rtJar.toString()), cp.getEntries());
    }

    @Test
    public void addsNoPlatformForUnknownSdkHome() throws IOException {
        Path notAJdk = Files.createDirectories(root.resolve("python"));

        AuxClasspath cp = AuxClasspath.build(List.of(), notAJdk.toString());

        assertEquals(List.of(), cp.getEntries());
    }

    @Test
    public void classpathStringUsesPathSeparator() throws IOException {
        Path a = createFile("libs/a.jar");
        Path b = createFile("libs/b.jar");

        AuxClasspath cp = AuxClasspath.build(List.of(a.toString(), b.toString()), null);

        assertEquals(a + File.pathSeparator + b, cp.getClasspath());
    }

    @Test
    public void stampIsStableWhenNothingChanges() throws IOException {
        Path classes = createFile("target/classes/p/A.class").getParent().getParent();
        List<String> entries = List.of(classes.toString());

        assertEquals(AuxClasspath.build(entries, null).getOutputStamp(),
                AuxClasspath.build(entries, null).getOutputStamp());
    }

    @Test
    public void stampChangesWhenOutputDirAppears() throws IOException {
        // PMD does not fingerprint directory contents for its analysis cache,
        // so a later build must be detected by us.
        List<String> entries = List.of(root.resolve("target/classes").toString());
        long before = AuxClasspath.build(entries, null).getOutputStamp();

        createFile("target/classes/p/A.class");

        assertNotEquals(before, AuxClasspath.build(entries, null).getOutputStamp());
    }

    @Test
    public void stampChangesWhenClassFileAdded() throws IOException {
        Path classes = createFile("target/classes/p/A.class").getParent().getParent();
        List<String> entries = List.of(classes.toString());
        long before = AuxClasspath.build(entries, null).getOutputStamp();

        createFile("target/classes/p/B.class");

        assertNotEquals(before, AuxClasspath.build(entries, null).getOutputStamp());
    }

    @Test
    public void stampChangesWhenClassFileRecompiled() throws IOException {
        Path classFile = createFile("target/classes/p/A.class");
        List<String> entries = List.of(classFile.getParent().getParent().toString());
        long before = AuxClasspath.build(entries, null).getOutputStamp();

        Files.setLastModifiedTime(classFile, FileTime.fromMillis(Files.getLastModifiedTime(classFile).toMillis() + 5000));

        assertNotEquals(before, AuxClasspath.build(entries, null).getOutputStamp());
    }

    @Test
    public void stampIgnoresJars() throws IOException {
        // jar contents are fingerprinted by PMD itself
        Path classes = createFile("target/classes/p/A.class").getParent().getParent();
        long without = AuxClasspath.build(List.of(classes.toString()), null).getOutputStamp();

        Path jar = createFile("libs/a.jar");
        long with = AuxClasspath.build(Arrays.asList(classes.toString(), jar.toString()), null).getOutputStamp();

        assertEquals(without, with);
    }

    @Test
    public void skipsJarThatIsNotAValidArchive() throws IOException {
        // issue #322: PMD's analysis cache fails on invalid archives with a ZipException
        Path jar = createFile("libs/a.jar");
        Path invalidJar = createFile("libs/invalid.jar", "not a zip");

        AuxClasspath cp = AuxClasspath.build(List.of(invalidJar.toString(), jar.toString()), null);

        assertEquals(List.of(jar.toString()), cp.getEntries());
    }

    @Test
    public void usesJarAgainWhenItBecomesValid() throws IOException {
        Path jar = createFile("libs/a.jar", "not a zip");
        assertEquals(List.of(), AuxClasspath.build(List.of(jar.toString()), null).getEntries());

        Files.delete(jar);
        createFile("libs/a.jar");
        Files.setLastModifiedTime(jar, FileTime.fromMillis(Files.getLastModifiedTime(jar).toMillis() + 5000));

        assertEquals(List.of(jar.toString()), AuxClasspath.build(List.of(jar.toString()), null).getEntries());
    }

    private Path createJdk11Plus(String name) throws IOException {
        createFile(name + "/lib/jrt-fs.jar");
        return root.resolve(name);
    }

    private Path createFile(String relative) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        if (relative.endsWith(".jar")) {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
                zip.putNextEntry(new ZipEntry("p/A.class"));
                zip.write(new byte[] {1});
                zip.closeEntry();
            }
            return file;
        }
        return Files.write(file, new byte[] {1});
    }

    private Path createFile(String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }
}
