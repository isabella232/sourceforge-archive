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
    
    /* ------------------------------------------------------------ */
    protected ServletLoader()
	throws IOException
    {}
    
    
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
	    c=loadServletClass(name);
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
    protected Class loadServletClass(String name)
    {
	return null;
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

	InputStream in= getServletResourceAsStream(filename);
	
	if (in==null)
	    in=ClassLoader.getSystemResourceAsStream(filename);
	
	return in;
    }
    

    /* ------------------------------------------------------------ */
    /** Load a resource from the servlet class loader.
     * This method is used by by getResourceAsStream to load resources
     * from the servlet class path and should be specialized by
     * derived ServletLoaders.
     * @param filename The filename of the resource
     * @return An InputStream to the resource or null
     */
    public InputStream getServletResourceAsStream(String filename)
    {
	return null;
    }

    
    /* ------------------------------------------------------------ */
    /** Return true a class is modified.
     * Calls isServletModified().
     * @return true if any of the classes loaded by this loader have been
     * modified since their load time.
     */
    final public boolean isModified()
    {	
	return isServletModified();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Must be implmented by derived loaders.
     * @return 
     */
    public boolean isServletModified()
    {
	return false;
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
    /** Get a resource.
     * Not implemented.
     * @param filename 
     * @return 
     */
    public java.net.URL getResource(String filename)
    {
	Code.debug("Load resource ",filename);
	Code.notImplemented();
        return null;
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
