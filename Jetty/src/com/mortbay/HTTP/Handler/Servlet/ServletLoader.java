// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler.Servlet;
//import com.sun.java.util.collections.*; XXX-JDK1.1

import com.mortbay.Util.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.net.*;

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
    private HashMap _cache = new HashMap(37);
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
	Code.debug("new ServletLoader(",servletClassPath,")");
	if (servletClassPath!=null && servletClassPath.length()>0)
	    _classPath=new ResourcePath(servletClassPath,quiet);
    }

    
    /* ------------------------------------------------------------ */
    /** Load a class.
     * Classes from the "javax.servlet." packages are loaded from the
     * builtin ZipResource. Other classes are loaded from a call to
     * loadServletClass.
     * @param name Class name (without ".class");
     * @return The Class instance
     * @exception ClassNotFoundException 
     */
    final public Class loadClass(String name)
	throws ClassNotFoundException
    {
	try
	{
	    // look in the cache
	    Class c=(Class)_cache.get(name);
	    if (c!=null)
		return c;
	
	    // If it is a system class, it must be loaded from the
	    // base loader
	    if (isSystemClass(name))
		return findSystemClass(name);    
	    
	    // try loading from the handler class path
	    c=loadFromClassPath(name);
	    if (c!=null)
	    {
		if (Code.verbose())Code.debug("loaded  ",name);
		_cache.put(name,c);
		return c;
	    }

	    return findSystemClass(name);
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
    /** Load a class from the servlet handler classpath.
     * This method is used by by loadClass to load classes
     * from the servlet class path and should be specialized by
     * derived ServletLoaders.
     * @param name Class name (without ".class");
     * @return the class or null if not found.
     */
    protected Class loadFromClassPath(String name)
    {
	if (_classPath==null)
	    return null;
	
        Class c;        
        //remove the .class
        if (name.endsWith(".class"))
            name = name.substring(0,name.lastIndexOf('.'));
        
        // Look for a cached class
        c=(Class)_cache.get(name);
        if (c!=null)
            return c;

	// get the package name
	String packageName="";
	if (name.indexOf(".")>=0)
	    packageName=name.substring(0,name.lastIndexOf('.'));
	
	// Check permission
	SecurityManager sm=System.getSecurityManager();
	if (sm!=null)
	    sm.checkPackageAccess(packageName);
	
	// Load & define
        String filename = name.replace('.',File.separatorChar)+".class";
        InputStream in=_classPath.getInputStream(filename);
        if (in!=null)
        {
            try{
                ByteArrayOutputStream out =
                    new ByteArrayOutputStream(8192);
                com.mortbay.Util.IO.copy(in,out);
                
                byte data[] = out.toByteArray();
                c= defineClass(name,data,0,data.length);
		_cache.put(name,c);
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

	if (Code.verbose())Code.debug("Loaded ",c," for ",name);
        return c;
    }
    
    /* ------------------------------------------------------------ */
    /** Get a resource as a stream.
     * Return the raw input stream from a file in the classpath
     * @param filename The filename of the resource
     * @return An InputStream to the resource or null
     */
    final public InputStream getResourceAsStream(String filename)
    {
	if (Code.verbose()) Code.debug("Load resource as stream ",filename);

	InputStream in=null;
	if (_classPath!=null)
	    in=_classPath.getInputStream(filename);
	if (in==null)
	    in=ClassLoader.getSystemResourceAsStream(filename);
	
	return in;
    }

    /* ------------------------------------------------------------ */
    /** Get a resource.
     * Not implemented.
     * @param filename 
     * @return 
     */
    public URL getResource(String filename)
    {
	Code.debug("Load resource ",filename);
	URL url=null;
	if (_classPath!=null)
	{
	    Resource resource=_classPath.getResource(filename);
	    if (resource!=null)
		url=resource.getURL();
	}
	if (url==null)
	    url=ClassLoader.getSystemResource(filename);
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
    
    
    /* ------------------------------------------------------------ */
    /** Load a class.
     * @param name Class name (without ".class");
     * @param resolve True if the class should be resolved when loaded. 
     * @return The Class instance
     * @exception ClassNotFoundException 
     */
    public Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        Class c = loadClass(name);
        
        // resolve
        if (resolve)
            resolveClass(c);
        return c;
    }

    
    
    /* ------------------------------------------------------------ */
    /** Classes from packages starting with these strings can only
     * be loaded from the builtin loader.
     */
    private final static String[] __systemClasses =
    {
        "java.",
	"javax.",
	"com.mortbay.Util.",
	"com.mortbay.HTTP.",
	"com.mortbay.HTML.",
	"com.mortbay.FTP.",
    };
      

    /* ------------------------------------------------------------ */
    /* Is the class a system class.
     * Check if the class is from java, javax or the com.mortbay
     * packages.
     * @param name The name of the class or package.
     * @return true if the class name is in a system package.
     */
    private static boolean isSystemClass(String name)
    {
        for (int i=__systemClasses.length;i-->0;)
            if (name.startsWith(__systemClasses[i]))
                return true;
        return false;
    }

}
