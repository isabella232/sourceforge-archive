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

public class DebugServlet extends HttpServlet
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
    public void doPost(HttpServletRequest request,
			HttpServletResponse response) 
	throws ServletException, IOException
    {
	if ("Set Options".equals(request.getParameter("Action")))
	{
	    Code.setDebug("on".equals(request.getParameter("D")));
	    Code.setSuppressWarnings("on".equals(request.getParameter("W")));
	    Code.setSuppressStack("on".equals(request.getParameter("S")));
	    String v=request.getParameter("V");
	    if (v!=null && v.length()>0)
		Code.setVerbose(Integer.parseInt(v));
	    else
		Code.setVerbose(0);
	    Code.setDebugPatterns(request.getParameter("P"));
	    Code.setDebugTriggers(request.getParameter("T"));

	    String lo="";
	    if ("on".equals(request.getParameter("Lt")))
		lo+=Log.TIMESTAMP;
	    if ("on".equals(request.getParameter("LL")))
		lo+=Log.LABEL;
	    if ("on".equals(request.getParameter("LT")))
		lo+=Log.TAG;
	    if ("on".equals(request.getParameter("Ls")))
		lo+=Log.STACKSIZE;
	    if ("on".equals(request.getParameter("LS")))
		lo+=Log.STACKTRACE;
	    if ("on".equals(request.getParameter("LO")))
		lo+=Log.ONELINE;
	    Log.instance().setOptions(lo);
		
	    Code.warning("Debug options changed");
	    Code.debug("Debug options changed");
	}
	    
	doGet(request,response);
    }
	
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
			HttpServletResponse response) 
	throws ServletException, IOException
    {
        response.setContentType("text/html");
        OutputStream out = response.getOutputStream();
	PrintWriter pout = new PrintWriter(out);

	Page page=null;

	try{
	    page = Page.getPage(pageType,request);
	    page.title("Debug Servlet");

	    page.add("This form displays and sets the debug and log options<P>");
    	    TableForm tf = new TableForm(request.getRequestURI());
	    page.add(tf);

	    tf.addCheckbox("D","Debug On",Code.getDebug());
	    tf.addCheckbox("W","Suppress Warnings",Code.getSuppressWarnings());
	    tf.addCheckbox("S","Suppress Stack Trace",Code.getSuppressStack());
	    tf.addTextField("V","Verbosity Level",2,""+Code.getVerbose());
	    tf.addTextField("P","Debug Patterns",40,Code.getDebugPatterns());
	    tf.addTextField("T","Debug Triggers",40,Code.getDebugTriggers());

	    String lo = Log.instance().getOptions();
	    if (lo==null)
		lo="";
	    tf.addCheckbox("Lt","Time Stamp",lo.indexOf(Log.TIMESTAMP)>=0);
	    tf.addCheckbox("LL","Labels",lo.indexOf(Log.LABEL)>=0);
	    tf.addCheckbox("LT","Tags",lo.indexOf(Log.TAG)>=0);
	    tf.addCheckbox("Ls","Stack Size",lo.indexOf(Log.STACKSIZE)>=0);
	    tf.addCheckbox("LS","Stack Trace",lo.indexOf(Log.STACKTRACE)>=0);
	    tf.addCheckbox("LO","One Line",lo.indexOf(Log.ONELINE)>=0);
	    
	    tf.addButton("Action","Set Options");
	}
	catch (Exception e)
	{
	    Code.warning("Debug",e);
	}
	
	page.write(pout);
	pout.flush();
    }
};
