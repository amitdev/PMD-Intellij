package com.intellij.plugins.bodhi.pmd.filter;

import com.intellij.openapi.vfs.*;

class NotFilter implements VirtualFileFilter
{
    private final VirtualFileFilter filter;

    NotFilter(VirtualFileFilter filter)
    {
        this.filter = filter;
    }

    @Override
    public boolean accept(VirtualFile file)
    {
        return !filter.accept(file);
    }
}
