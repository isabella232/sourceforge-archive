// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty;
import com.mortbay.Util.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This is an example of a simple Servlet
 */
public class JettyServlet extends HttpServlet
{
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
        throws ServletException, IOException
    {	
        String pathInfo=request.getPathInfo();

	File file=(File)
	    request.getAttribute("JettyFile");
	if (file==null)
	{
	    if ("/".equals(pathInfo))
		pathInfo="/index.html";
	    
	    if (!pathInfo.endsWith(".html"))
		return;
	    
	    file = new File(request.getPathTranslated());
	}

	if (file==null || !file.exists())
	    return;
	
	JettyPage page = new JettyPage(pathInfo);
	if (page.getSection()==null)
	    return;
	
	page.add(new Include(file));
	
	PrintWriter pout = response.getWriter();
	page.write(pout);
	pout.flush();
    }
    

    /* ------------------------------------------------------------ */
    public long getLastModified(HttpServletRequest request)
    {
	long lm=-1;
	try{
	    String pathInfo=request.getPathInfo();
	    File file=null;

	    if ("/".equals(pathInfo))
	    {
		pathInfo="/index.html";
		String filename=request.getPathTranslated()+"index.html";
		file = new File(filename);
	    }
	    else if (pathInfo.endsWith(".html"))
	    {
		file = new File(request.getPathTranslated());
	    }
	    
	    if (file!=null && file.exists())
	    {
		request.setAttribute("JettyFile",file);
		lm=file.lastModified();
	    }
	}
	catch(Exception e)
	{
	    Code.ignore(e);
	}
	return lm;
    }
    
    
}



