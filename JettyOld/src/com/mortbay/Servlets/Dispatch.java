// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Servlets;
import com.mortbay.Base.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This is an example of a simple Servlet
 */
public class Dispatch extends HttpServlet
{
    /* ------------------------------------------------------------ */
    String pageType;

    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);
        
        pageType = getInitParameter(Page.PageType);
        if (pageType ==null)
            pageType=Page.getDefaultPageType();
    }

    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest sreq, HttpServletResponse sres) 
        throws ServletException, IOException
    {
        doGet(sreq,sres);
    }
    
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest sreq, HttpServletResponse sres) 
        throws ServletException, IOException
    {   
        sres.setContentType("text/html");

	String info = sreq.getPathInfo();
	if (info==null)
	    info="NULL";

	if (info.startsWith("/include/"))
	{
	    info=info.substring(8);
            if (info.indexOf('?')<0)
                info+="?Dispatch=Demo";
	    PrintWriter pout = sres.getWriter();
	    pout.write("<H1>Include: "+info+"</H1><HL>");
	    pout.flush();

	    RequestDispatcher dispatch = getServletContext().getRequestDispatcher(info);
	    dispatch.include(sreq,sres);

	    pout.write("<HL><H1>Included ");
	}
	else if (info.startsWith("/INCLUDE/"))
	{
	    info=info.substring(8);
            if (info.indexOf('?')<0)
                info+="?Dispatch=Demo";
	    OutputStream out = sres.getOutputStream();
	    PrintWriter pout = new PrintWriter(out);
	    pout.write("<H1>Include: "+info+"</H1><HL>");
	    pout.flush();


	    RequestDispatcher dispatch = getServletContext().getRequestDispatcher(info);
	    dispatch.include(sreq,sres);

	    pout.write("<HL><H1>Included ");
	}
	else if (info.startsWith("/forward/"))
	{
	    info=info.substring(8);
            if (info.indexOf('?')<0)
                info+="?Dispatch=Demo";
	    RequestDispatcher dispatch = getServletContext().getRequestDispatcher(info);
	    dispatch.forward(sreq,sres);
	}
	else
	{
	    PrintWriter pout = sres.getWriter();
	    pout.write("<H1>Dispatch URL must be of the form: "+
		       "<BR>/Dispatch/include/path"+
		       "<BR>/Dispatch/INCLUDE/path"+
		       "<BR>/Dispatch/forward/path</H1>");
	    pout.flush();
	}
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Include Servlet";
    }

    /* ------------------------------------------------------------ */
    public synchronized void destroy()
    {
        Code.debug("Destroyed");
    }
    
}
