// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler.Servlet;

import java.util.*;
import com.mortbay.Util.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Servlet Class Loader.
 * 
 *
 * <h4>Notes</h4>
 * The load search order is:<NL>
 * <LI>Loader cache.
 * <LI>If the class starts with a systemClass package name,
 *     load from builtin loader or fail.
 * <LI>Try the loader path.
 * <LI>Try the builtin loader. Classes found here may not be reloaded.
 *
 * @version 1.0 Tue May  4 1999
 * @author Greg Wilkins (gregw)
 */
public class FileJarServletLoader extends ServletLoader
{
    /* ------------------------------------------------------------ */
    private HashMap _cache = new HashMap(37);
    private FileJarPath _classPath;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public FileJarServletLoader(String servletClassPath)
        throws IOException
    {
	this(servletClassPath,false);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public FileJarServletLoader(String servletClassPath,
				boolean quiet)
        throws IOException
    {
	_classPath=new FileJarPath(servletClassPath,quiet);
    }


    /* ------------------------------------------------------------ */
    /** Get a resource as a stream.
     * Return the raw input stream from a file in the classpath
     * @param filename The filename of the resource
     * @return An InputStream to the resource or null
     */
    public InputStream getServletResourceAsStream(String filename)
    {
	if (Code.verbose())Code.debug("Load resource as stream ",filename);
        return _classPath.getInputStream(filename);
    }
    
    
    /* ------------------------------------------------------------ */
    /** Load a servlet class
     * @param name 
     * @param resolve 
     * @return 
     * @exception ClassNotFoundException 
     */
    synchronized public Class loadServletClass(String name)
    {
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
	if (Code.verbose())Code.debug("Loaded servlet class ",c);
        return c;
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if any loaded element from the classpath has been
     * modified.
     */
    public boolean isServletModified()
    {
	return _classPath.isModified();
    }
}



