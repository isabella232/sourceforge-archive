// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Servlets;
import com.mortbay.Base.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTTP.Handler.*;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This is an example of a simple Servlet
 */
public class SessionDump extends HttpServlet
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
    public void service(HttpServletRequest request,
			HttpServletResponse response) 
	throws ServletException, IOException
    {
	PrintWriter pout = new PrintWriter(response.getOutputStream());
	Page page=Page.getPage(pageType,request,response);
	page.title("Session Dump Servlet");
	String action = request.getParameter("Action");
	
	HttpSession session = request.getSession("New Session".equals(action));
	TableForm tf =
	    new TableForm(response.encodeURL(request.getRequestURI()));
	    
	if (session==null)
	{
	    page.add("<B>No Session</B>");
	    tf.addButton("Action","New Session");
	    page.add(tf);
	}
	else
	{
	    try
	    {
		String id = session.getId();
	    
		if (action!=null)
		{
		    String name =  request.getParameter("Name");
		    String value =  request.getParameter("Value");
		    if (action.equals("Invalidate"))
			session.invalidate();
		    else if (action.equals("Add"))
			session.putValue(name,value);
		    else if (action.equals("Remove"))
			session.removeValue(name);
		}
	    
		Table table = new Table(0).cellPadding(0).cellSpacing(0);
		page.add(table);
		table.newRow();
		table.newHeading()
		    .cell().nest(new Font(2,true))
		    .add("<BR>Session "+id+
			 (session.isNew()?" is NEW</B>":" is valid</B><BR>"))
		    .attribute("COLSPAN","2")
		    .left();

		String[] keys= session.getValueNames();
		for(int k=keys.length;k-->0;)
		{
		    String name=keys[k];
		    
		    table.newRow();
		    table.addHeading(name+":&nbsp;").cell().right().top();
		    Object value = session.getValue(name);
		    table.addCell(value);
		}
	    
		page.add(tf);
		tf.addTextField("Name","Name",20,"name");
		tf.addTextField("Value","Value",20,"value");
		tf.addButton("Action","Add");
		tf.addButton("Action","Remove");
		tf.addButton("Action","Invalidate");

		
		page.add("<P>Turn off cookies in your browser to try url encoding<BR>");

	    }
	    catch (IllegalStateException e)
	    {
		Code.debug(e);
		page.add("<B>INVALID Session</B>");
		tf=new TableForm(request.getRequestURI());
		tf.addButton("Action","New Session");
		page.add(tf);
	    }
	}
	
	page.write(pout);
	pout.flush();
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo() {
        return "Dump Servlet";
    }

}
