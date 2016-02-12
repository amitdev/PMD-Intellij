package com.intellij.plugins.bodhi.pmd.filter;

import com.intellij.openapi.vfs.*;

class FileHasExtensionFilter implements VirtualFileFilter
{
    private final String extension;

    FileHasExtensionFilter(String extension)
    {
        this.extension = extension;
    }

    @Override
    public boolean accept(VirtualFile file)
    {
        return !file.isDirectory() && file.getPresentableUrl().endsWith("." + extension);
    }
}
