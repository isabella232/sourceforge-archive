// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ========================================================================

package com.mortbay.Servlets;

import com.mortbay.Base.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.mortbay.HTML.*;
import java.io.*;
import java.util.*;


/** LookAndFeel Servlet
 * <p> The LookAndFeel Servlet serves file based pages as the body of html
 * pages which it wraps in a certain LookAndFeel. The LookAnd Feel it uses is
 * given in the "LookAndFeel" init parameter, but each individual request
 * must supply a "Heading" query parameter at the minimum. All other query
 * parameters given in the request are passed to the LookAndFeel as
 * LookAndFeel parameters.
 *
 * <p> The LookAndFeel Servlet is thus good for using for fomatting help
 * pages in their proper LookAndFeel, since a particular instance of the
 * LookAndFeel Servlet can be configured with the correct LookAndFeel for the
 * referring pages and then the individual requests override parameters with
 * the settings from the referring page.
 *
 * <p><h4>Notes</h4>
 * <p> The page file is described in the request pathInfo, prefixed with a
 * path given in the "FileBase" init parameter. The page heading is in the
 * "Heading" query parameter. 
 *
 * @version $Id$
 * @author Greg Wilkins
*/
public class LookAndFeelServlet extends HttpServlet
{
    /* ------------------------------------------------------------ */
    String fileBase;
    String pageType;
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
	 throws ServletException
    {
	super.init(config);
	
	fileBase = getInitParameter(Page.FileBase);
	if (fileBase==null)
	    fileBase = Page.FileBase;
	pageType = getInitParameter(Page.PageType);
	if (pageType ==null)
	    pageType=Page.getDefaultPageType();
    }

    /* ------------------------------------------------------------ */
    public void service(HttpServletRequest request,
			HttpServletResponse response) 
	 throws ServletException, IOException
    {
	try{
	    response.setContentType("text/html");
	    OutputStream out = response.getOutputStream();

	    Page page = Page.getPage(pageType,request);
	    page.properties().put(Page.Request,request);
	    
	    String referer = request.getHeader("Referer");
	    if (referer != null)
		page.properties().put(Page.Back, referer);
	
	    Enumeration enum = request.getParameterNames();
	    while (enum.hasMoreElements())
	    {
		String key = enum.nextElement().toString();
		if (!key.equals(Page.Help))
		    page.properties().put(key,request.getParameter(key));
	    }

	    Code.debug("Include file:" + fileBase +
		       request.getServletPath() +
		       request.getPathInfo());

	    try{
		page.add(new Include(fileBase,
				     request.getServletPath()+
				     request.getPathInfo()));
	    }
	    catch(FileNotFoundException ioe){
		return;
	    }
	    
	    page.write(out);
	}
	catch(RuntimeException e){
	    throw e;
	}
	catch(IOException e){
	    throw e;
	}
	catch(Exception e){
	    Code.debug("Converted",e);
	    throw new ServletException(e.toString());
	}
    }
};
