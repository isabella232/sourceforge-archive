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
	HttpSession session =
	    request.getSession(true);
	    
	PrintWriter pout = new PrintWriter(response.getOutputStream());
	Page page=null;

	try{

	    String action = request.getParameter("Action");
	    if (action!=null)
	    {
		String name =  request.getParameter("Name");
		String value =  request.getParameter("Value");
		Code.debug(action);
		if (action.equals("Add to Session"))
		    session.putValue(name,value);
		else
		    session.removeValue(name);
	    }
	    
	    page = Page.getPage(pageType,request);
	    page.title("Session Dump Servlet");	    

	    Table table = new Table(0).cellPadding(0).cellSpacing(0);
	    page.add(table);
	    table.newRow();
	    table.newHeading()
		.cell().nest(new Font(2,true))
		.add("<BR>Session Data")
		.attribute("COLSPAN","2")
		.left();

	    String[] keys= session.getValueNames();

	    for (int k=keys.length;k-->0;)
	    {
		String name=keys[k];
		
		table.newRow();
		table.addHeading(name+":&nbsp;").cell().right().top();
		Object value = session.getValue(name);

		// XXX - Get rid of session context ????
		if (name.equals(SessionContext.SessionStatus))
		{
		    if (value.equals(SessionContext.NewSession))
			table.addCell("New  - This is the first hit of the session<BR>(or the browswer does not support cookies).");
		    else if (value.equals(SessionContext.OldSession))
			table.addCell("Old  - The server has been restarted.<BR> Session data lost.");
		    else
			table.addCell(value.toString()+
				      "   - This session is confirmed");
		}
		else
		    table.addCell(value);
	    }
	    
	    page.add("<P><B>Form to modify session</B>");
	    TableForm tf = new TableForm(request.getRequestURI());
	    page.add(tf);
	    tf.addTextField("Name","Name",20,"name");
	    tf.addTextField("Value","Value",20,"value");
	    tf.addButton("Action","Add to Session");
	    tf.addButton("Action","Delete from Session");
	}
	catch (Exception e){
	    Code.warning("SessionDump",e);
	}
	

    
	page.write(pout);
	pout.flush();
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo() {
        return "Dump Servlet";
    }

}
