// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler.Servlet;
//import com.sun.java.util.collections.*; XXX-JDK1.1

import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.net.URL;


/* --------------------------------------------------------------------- */
/** ServletHandler<p>
 * This handler maps requests to servlets that implement the
 * javax.servlet.http.HttpServlet API.
 * It is configured with a PathMap of paths to ServletHolder instances.
 *
 * @version $Id$
 * @author Greg Wilkins
 */
public class DynamicHandler extends ServletHandler 
{
    private Set _paths = new HashSet();
	
    /* ------------------------------------------------------------ */
    Map _initParams ;
    public Map getInitParams()
    {
	return _initParams;
    }
    public void setInitParams(Map initParams)
    {
	_initParams = initParams;
    }
    
    /* ----------------------------------------------------------------- */
    public DynamicHandler()
    {}
    
    /* ----------------------------------------------------------------- */
    public void start()
    {
        Log.event("DynamicHandler started for "+
		  getHandlerContext().getClassPath());
	super.start();
    }
    

    /* ------------------------------------------------------------ */
    /** List of ServletHolders matching path. 
     * @param contextPathSpec Context path spec
     * @param pathInContext Path including context
     * @return List of matching holders.
     */
    synchronized List holderMatches(String pathInContext)
    {
	String path=pathInContext;
	
	// Do we have any matches already
	List holders = super.holderMatches(path);
	if (holders!=null && holders.size()>0)
	    return holders;

	// OK lets look for a dynamic servlet.
	if (!_paths.contains(path))
	{
	    _paths.add(path);
	    Code.debug(path," from ",
		       getHandlerContext().getClassPath());
	    
	    String servletClass=path.substring(1);
	    int slash=servletClass.indexOf("/");
	    if (slash>=0)
		servletClass=servletClass.substring(0,slash);            
	    if (servletClass.endsWith(".class"))
		servletClass=servletClass.substring(0,servletClass.length()-6);
	    
	    path="/"+servletClass;
	    
	    Code.debug("Dynamic path=",path);
	    
	    ServletHolder holder=null;
	    try{
		holder=newServletHolder(servletClass);
		Map params=getInitParams();
		if (params!=null)
		    holder.putAll(params);
		holder.getServlet();
	    }
	    catch(Exception e)
	    {
		Code.ignore(e);
		return super.holderMatches(path);
	    }
	    
	    Log.event("Dynamic load '"+servletClass+"' at "+path);
	    addHolder(path,holder);
	    addHolder(path+".class",holder);
	    addHolder(path+"/*",holder);
	    addHolder(path+".class/*",holder);
	}

	// return the normal list
	return super.holderMatches(pathInContext);
    }
}





