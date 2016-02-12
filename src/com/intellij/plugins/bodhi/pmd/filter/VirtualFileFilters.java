package com.intellij.plugins.bodhi.pmd.filter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileFilter;

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
}
