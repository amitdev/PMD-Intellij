package com.intellij.plugins.bodhi.pmd.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PMDProjectCacheFileTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void keepsCacheWhenStampUnchanged() throws IOException {
        Path file = tmp.newFile("pmd.cache").toPath();
        PMDProjectCacheFile cacheFile = new PMDProjectCacheFile(file);

        assertEquals(file.toString(), cacheFile.pathFor(1L));
        assertEquals(file.toString(), cacheFile.pathFor(1L));

        assertTrue(Files.exists(file));
    }

    @Test
    public void deletesCacheWhenStampChanges() throws IOException {
        Path file = tmp.newFile("pmd.cache").toPath();
        PMDProjectCacheFile cacheFile = new PMDProjectCacheFile(file);
        cacheFile.pathFor(1L);

        assertEquals(file.toString(), cacheFile.pathFor(2L));

        assertFalse(Files.exists(file));
    }

    @Test
    public void keepsCacheOnFirstUse() throws IOException {
        Path file = tmp.newFile("pmd.cache").toPath();

        new PMDProjectCacheFile(file).pathFor(1L);

        assertTrue(Files.exists(file));
    }
}
