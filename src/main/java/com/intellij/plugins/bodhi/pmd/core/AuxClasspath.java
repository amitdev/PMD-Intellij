package com.intellij.plugins.bodhi.pmd.core;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * The auxiliary classpath for PMD's type resolution, built from the classpath entries of the IDE.
 *
 * <p>Since PMD 7.27, PMD resolves classes only from the auxClasspath (no fallback to a parent classloader)
 * and warns about entries that do not exist. Therefore:</p>
 * <ul>
 *     <li>Entries that do not exist and empty directories are skipped (a missing jar would even make PMD fail).</li>
 *     <li>Archives that cannot be opened are skipped, as PMD's analysis cache fails on them (issue #322).
 *     Fixed in PMD 7.29.0 (pmd/pmd#7156).</li>
 *     <li>JDK module roots in IntelliJ notation (e.g. {@code /jdk!/java.base}) are skipped, instead the
 *     {@code lib/jrt-fs.jar} (or {@code rt.jar} for Java 8) of the given SDK home is added, so PMD resolves
 *     the JDK classes of the project and not of the IDE runtime.</li>
 *     <li>An output stamp is calculated over the directory entries, because PMD's analysis cache does not
 *     notice changes in directories (e.g. {@code target/classes} appearing after a build).</li>
 * </ul>
 */
public final class AuxClasspath {
    private static final Logger LOG = Logger.getInstance(AuxClasspath.class);

    /** Validity of archives, so that unchanged archives are not opened again for every run. */
    private static final Map<Path, ArchiveCheck> ARCHIVE_CHECKS = new ConcurrentHashMap<>();

    private final List<String> entries;
    private final long outputStamp;

    private AuxClasspath(List<String> entries, long outputStamp) {
        this.entries = List.copyOf(entries);
        this.outputStamp = outputStamp;
    }

    /**
     * @param rawEntries classpath entries as provided by the IDE, without the SDK
     * @param sdkHome home directory of the project SDK, or null if unknown
     */
    public static AuxClasspath build(@NotNull Collection<String> rawEntries, @Nullable String sdkHome) {
        Set<String> entries = new LinkedHashSet<>();
        long stamp = 1;
        for (String raw : new LinkedHashSet<>(rawEntries)) {
            Path path = toPath(raw);
            if (path == null) {
                continue;
            }
            if (!isArchive(path)) {
                stamp = 31 * stamp + stampOf(path);
            }
            if (isArchive(path) ? isValidArchive(path) : Files.isRegularFile(path) || isNonEmptyDirectory(path)) {
                entries.add(raw);
            }
        }
        Path platform = findPlatformClasspath(sdkHome);
        if (platform != null) {
            entries.add(platform.toString());
        }
        return new AuxClasspath(new ArrayList<>(entries), stamp);
    }

    public List<String> getEntries() {
        return entries;
    }

    public String getClasspath() {
        return String.join(File.pathSeparator, entries);
    }

    /**
     * A value that changes when class files in the directory entries change, appear or disappear.
     */
    public long getOutputStamp() {
        return outputStamp;
    }

    private static @Nullable Path toPath(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("!/") || raw.contains("!\\")) {
            return null; // e.g. a JDK module root inside the jrt filesystem
        }
        try {
            return Paths.get(raw);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static boolean isArchive(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jar") || name.endsWith(".zip");
    }

    // Workaround for PMD < 7.29.0, see pmd/pmd#7156 - remove after upgrading
    private static boolean isValidArchive(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try {
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            long size = Files.size(path);
            ArchiveCheck check = ARCHIVE_CHECKS.get(path);
            if (check == null || check.lastModified != lastModified || check.size != size) {
                check = new ArchiveCheck(lastModified, size, canOpenAsZip(path));
                ARCHIVE_CHECKS.put(path, check);
            }
            return check.valid;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean canOpenAsZip(Path path) {
        try (ZipFile ignored = new ZipFile(path.toFile())) {
            return true;
        } catch (IOException e) {
            LOG.warn("Skipping invalid archive on the auxClasspath: " + path + " (" + e.getMessage() + ")");
            return false;
        }
    }

    private static final class ArchiveCheck {
        private final long lastModified;
        private final long size;
        private final boolean valid;

        private ArchiveCheck(long lastModified, long size, boolean valid) {
            this.lastModified = lastModified;
            this.size = size;
            this.valid = valid;
        }
    }

    private static boolean isNonEmptyDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        try (Stream<Path> children = Files.list(path)) {
            return children.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    private static long stampOf(Path dir) {
        long stamp = dir.toString().hashCode();
        if (!Files.isDirectory(dir)) {
            return stamp;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            // order independent, the walk order is not guaranteed
            return stamp + files
                    .filter(f -> f.toString().endsWith(".class"))
                    .mapToLong(AuxClasspath::stampOfFile)
                    .sum();
        } catch (IOException | UncheckedIOException e) {
            return stamp + System.nanoTime(); // unknown state: rather invalidate the cache
        }
    }

    private static long stampOfFile(Path file) {
        try {
            long hash = file.toString().hashCode();
            return 31 * (31 * hash + Files.getLastModifiedTime(file).toMillis()) + Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static @Nullable Path findPlatformClasspath(@Nullable String sdkHome) {
        if (sdkHome == null || sdkHome.isBlank()) {
            return null;
        }
        Path home = Paths.get(sdkHome);
        for (Path candidate : List.of(
                home.resolve("lib").resolve("jrt-fs.jar"), // Java 9+
                home.resolve("jre").resolve("lib").resolve("rt.jar"), // Java 8 JDK
                home.resolve("lib").resolve("rt.jar"))) { // Java 8 JRE
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
