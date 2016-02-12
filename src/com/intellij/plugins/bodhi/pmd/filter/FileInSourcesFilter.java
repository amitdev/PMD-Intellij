package com.intellij.plugins.bodhi.pmd.filter;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.*;

class FileInSourcesFilter implements VirtualFileFilter
{
    private final Project project;

    FileInSourcesFilter(Project project)
    {
        this.project = project;
    }

    @Override
    public boolean accept(VirtualFile file)
    {
        return ProjectRootManager.getInstance(project).getFileIndex().isInSource(file);
    }
}
