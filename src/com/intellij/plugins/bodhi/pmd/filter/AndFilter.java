package com.intellij.plugins.bodhi.pmd.filter;

import com.intellij.openapi.vfs.*;

class AndFilter implements VirtualFileFilter
{
    private final VirtualFileFilter[] filters;

    AndFilter(VirtualFileFilter... filters)
    {
        this.filters = filters;
    }


    @Override
    public boolean accept(VirtualFile file)
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
