// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.Base.*;
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
public class ServletLoader extends ClassLoader
{
    /* ------------------------------------------------------------ */
    /** Classes from packages starting with these strings can only
     * be loaded from the builtin loader.
     */
    private final static String[] systemClasses =
    {
	"java.",
	"javax.",
	"com.mortbay."
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
	for (int i=systemClasses.length;i-->0;)
	    if (name.startsWith(systemClasses[i]))
		return true;
	return false;
    }

    /* ------------------------------------------------------------ */
    static class LoadedFile
    {
	File file;
	long timeLoaded;
    }
    
    /* ------------------------------------------------------------ */
    private Hashtable cache = new Hashtable();
    Vector paths=new Vector();
    Vector loaded=new Vector();
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public ServletLoader(String servletClassPath)
	throws IOException
    {
	StringTokenizer tokenizer =
	    new StringTokenizer(servletClassPath,File.pathSeparator);
	while (tokenizer.hasMoreTokens())
	{
	    String filename=tokenizer.nextToken();
	    File file=new File(filename);
	    try
	    {
		// see if the path element exists
		if (!file.exists())
		    throw new FileNotFoundException(filename);
		file = new File(file.getCanonicalPath());
		if (file.isDirectory())
		    // add to the list if it is a directory.
		    paths.addElement(file.getCanonicalPath());
		else
		{
		    // Assume it is a zip file
		    paths.addElement(new ZipFile(file));
		    LoadedFile lc=new LoadedFile();
		    lc.file=file;
		    lc.timeLoaded=System.currentTimeMillis();
		    loaded.addElement(lc);
		}
	    }
	    catch(IOException e)
	    {
		Code.warning("Problem with "+file,e);
	    }
	}
    }
    
    /* ------------------------------------------------------------ */
    /** 
     */
    private Class loadFromServletClassPath(String name)
    {
	String filename = name.replace('.',File.separatorChar)+".class";
	Code.debug("Load ",name);
	InputStream in=null;
	int length=0;
	
	// For each potential path
	for (int p=0;p<paths.size();p++)
	{
	    try
	    {
		Object source = paths.elementAt(p);
		if (source instanceof java.util.zip.ZipFile)
		{
		    ZipFile zip = (ZipFile)source;
		    ZipEntry entry = zip.getEntry(filename);
		    if (entry!=null)
		    {
			Code.debug("Loading ",entry," from ",zip.getName());
			in = zip.getInputStream(entry);
			length=(int)entry.getSize();
			break;
		    }
		}
		else
		{
		    String dir = (String)source;
		    File file = new File(dir+File.separator+filename);
		    if (file.exists())
		    {
			Code.debug("Loading ",file);
			in = new FileInputStream(file);
			length=(int)file.length();
			
			LoadedFile lc=new LoadedFile();
			lc.file=file;
			lc.timeLoaded=System.currentTimeMillis();
			loaded.addElement(lc);
			break;
		    }
		}
	    }
	    catch(Exception e)
	    {
		if (Code.verbose(9))
		    Code.ignore(e);
	    }
	}

	if (in!=null)
	{
	    try{
		ByteArrayOutputStream out =
		    new ByteArrayOutputStream(length);
		com.mortbay.Util.IO.copy(in,out);
		
		byte data[] = out.toByteArray();
		return defineClass(name,data,0,data.length);
	    }
	    catch(Exception e)
	    {
		if (Code.verbose(9))
		    Code.ignore(e);
	    }
	}
	
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /** Load a class
     * @param name 
     * @param resolve 
     * @return 
     * @exception ClassNotFoundException 
     */
    synchronized
    public Class loadClass(String name, boolean resolve)
	throws ClassNotFoundException
    {
	Class c;
	
	//remove the .class
	if (name.endsWith(".class"))
	    name = name.substring(0,name.lastIndexOf('.'));
	
	// Look for a cached class
	c=(Class)cache.get(name);
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
	
	// Look for a system class
	if (isSystemClass(name))
	    // If it is a system class it must be found here or fail
	    return findSystemClass(name);
	
	// Load & define
	c=loadFromServletClassPath(name);

	// Try the system class for additional CLASSPATH entries.
	// classes found here can't be reloaded.
	if (c==null)
	    return findSystemClass(name);
	
	// resolve
	if (resolve)
	    resolveClass(c);

	return c;
    }


    /* ------------------------------------------------------------ */
    /** Return true a class is modified.
     * @return true if any of the classes loaded by this loader have been
     * modified since their load time.
     */
    synchronized public boolean isModified()
    {
	for (int f=loaded.size();f-->0;)
	{
	    LoadedFile lf = (LoadedFile)loaded.elementAt(f);
	    if (!lf.file.exists() || lf.file.lastModified()>lf.timeLoaded)
		return true;
	}
	return false;	
    }
    
};






