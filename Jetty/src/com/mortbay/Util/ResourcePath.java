// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;
//import com.sun.java.util.collections.*; XXX-JDK1.1

import com.mortbay.Util.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Resource Path.
 *
 * Access files from a path of URLs, directories and Jar files.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ResourcePath
{
    /* ------------------------------------------------------------ */
    private List _paths=new ArrayList();
    private List _loaded=new ArrayList();
	
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public ResourcePath(String path)
	throws IOException
    {
	this(path,false);
    }
	
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param servletClassPath 
     */
    public ResourcePath(String path, boolean quiet)
	throws IOException
    { 
	StringTokenizer tokenizer = new StringTokenizer(path,",;");
	 
	while (tokenizer.hasMoreTokens())
	{
	    String token=tokenizer.nextToken();
	    Resource resource = Resource.newResource(token);
	    if( !resource.exists())
		throw new FileNotFoundException(token);
	    _paths.add(resource);
	}
    }
	
    /* ------------------------------------------------------------ */
    /** 
     */
    public InputStream getInputStream(String filename)
    {
	if (Code.verbose()) Code.debug("get ",filename);
	 
	// Maybe it should get here with / instead of \.
	filename = filename.replace( '\\', '/');
	InputStream in=null;
	int length=0;

	// For each potential path
	for (int p=0;p<_paths.size();p++)
	{
	    Resource resource=null;
	    try
	    {
		Resource resourceBase = (Resource)_paths.get(p);
		resource = resourceBase.relative(filename);
		if (resource.exists())
		{
		    if (Code.verbose(1000))
			Code.debug("Look in dir ",resourceBase, " for ",filename);

		    in = resource.getInputStream();
		    length=(int)resource.length();
	 		 	
		    LoadedFile lc=new LoadedFile();
		    lc.resource=resource;
		    lc.lastModified=resource.lastModified();
		    lc.resource.release();
		    if (lc.lastModified>0)
			_loaded.add(lc);
		    if (Code.verbose()) Code.debug("Found ",filename);
		    break;
		}
	    }
	    catch(Exception e)
	    {
		if (Code.verbose())
		    Code.debug(e);
	    }
	    finally
	    {
		if(resource!=null)
		    resource.release();
	    }
	}
	 
	return in;
    }

    /* ------------------------------------------------------------ */
    /** Return true a loaded file is modified.
     * @return true if any of the files loaded have been
     * modified since their load time.
     * 
     */
    synchronized public boolean isModified()
    {
	for (int f=_loaded.size();f-->0;)
	{
	    LoadedFile lf = (LoadedFile)_loaded.get(f);
	    if (!lf.resource.exists() || lf.resource.lastModified()>lf.lastModified)
		return true;
	}
	return false; 
    }

    /* ------------------------------------------------------------ */
    static class LoadedFile
    {
	Resource resource;
	long lastModified;
    }
}


