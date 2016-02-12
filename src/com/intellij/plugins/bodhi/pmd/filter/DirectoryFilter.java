package com.intellij.plugins.bodhi.pmd.filter;

import com.intellij.openapi.vfs.*;

class DirectoryFilter implements VirtualFileFilter
{
    @Override
    public boolean accept(VirtualFile file)
    {
        return file.isDirectory();
    }
}
