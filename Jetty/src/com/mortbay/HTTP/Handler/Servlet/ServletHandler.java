// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler.Servlet;
//import com.sun.java.util.collections.*; XXX-JDK1.1
import java.util.*; //XXX-JDK1.2

import com.mortbay.HTTP.*;
import com.mortbay.HTTP.Handler.NullHandler;
import com.mortbay.Util.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import javax.servlet.*;
import javax.servlet.http.*;



/* --------------------------------------------------------------------- */
/** ServletHandler<p>
 * This handler maps requests to servlets that implement the
 * javax.servlet.http.HttpServlet API.
 * It is configured with a PathMap of paths to ServletHolder instances.
 *
 * @version $Id$
 * @author Greg Wilkins
 */
public class ServletHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    private PathMap _servletMap=new PathMap();
    private Map _nameMap=new HashMap();
    private Context _context;
    private boolean _autoReload;
    private ServletLoader _loader;

    /* ----------------------------------------------------------------- */
    /** Construct basic auth handler.
     */
    public ServletHandler()
    {
	_context = new Context(this);
    }
    
    /* ------------------------------------------------------------ */
    public Context getContext()
    {
	return _context;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isAutoReload()
    {
	return _autoReload;
    }
    /* ------------------------------------------------------------ */
    public void setAutoReload(boolean autoReload)
    {
	_autoReload = autoReload;
    }
    
    /* ------------------------------------------------------------ */
    public ServletLoader getServletLoader()
    {
	return _loader;
    }
    
    /* ------------------------------------------------------------ */
    private synchronized void initializeLoader()
    {
	try
	{
	    String classPath=getHandlerContext().getClassPath();	    
	    if (classPath!=null && classPath.length()>0)
	    {
		_loader=new ServletLoader(classPath,false);
		Code.debug("servlet classpath=",classPath);
	    }
	    else
		_loader=new ServletLoader(null,false);    
	}
	catch(Throwable e)
	{
	    Code.fail(e);
	}
    }
    
    /* ----------------------------------------------------------------- */
    public void start()
    {
	initializeLoader();
        Log.event("ServletHandler started");
	super.start();
    }
	
    
    /* ------------------------------------------------------------ */
    /** 
     * @param path 
     * @param servletClass 
     */
    public ServletHolder addServlet(String pathSpec,
				    String servletClass)
    {
	try
	{
	    ServletHolder holder =
		newServletHolder(servletClass);
	    addHolder(pathSpec,holder);
	    return holder;
	}
	catch(Exception e)
	{
	    Code.warning(e);
	    throw new IllegalArgumentException(e.toString());
	}
    }

    /* ------------------------------------------------------------ */
    /** Get servlet by name.
     * @param name 
     * @return 
     */
    public ServletHolder getServletHolder(String name)
    {
	return (ServletHolder)_nameMap.get(name);
    }
    
    /* ------------------------------------------------------------ */
    public void addHolder(String pathSpec, ServletHolder holder)
    {
	_servletMap.put(pathSpec,holder);
    }
    
    /* ----------------------------------------------------------------- */
    /** 
     * @param handlerPathSpec 
     * @param httpRequest 
     * @param httpResponse 
     * @exception IOException 
     */
    public void handle(String contextPathSpec,
		       HttpRequest httpRequest,
                       HttpResponse httpResponse)
         throws IOException
    {
	if (!isStarted())
	    return;
	
	try
	{
	    // Handle reload 
	    if (isAutoReload())
	    {
		synchronized(this)
		{
		    if (_loader.isModified())
		    {
			Log.event("RELOAD "+this);
			// XXX Should wait for requests to goto 0;
			
			// destroy old servlets
			destroy();
			// New loader
			initializeLoader();
		    }
		}
	    }
	    
	    
	    String path = httpRequest.getPath();
	    String contextPath=null;
	    if (!"/".equals(contextPathSpec))
	    {
		contextPath=PathMap.pathMatch(contextPathSpec,path);
		path=PathMap.pathInfo(contextPathSpec,path);
	    }
	    
	    
	    // Build servlet request and response
	    ServletRequest request =
		new ServletRequest(contextPath,
				   httpRequest,
				   _context);
	    ServletResponse response =
		new ServletResponse(request,httpResponse);

	    handle(path,request,response);
	}
	catch(Exception e)
	{
	    Code.warning(e);
	    System.err.println(httpRequest);
	    throw new HttpException();
	}
    }


    /* ------------------------------------------------------------ */
    /** 
     * @param path 
     * @param request 
     * @param response 
     * @exception IOException 
     * @exception ServletException 
     * @exception UnavailableException 
     */
    void handle(String path,ServletRequest request, ServletResponse response)
	throws IOException, ServletException, UnavailableException
    {
	Code.debug("Looking for servlet at ",path);
	HttpResponse httpResponse=response.getHttpResponse();
	
	List matches=_servletMap.getMatches(path);
	
	for (int i=0;i<matches.size();i++)
	{
	    Map.Entry entry =
		(Map.Entry)matches.get(i);
	    String servletPathSpec=(String)entry.getKey();
	    ServletHolder holder = (ServletHolder)entry.getValue();
	    
	    Code.debug("Pass request to servlet at ",entry);
	    request.setServletPath(PathMap.pathMatch(servletPathSpec,path),
				   PathMap.pathInfo(servletPathSpec,path));
	    
	    // Check session stuff
	    HttpSession session=null;
	    if ((session=request.getSession(false))!=null)
		Context.access(session);
	    
	    // try service request
	    holder.handle(request,response);
	    
	    // Break if the response has been updated
	    if (httpResponse.isDirty())
	    {
		Code.debug("Handled by ",entry);
		if (!httpResponse.isCommitted())
		    httpResponse.commit();
		break;
	    }
	}
    }
	
    /* ------------------------------------------------------------ */
    /** 
     * @param servletName 
     * @return 
     */
    public ServletHolder newServletHolder(String servletClass)
	throws javax.servlet.UnavailableException,
	       ClassNotFoundException
    {
	return new ServletHolder(this,servletClass);
    }

    
    /* ------------------------------------------------------------ */
    /** Destroy Handler.
     * Destroy all servlets
     */
    public synchronized void destroy()
    {
        Iterator i = _servletMap.values().iterator();
        while (i.hasNext())
        {
            ServletHolder holder = (ServletHolder)i.next();
            holder.destroy();
        }
    }


    /* ------------------------------------------------------------ */
    void mapHolder(String name,ServletHolder holder, String oldName)
    {
	synchronized(_nameMap)
	{
	    if (oldName!=null)
		_nameMap.remove(oldName);
	    _nameMap.put(name,holder);
	}
    }
}
