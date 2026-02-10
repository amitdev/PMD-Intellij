package com.intellij.plugins.bodhi.pmd.filter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class VirtualFileFilters
{
    public static VirtualFileFilter and(VirtualFileFilter... filters)
    {
        return new AndFilter(filters);
    }

    public static VirtualFileFilter or(VirtualFileFilter... filters)
    {
        return new OrFilter(filters);
    }

    public static VirtualFileFilter not(VirtualFileFilter filter)
    {
        return new NotFilter(filter);
    }

    public static VirtualFileFilter fileHasExtension(String extension)
    {
        return new FileHasExtensionFilter(extension);
    }

    public static VirtualFileFilter isDirectory()
    {
        return new DirectoryFilter();
    }

    public static VirtualFileFilter fileInTestSources(Project project)
    {
        return new FileInTestSourcesFilter(project);
    }

    public static VirtualFileFilter fileInSources(Project project)
    {
        return new FileInSourcesFilter(project);
    }

    public static VirtualFileFilter fileNotInExcludeRoots(Set<String> excludeRoots)
    {
        return new FileNotInExcludeRootsFilter(excludeRoots);
    }

    static class AndFilter implements VirtualFileFilter
    {
        private final VirtualFileFilter[] filters;

        AndFilter(VirtualFileFilter... filters)
        {
            this.filters = filters;
        }


        public boolean accept(@NotNull VirtualFile file)
        {
            for (VirtualFileFilter filter : filters)
            {
                if(!filter.accept(file))
                {
                    return false;
                }
            }
            return true;
        }
    }

    static class OrFilter implements VirtualFileFilter
    {
        private final VirtualFileFilter[] filters;

        OrFilter(VirtualFileFilter... filters)
        {
            this.filters = filters;
        }


        public boolean accept(@NotNull VirtualFile file)
        {
            for (VirtualFileFilter filter : filters)
            {
                if(filter.accept(file))
                {
                    return true;
                }
            }
            return false;
        }
    }

    static class DirectoryFilter implements VirtualFileFilter
    {
        public boolean accept(VirtualFile file)
        {
            return file.isDirectory();
        }
    }

    static class FileHasExtensionFilter implements VirtualFileFilter
    {
        private final String extension;

        FileHasExtensionFilter(String extension)
        {
            this.extension = extension;
        }

        public boolean accept(VirtualFile file)
        {
            return !file.isDirectory() && file.getPresentableUrl().endsWith("." + extension);
        }
    }

    static class FileInSourcesFilter implements VirtualFileFilter
    {
        private final Project project;

        FileInSourcesFilter(Project project)
        {
            this.project = project;
        }

        public boolean accept(@NotNull VirtualFile file)
        {
            return ProjectRootManager.getInstance(project).getFileIndex().isInSource(file);
        }
    }

    static class FileInTestSourcesFilter implements VirtualFileFilter
    {
        private final Project project;

        FileInTestSourcesFilter(Project project)
        {
            this.project = project;
        }

        public boolean accept(@NotNull VirtualFile file)
        {
            return ProjectRootManager.getInstance(project).getFileIndex().isInTestSourceContent(file);
        }
    }

    static class NotFilter implements VirtualFileFilter
    {
        private final VirtualFileFilter filter;

        NotFilter(VirtualFileFilter filter)
        {
            this.filter = filter;
        }

        public boolean accept(@NotNull VirtualFile file)
        {
            return !filter.accept(file);
        }
    }

    static class FileNotInExcludeRootsFilter implements VirtualFileFilter
    {
        private final Set<String> excludeRoots;

        FileNotInExcludeRootsFilter(Set<String> excludeRoots)
        {
            this.excludeRoots = excludeRoots;
        }

        public boolean accept(@NotNull VirtualFile file)
        {
            if (excludeRoots.isEmpty()) {
                return true;
            }


            String filePath = file.getPath();
            for (String excludeRoot : excludeRoots) {
                String normalizedExcludeRoot = excludeRoot.trim();
                if (normalizedExcludeRoot.isEmpty()) {
                    continue;
                }

                if (isAbsolutePath(normalizedExcludeRoot)) {
                    if (filePath.startsWith(normalizedExcludeRoot)) {
                        return false;
                    }
                } else {
                    if (matchesRelativePattern(filePath, normalizedExcludeRoot)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Checks if the file path matches a relative exclude pattern.
         * The pattern is normalized to /pattern/ and we check if the file path contains it.
         *  <br/>
         * For example, pattern "target/generated-sources" becomes "/target/generated-sources/"
         * and will match any path containing that segment:
         * - /project/target/generated-sources/Foo.java
         * - /project/module1/target/generated-sources/Bar.java
         */
        private boolean matchesRelativePattern(String filePath, String pattern) {
            return  normalizePath(filePath).contains(normalizePath(pattern));
        }

        private static String normalizePath(String path) {
            String normalizedPath = path;
            if (!normalizedPath.startsWith("/")) {
                normalizedPath = "/" + normalizedPath;
            }
            if (!normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath + "/";
            }
            return normalizedPath;
        }

        private static boolean isAbsolutePath(String path) {
            // Unix absolute path or Windows absolute path (e.g., C:\)
            return path.startsWith("/") || (path.length() > 2 && path.charAt(1) == ':');
        }
    }
}
