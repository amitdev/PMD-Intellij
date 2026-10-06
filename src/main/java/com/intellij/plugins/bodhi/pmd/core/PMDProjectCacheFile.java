package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The PMD analysis cache file of a project. PMD does not invalidate its cache when class files in a
 * directory on the auxClasspath change, so the cache file is deleted when the output stamp changes.
 *
 * @see AuxClasspath#getOutputStamp()
 */
public final class PMDProjectCacheFile {
    private static final Map<Project, PMDProjectCacheFile> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private final Path file;
    private Long outputStamp;

    PMDProjectCacheFile(Path file) {
        this.file = file;
    }

    public static String getOrCreate(Project project, long outputStamp) {
        return CACHE.computeIfAbsent(project, p -> {
            try {
                return new PMDProjectCacheFile(Files.createTempFile("pmd-intellij-cache", ".cache").toAbsolutePath());
            } catch (IOException ioex) {
                throw new UncheckedIOException(ioex);
            }
        }).pathFor(outputStamp);
    }

    synchronized String pathFor(long outputStamp) {
        if (this.outputStamp != null && this.outputStamp != outputStamp) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException ioex) {
                throw new UncheckedIOException(ioex);
            }
        }
        this.outputStamp = outputStamp;
        return file.toString();
    }
}
