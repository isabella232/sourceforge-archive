// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler.Servlet;

import com.mortbay.Util.Code;
import com.mortbay.Util.IO;
import com.mortbay.Util.Resource;
import com.mortbay.Util.ResourcePath;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

/* ------------------------------------------------------------ */
/** Servlet Class Loader.
 *
 * <h4>Notes</h4>
 * The load search order is:<NL>
 * <LI>Loader cache.
 * <LI>If the class starts with a systemClass package name,
 *     load from builtin loader or fail.
 * <LI>Try the loader path.
 * <LI>Try the builtin loader. Classes found here may not be reloaded.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ServletLoader extends ClassLoader
{
    private ResourcePath _classPath;

    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param servletClassPath Coma separated path of filenames or URLs
     * pointing to directories or jar files. Directories should end
     * with '/'.
     */
    protected ServletLoader()
        throws IOException
    {
        this(null,false);
    }

    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param servletClassPath Coma separated path of filenames or URLs
     * pointing to directories or jar files. Directories should end
     * with '/'.
     */
    protected ServletLoader(String servletClassPath)
        throws IOException
    {
        this(servletClassPath,false);
    }


    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param servletClassPath Coma separated path of filenames or URLs
     * pointing to directories or jar files. Directories should end
     * with '/'.
     * @param quiet If true, non existant paths are not reported
     * @exception IOException
     */
    public ServletLoader(String servletClassPath,
                         boolean quiet)
        throws IOException
    {
        super(ServletLoader.class.getClassLoader());
        Code.debug("new ServletLoader(",servletClassPath,")");
        if (servletClassPath!=null && servletClassPath.length()>0)
            _classPath=new ResourcePath(servletClassPath,quiet);
    }


    /* ------------------------------------------------------------ */
    /** Find and define a class.
     * Classes from the "javax.servlet." and  "com.mortbay." packages are loaded
     * only from the system class loader. Other classes are loaded using
     * loadServletClass.
     * @param name Class name (without ".class");
     * @return The Class instance
     * @exception ClassNotFoundException
     */
    protected Class findClass(String name)
        throws ClassNotFoundException
    {
        try
        {
            // try loading from the handler class path
            byte data[] =loadFromClassPath(name);
            if (data != null)
            {
                Class c = defineClass(name,data,0,data.length);
                if (Code.verbose())Code.debug("Loaded ",c," for ",name);
                return c;
            }
            return super.findClass(name);
        }
        catch(ClassNotFoundException e)
        {
          throw e;
        }
        catch(Throwable e)
        {
            Code.warning(e);
            throw new ClassNotFoundException(name);
        }
    }

    /* ------------------------------------------------------------ */
    /** Load a class.
     * This method is not compliant with the Java2 spec. It is here only
     * to avoid servlets being loaded from the system classpath. To obtain
     * compliance, simply remove this method.
     * @param name Class name (without ".class");
     * @param resolve True if the class should be resolved when loaded.
     * @return The Class instance
     * @exception ClassNotFoundException
     */
    protected synchronized Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
    // First, check if the class has already been loaded
    Class c = findLoadedClass(name);
    if (c == null)
    {
        try
        {
            c = findClass(name);
        }
        catch (ClassNotFoundException e)
        {
            if (getParent() != null)
                c = getParent().loadClass(name);
            else
                c = findSystemClass(name);
        }
    }
    if (resolve) {
        resolveClass(c);
    }
    return c;
    }


    /* ------------------------------------------------------------ */
    /** Load a class from the servlet handler classpath.
     * This method is used by by findClass to load classes
     * from the servlet class path and should be specialized by
     * derived ServletLoaders.
     * @param name Class name (without ".class");
     * @return the A byte array with the class data.
     */
    protected byte[] loadFromClassPath(String name)
    {
        if (_classPath==null)
            return null;

        //remove the .class
        if (name.endsWith(".class"))
            name = name.substring(0,name.lastIndexOf('.'));

        // get the package name
        String packageName="";
        if (name.indexOf(".")>=0)
            packageName=name.substring(0,name.lastIndexOf('.'));

        // Check permission
        SecurityManager sm=System.getSecurityManager();
        if (sm!=null)
            sm.checkPackageAccess(packageName);

        // Load the class data
        String filename = name.replace('.',File.separatorChar)+".class";
        InputStream in=_classPath.getInputStream(filename);
        if (in!=null)
        {
            try{
                ByteArrayOutputStream out =
                    new ByteArrayOutputStream(8192);
                com.mortbay.Util.IO.copy(in,out);
                if (Code.verbose())Code.debug("Loaded ",name);
                return out.toByteArray();
            }
            catch(Exception e)
            {
                Code.ignore(e);
            }
            finally
            {
                try{in.close();}
                catch(Exception e){Code.ignore(e);}
            }
        }

        return null;
    }


    /* ------------------------------------------------------------ */
    /** Get a resource.
     * Not implemented.
     * @param filename
     * @return
     */
    protected URL findResource(String filename)
    {
    Code.debug("Load resource ",filename);
    URL url=null;
    if (_classPath!=null)
    {
        Resource resource=_classPath.getResource(filename);
        if (resource!=null)
        url=resource.getURL();
    }
    return url;
    }


    /* ------------------------------------------------------------ */
    /** Return true a class is modified.
     * Calls isServletModified().
     * @return true if any of the classes loaded by this loader have been
     * modified since their load time.
     */
    public boolean isModified()
    {
        return _classPath.isModified();
    }

}
