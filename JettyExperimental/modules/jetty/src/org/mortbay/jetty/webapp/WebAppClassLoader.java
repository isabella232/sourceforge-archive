// ========================================================================
// $Id$
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.webapp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.StringTokenizer;

import org.mortbay.io.IO;
import org.mortbay.log.Log;
import org.mortbay.resource.Resource;


/* ------------------------------------------------------------ */
/** ClassLoader for HttpContext.
 * Specializes URLClassLoader with some utility and file mapping
 * methods.
 *
 * This loader defaults to the 2.3 servlet spec behaviour where non
 * system classes are loaded from the classpath in preference to the
 * parent loader.  Java2 compliant loading, where the parent loader
 * always has priority, can be selected with the setJava2Complient method.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class WebAppClassLoader extends URLClassLoader
{

    private boolean _parentLoaderPriority= true;
    private ClassLoader _parent;
    private PermissionCollection _permissions;
    private String _urlClassPath;
    private String[] _systemClasses;
    private String[] _serverClasses;
    private File _tmpdir;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     */
    public WebAppClassLoader(ClassLoader parent)
    {
        super(new URL[0], parent);
        _parent=parent;
        if (parent==null)
            throw new IllegalArgumentException("no parent classloader!");
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the java2compliant.
     */
    public boolean isParentLoaderPriority()
    {
        return _parentLoaderPriority;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param java2compliant The java2compliant to set.
     */
    public void setParentLoaderPriority(boolean java2compliant)
    {
        _parentLoaderPriority = java2compliant;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the permissions.
     */
    public PermissionCollection getPermissions()
    {
        return _permissions;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param permissions The permissions to set.
     */
    public void setPermissions(PermissionCollection permissions)
    {
        _permissions = permissions;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the urlClassPath.
     */
    public String getUrlClassPath()
    {
        return _urlClassPath;
    }

    /* ------------------------------------------------------------ */
    public void setTempDirectory(File tmpdir)
    {
        if (_tmpdir!=null || _urlClassPath!=null)
            throw new IllegalStateException();
        _tmpdir=tmpdir;
    }
    
    /* ------------------------------------------------------------ */
    public File getTempDirectory()
    {
        if (_tmpdir!=null)
            return _tmpdir;
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param classPath Comma or semicolon separated path of filenames or URLs
     * pointing to directories or jar files. Directories should end
     * with '/'.
     */
    public void addClassPath(String classPath)
    	throws IOException
    {
        if (classPath == null)
            return;
            
        StringTokenizer tokenizer= new StringTokenizer(classPath, ",;");
        while (tokenizer.hasMoreTokens())
        {
            Resource resource= Resource.newResource(tokenizer.nextToken());
            if (Log.isDebugEnabled())
                Log.debug("Path resource=" + resource);

            // Resolve file path if possible
            File file= resource.getFile();
            if (file != null)
            {
                URL url= resource.getURL();
                addURL(url);
                _urlClassPath= (_urlClassPath == null) ? url.toString() : (_urlClassPath + "," + url.toString());
            }
            else
            {
                // Add resource or expand jar/
                if (!resource.isDirectory() && file == null)
                {
                    InputStream in= resource.getInputStream();
                    if (_tmpdir==null)
                    {
                        _tmpdir = File.createTempFile("jetty.cl.lib",null);
                        _tmpdir.mkdir();
                        _tmpdir.deleteOnExit();
                    }
                    File lib= new File(_tmpdir, "lib");
                    if (!lib.exists())
                    {
                        lib.mkdir();
                        lib.deleteOnExit();
                    }
                    File jar= File.createTempFile("Jetty-", ".jar", lib);
                    
                    jar.deleteOnExit();
                    if (Log.isDebugEnabled())
                        Log.debug("Extract " + resource + " to " + jar);
                    FileOutputStream out = null;
                    try
                    {
                        out= new FileOutputStream(jar);
                        IO.copy(in, out);
                    }
                    finally
                    {
                        IO.close(out);
                    }
                    
                    URL url= jar.toURL();
                    addURL(url);
                    _urlClassPath=
                        (_urlClassPath == null) ? url.toString() : (_urlClassPath + "," + url.toString());
                }
                else
                {
                    URL url= resource.getURL();
                    addURL(url);
                    _urlClassPath=
                        (_urlClassPath == null) ? url.toString() : (_urlClassPath + "," + url.toString());
                }
            }
        }
    }
    /* ------------------------------------------------------------ */
    /** Add elements to the class path for the context from the jar and zip files found
     *  in the specified resource.
     * @param lib the resource that contains the jar and/or zip files.
     * @param append true if the classpath entries are to be appended to any
     * existing classpath, or false if they replace the existing classpath.
     * @see #setClassPath(String)
     */
    public void addJars(Resource lib)
    {
        if (lib.exists() && lib.isDirectory())
        {
            String[] files=lib.list();
            for (int f=0;files!=null && f<files.length;f++)
            {
                try {
                    Resource fn=lib.addPath(files[f]);
                    String fnlc=fn.getName().toLowerCase();
                    if (fnlc.endsWith(".jar") || fnlc.endsWith(".zip"))
                    {
                        addClassPath(fn.toString());
                    }
                }
                catch (Exception ex)
                {
                    Log.warn(Log.EXCEPTION,ex);
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Set Java2 compliant status.
     * @param compliant 
     */
    public void setJava2Compliant(boolean compliant)
    {
        _parentLoaderPriority= compliant;
    }

    /* ------------------------------------------------------------ */
    public boolean isJava2Compliant()
    {
        return _parentLoaderPriority;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (Log.isDebugEnabled())
            return "ContextLoader@" + hashCode() + "(" + _urlClassPath + ") / " + _parent;
        return "ContextLoader@" + hashCode();
    }

    /* ------------------------------------------------------------ */
    public PermissionCollection getPermissions(CodeSource cs)
    {
        PermissionCollection pc= (_permissions == null) ? super.getPermissions(cs) : _permissions;
        if (Log.isDebugEnabled())
            Log.debug("loader.getPermissions(" + cs + ")=" + pc);
        return pc;
    }

    /* ------------------------------------------------------------ */
    public synchronized Class loadClass(String name) throws ClassNotFoundException
    {
        return loadClass(name, false);
    }

    /* ------------------------------------------------------------ */
    protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class c= findLoadedClass(name);
        ClassNotFoundException ex= null;
        boolean tried_parent= false;
        if (c == null && (_parentLoaderPriority || isSystemPath(name)) && !isServerPath(name) && _parent!=null)
        {
            if (Log.isDebugEnabled())
                Log.debug("try loadClass " + name + " from " + _parent);
            tried_parent= true;
            try
            {
                c= _parent.loadClass(name);
                if (Log.isDebugEnabled())
                    Log.debug("loaded " + c);
            }
            catch (ClassNotFoundException e)
            {
                ex= e;
            }
        }

        if (c == null)
        {
            if (Log.isDebugEnabled())
                Log.debug("try findClass " + name + " from " + _urlClassPath);
            try
            {
                c= this.findClass(name);
                if (Log.isDebugEnabled())
                    Log.debug("loaded " + c);
            }
            catch (ClassNotFoundException e)
            {
                ex= e;
            }
        }

        if (c == null && !tried_parent && !isServerPath(name) && _parent!=null)
        {
            if (Log.isDebugEnabled())
                Log.debug("try loadClass " + name + " from " + _parent);
            c= _parent.loadClass(name);
            if (Log.isDebugEnabled())
                Log.debug("loaded " + c);
        }

        if (c == null)
            throw ex;

        if (resolve)
            resolveClass(c);

        return c;
    }

    /* ------------------------------------------------------------ */
    public synchronized URL getResource(String name)
    {
        URL url= null;
        boolean tried_parent= false;
        if (_parentLoaderPriority || isSystemPath(name))
        {
            if (Log.isDebugEnabled())
                Log.debug("try getResource " + name + " from " + _parent);
            tried_parent= true;
            
            if (_parent!=null)
                url= _parent.getResource(name);
        }

        if (url == null)
        {
            if (Log.isDebugEnabled())
                Log.debug("try findResource " + name + " from " + _urlClassPath);
            url= this.findResource(name);

            if (url == null && name.startsWith("/"))
            {
                if (Log.isDebugEnabled())
                    Log.debug("HACK leading / off " + name);
                url= this.findResource(name.substring(1));
            }
        }

        if (url == null && !tried_parent)
        {
            if (Log.isDebugEnabled())
                Log.debug("try getResource " + name + " from " + _parent);
            if (_parent!=null)
                url= _parent.getResource(name);
        }

        if (url != null)
            if (Log.isDebugEnabled())
                Log.debug("found " + url);

        return url;
    }

    /* ------------------------------------------------------------ */
    public boolean isServerPath(String name)
    {
        name=name.replace('/','.');
        while(name.startsWith("."))
            name=name.substring(1);

        if (_serverClasses!=null)
        {
            for (int i=0;i<_serverClasses.length;i++)
            {
                if (_serverClasses[i].startsWith("-"))
                {
                    if (name.equals(_serverClasses[i].substring(1)))
                        return false;
                }
                else if (_serverClasses[i].endsWith("."))
                {
                    if (name.startsWith(_serverClasses[i]))
                        return true;
                }
                else if (name.equals(_serverClasses[i]))
                    return true;
            }
            return false;
        }

        
        // Arbitrary list that covers the worst security problems.
        // If you are worried by this, then use a permissions file!
        if (name.equals("org.mortbay.jetty.servlet.DefaultServlet") ||
                        name.startsWith("org.mortbay.util."))
                        return false;
        return name.startsWith("org.mortbay.jetty.");
    }

    /* ------------------------------------------------------------ */
    public boolean isSystemPath(String name)
    {
        name=name.replace('/','.');
        while(name.startsWith("."))
            name=name.substring(1);
        
        if (_systemClasses!=null)
        {
            for (int i=0;i<_systemClasses.length;i++)
            {
                if (_systemClasses[i].startsWith("-"))
                {
                    if (name.equals(_systemClasses[i].substring(1)))
                        return false;
                }
                else if (_systemClasses[i].endsWith("."))
                {
                    if (name.startsWith(_systemClasses[i]))
                        return true;
                }
                else if (name.equals(_systemClasses[i]))
                    return true;
            }
            return false;
        }
        
        // guessing a list
        return (
                   name.startsWith("java.")
                || name.startsWith("javax.servlet.")
                || name.startsWith("javax.xml.")
                || name.startsWith("org.mortbay.")
                || name.startsWith("org.xml.")
                || name.startsWith("org.w3c."));
    }

    /* ------------------------------------------------------------ */
    public void destroy()
    {
        this._parent=null;
        this._permissions=null;
        this._urlClassPath=null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the serverClasses.
     */
    String[] getServerClasses()
    {
        return _serverClasses;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param serverClasses The serverClasses to set.
     */
    public void setServerClasses(String[] serverClasses) // TODO - this shouldn't really be public...
    {
        _serverClasses = serverClasses==null?null:(String[])serverClasses.clone();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the systemClasses.
     */
    String[] getSystemClasses()
    {
        return _systemClasses;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param systemClasses The systemClasses to set.
     */
    void setSystemClasses(String[] systemClasses)
    {
        _systemClasses = systemClasses==null?null:(String[])systemClasses.clone();
    }
}
