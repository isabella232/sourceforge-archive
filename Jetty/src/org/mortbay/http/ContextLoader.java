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

package org.mortbay.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.IO;
import org.mortbay.util.LogSupport;
import org.mortbay.util.Resource;

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
public class ContextLoader extends URLClassLoader
{
    private static Log log= LogFactory.getLog(ContextLoader.class);

    private boolean _java2compliant= false;
    private ClassLoader _parent;
    private PermissionCollection _permissions;
    private String _urlClassPath;
    private boolean _fileClassPathWarning= false;

    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param classPath Comma separated path of filenames or URLs
     * pointing to directories or jar files. Directories should end
     * with '/'.
     * @exception IOException
     */
    public ContextLoader(HttpContext context, String classPath, ClassLoader parent, PermissionCollection permisions)
        throws MalformedURLException, IOException
    {
        super(new URL[0], parent);
        _permissions= permisions;
        _parent= parent;
        if (_parent == null)
            _parent= getSystemClassLoader();

        if (classPath == null)
        {
            _urlClassPath= "";
        }
        else
        {
            StringTokenizer tokenizer= new StringTokenizer(classPath, ",;");

            while (tokenizer.hasMoreTokens())
            {
                Resource resource= Resource.newResource(tokenizer.nextToken());
                if (log.isDebugEnabled())
                    log.debug("Path resource=" + resource);

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
                    _fileClassPathWarning= true;

                    // Add resource or expand jar/
                    if (!resource.isDirectory() && file == null)
                    {
                        InputStream in= resource.getInputStream();
                        File lib= new File(context.getTempDirectory(), "lib");
                        if (!lib.exists())
                        {
                            lib.mkdir();
                            lib.deleteOnExit();
                        }
                        File jar= File.createTempFile("Jetty-", ".jar", lib);

                        jar.deleteOnExit();
                        if (log.isDebugEnabled())
                            log.debug("Extract " + resource + " to " + jar);
                        FileOutputStream out= new FileOutputStream(jar);
                        IO.copy(in, out);
                        out.close();
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

        if (log.isDebugEnabled())
        {
            if (log.isDebugEnabled())
                log.debug("ClassPath=" + _urlClassPath);
            if (log.isDebugEnabled())
                log.debug("Permissions=" + _permissions);
            if (log.isDebugEnabled())
                log.debug("URL=" + Arrays.asList(getURLs()));
        }
    }

    /* ------------------------------------------------------------ */
    /** Set Java2 compliant status.
     * @param compliant 
     */
    public void setJava2Compliant(boolean compliant)
    {
        _java2compliant= compliant;
    }

    /* ------------------------------------------------------------ */
    public boolean isJava2Compliant()
    {
        return _java2compliant;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (log.isDebugEnabled())
            return "ContextLoader@" + hashCode() + "(" + _urlClassPath + ") / " + _parent.toString();
        return "ContextLoader@" + hashCode();
    }

    /* ------------------------------------------------------------ */
    public PermissionCollection getPermissions(CodeSource cs)
    {
        PermissionCollection pc= (_permissions == null) ? super.getPermissions(cs) : _permissions;
        if (log.isDebugEnabled())
            log.debug("loader.getPermissions(" + cs + ")=" + pc);
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
        if (c == null && (_java2compliant || isSystemPath(name)) && !isHiddenPath(name))
        {
            if (LogSupport.isTraceEnabled(log))
                log.trace("try loadClass " + name + " from " + _parent);
            tried_parent= true;
            try
            {
                c= _parent.loadClass(name);
                if (LogSupport.isTraceEnabled(log))
                    log.trace("loaded " + c);
            }
            catch (ClassNotFoundException e)
            {
                ex= e;
            }
        }

        if (c == null)
        {
            if (LogSupport.isTraceEnabled(log))
                log.trace("try findClass " + name + " from " + _urlClassPath);
            try
            {
                c= this.findClass(name);
                if (LogSupport.isTraceEnabled(log))
                    log.trace("loaded " + c);
            }
            catch (ClassNotFoundException e)
            {
                ex= e;
            }
        }

        if (c == null && !tried_parent && !isHiddenPath(name))
        {
            if (LogSupport.isTraceEnabled(log))
                log.trace("try loadClass " + name + " from " + _parent);
            c= _parent.loadClass(name);
            if (LogSupport.isTraceEnabled(log))
                log.trace("loaded " + c);
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
        if (_java2compliant || isSystemPath(name))
        {
            if (LogSupport.isTraceEnabled(log))
                log.trace("try getResource " + name + " from " + _parent);
            tried_parent= true;
            url= _parent.getResource(name);
        }

        if (url == null)
        {
            if (LogSupport.isTraceEnabled(log))
                log.trace("try findResource " + name + " from " + _urlClassPath);
            url= this.findResource(name);

            if (url == null && name.startsWith("/"))
            {
                if (log.isDebugEnabled())
                    log.debug("HACK leading / off " + name);
                url= this.findResource(name.substring(1));
            }
        }

        if (url == null && !tried_parent)
        {
            if (LogSupport.isTraceEnabled(log))
                log.trace("try getResource " + name + " from " + _parent);
            url= _parent.getResource(name);
        }

        if (url != null)
            if (LogSupport.isTraceEnabled(log))
                log.trace("found " + url);

        return url;
    }

    /* ------------------------------------------------------------ */
    public boolean isHiddenPath(String name)
    {
        // Arbitrary list that covers the worst security problems.
        // If you are worried by this, then use a permissions file!
        return name.equals("org.mortbay.jetty.Server")
            || name.equals("org.mortbay.http.HttpServer")
            || name.startsWith("org.mortbay.start.")
            || name.startsWith("org.mortbay.stop.");
    }

    /* ------------------------------------------------------------ */
    public boolean isSystemPath(String name)
    {
        return (
            name.startsWith("java.")
                || name.startsWith("javax.servlet.")
                || name.startsWith("javax.xml.")
                || name.startsWith("org.mortbay.")
                || name.startsWith("org.xml.")
                || name.startsWith("org.w3c.")
                || name.startsWith("java/")
                || name.startsWith("javax/servlet/")
                || name.startsWith("javax/xml/")
                || name.startsWith("org/mortbay/")
                || name.startsWith("org/xml/")
                || name.startsWith("org/w3c/")
                || name.startsWith("/java/")
                || name.startsWith("/javax/servlet/")
                || name.startsWith("/javax/xml/")
                || name.startsWith("/org/mortbay/")
                || name.startsWith("/org/xml/")
                || name.startsWith("/org/w3c/"));
    }
}
