// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.Code;
import java.net.URL;
import java.net.URLClassLoader;

/* ------------------------------------------------------------ */
/**
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ContextLoader extends URLClassLoader
{
    String _path;

    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param servletClassPath Coma separated path of filenames or URLs
     * pointing to directories or jar files. Directories should end
     * with '/'.
     * @param quiet If true, non existant paths are not reported
     * @exception IOException
     */
    public ContextLoader(String path,
                         URL[] urls,
                         ClassLoader parent)
    {
        super(urls,parent);
        _path=path;
    }


    /* ------------------------------------------------------------ */
    protected Class findClass(String name)
        throws ClassNotFoundException
    {
        if (Code.verbose())
            Code.debug("findClass(",name,") from ",_path);
        return super.findClass(name);
    }

    /* ------------------------------------------------------------ */
    protected synchronized Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        if (Code.verbose())
            Code.debug("loadClass(",name,","+resolve,") from ",_path);
        return super.loadClass(name,resolve);
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "com.mortbay.HTTP.ContextLoader("+
            _path+") / "+getParent();
    }
}


