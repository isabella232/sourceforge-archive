// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Servlet;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This is an example of a simple Servlet
 */
public class RequestDispatchTest extends HttpServlet
{
    /* ------------------------------------------------------------ */
    String pageType;

    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);
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
	String prefix = sreq.getContextPath()!=null
	    ? sreq.getContextPath()+sreq.getServletPath()
	    : sreq.getServletPath();
	
        sres.setContentType("text/html");

	String info ;

	if (sreq.getAttribute("javax.servlet.include.servlet_path")!=null)
	    info=(String)sreq.getAttribute("javax.servlet.include.path_info");
	else
	    info=sreq.getPathInfo();
	
	if (info==null)
	    info="NULL";

	if (info.startsWith("/include/"))
	{
	    info=info.substring(8);
            if (info.indexOf('?')<0)
                info+="?Dispatch=includeWriter";
	    else
                info+="&Dispatch=includeWriter";

	    if (System.currentTimeMillis()%2==0)
	    {
		PrintWriter pout = sres.getWriter();
		pout.write("<H1>Include: "+info+"</H1><HL>");
		pout.flush();
		
		RequestDispatcher dispatch = getServletContext()
		    .getRequestDispatcher(info);
		dispatch.include(sreq,sres);
		
		pout.write("<HL><H1>-- Included (writer)</H1>");
	    }
	    else 
	    {
		OutputStream out = sres.getOutputStream();
		PrintWriter pout = new PrintWriter(out);
		pout.write("<H1>Include: "+info+"</H1><HL>");
		pout.flush();
		
		RequestDispatcher dispatch = getServletContext()
		    .getRequestDispatcher(info);
		dispatch.include(sreq,sres);
		
		pout.write("<HL><H1>-- Included (outputstream)</H1>");
	    }
	}
	else if (info.startsWith("/forward/"))
	{
	    info=info.substring(8);
            if (info.indexOf('?')<0)
                info+="?Dispatch=forward";
	    else
		info+="&Dispatch=forward";
	    RequestDispatcher dispatch = getServletContext().getRequestDispatcher(info);
	    dispatch.forward(sreq,sres);
	}
	else if (info.startsWith("/includeN/"))
	{
	    info=info.substring(10);
	    PrintWriter pout = sres.getWriter();
	    pout.write("<H1>Include named: "+info+"</H1><HL>");
	    pout.flush();

	    RequestDispatcher dispatch = getServletContext()
		.getNamedDispatcher(info);
	    if (dispatch!=null)
		dispatch.include(sreq,sres);
	    else
		pout.write("<H1>No servlet named: "+info+"</H1>");

	    pout.write("<HL><H1>Included ");
	}
	else if (info.startsWith("/forwardN/"))
	{
	    info=info.substring(10);
	    RequestDispatcher dispatch = getServletContext()
		.getNamedDispatcher(info);
	    if (dispatch!=null)
		dispatch.forward(sreq,sres);
	    else
	    {
		PrintWriter pout = sres.getWriter();
		pout.write("<H1>No servlet named: "+info+"</H1>");
	    }
	    
	}
	else
	{
	    PrintWriter pout = sres.getWriter();
	    pout.write("<H1>Dispatch URL must be of the form: </H1>"+
		       "<PRE>"+prefix+"/include/path\n"+
		       prefix+"/forward/path\n"+
		       prefix+"/includeN/name\n"+
		       prefix+"/forwardN/name</PRE>"
		       );
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
