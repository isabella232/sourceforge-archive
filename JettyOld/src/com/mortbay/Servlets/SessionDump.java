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
    int redirectCount=0;
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
	HttpSession session = request.getSession(false);
	String action = request.getParameter("Action");
	String name =  request.getParameter("Name");
	String value =  request.getParameter("Value");

	Code.warning(action+" : "+name+" == "+value);
	
	if (action.equals("New Session"))
	    session = request.getSession(true);
	else if (action.equals("Invalidate"))
	    session.invalidate();
	else if (action.equals("Add"))
	    session.putValue(name,value);
	else if (action.equals("Remove"))
	    session.removeValue(name);

	response.sendRedirect(request.getRequestURI()+
			      "?R="+redirectCount++);
    }
    
	
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
	throws ServletException, IOException
    {
	PrintWriter pout = new PrintWriter(response.getOutputStream());
	Page page=Page.getPage(pageType,request,response);
	page.title("Session Dump Servlet");
	
	HttpSession session = request.getSession(false);
	
	TableForm tf =
	    new TableForm(response.encodeURL(request.getRequestURI()));
	tf.method("POST");    
	page.add(tf);
	
	if (session==null)
	{
	    page.add("<B>No Session</B>");
	    tf.addButton("Action","New Session");
	}
	else
	{
	    try
	    {
		String id = session.getId();
	    
		tf.addText("ID",session.getId());
		tf.addText("State",session.isNew()?"NEW":"Valid");
		tf.addText("Creation",
			   new Date(session.getCreationTime()).toString());
		tf.addText("Last Access",
			   new Date(session.getLastAccessedTime()).toString());
		tf.addText("Max Inactive",
			   ""+session.getMaxInactiveInterval());
		
		String[] keys= session.getValueNames();
		for(int k=keys.length;k-->0;)
		{
		    String name=keys[k];
		    String value=session.getValue(name).toString();
		    tf.addText(name,value);
		}
		
		tf.addTextField("Name","Property Name",20,"name");
		tf.addTextField("Value","Property Value",20,"value");
		tf.addButtonArea();
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
