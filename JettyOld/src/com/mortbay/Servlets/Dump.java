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
public class Dump extends HttpServlet
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
        OutputStream out = sres.getOutputStream();
	PrintWriter pout = new PrintWriter(out);

	Page page=null;

	try{
	    page = Page.getPage(pageType,sreq);
	    page.title("Dump Request Servlet");	    

	    Table table = new Table(0).cellPadding(0).cellSpacing(0);
	    page.add(table);
	    table.newRow();
	    table.newHeading()
		.cell().nest(new Font(2,true))
		.add("<BR>Interface javax.servlet.* Request Methods")
		.attribute("COLSPAN","2")
		.left();

	    table.newRow();
	    table.addHeading("getMethod:&nbsp;").cell().right();
	    table.addCell(sreq.getMethod());
	    table.newRow();
	    table.addHeading("getContentLength:&nbsp;").cell().right();
	    table.addCell(Integer.toString(sreq.getContentLength()));
	    table.newRow();
	    table.addHeading("getContentType:&nbsp;").cell().right();
	    table.addCell(sreq.getContentType());
	    table.newRow();
	    table.addHeading("getRequestURI:&nbsp;").cell().right();
	    table.addCell(sreq.getRequestURI());
	    table.newRow();
	    table.addHeading("getServletPath:&nbsp;").cell().right();
	    table.addCell(sreq.getServletPath());
	    table.newRow();
	    table.addHeading("getPathInfo:&nbsp;").cell().right();
	    table.addCell(sreq.getPathInfo());
	    table.newRow();
	    table.addHeading("getPathTranslated:&nbsp;").cell().right();
	    table.addCell(sreq.getPathTranslated());
	    table.newRow();
	    table.addHeading("getQueryString:&nbsp;").cell().right();
	    table.addCell(sreq.getQueryString());
	
	    table.newRow();
	    table.addHeading("getProtocol:&nbsp;").cell().right();
	    table.addCell(sreq.getProtocol());
	    table.newRow();
	    table.addHeading("getServerName:&nbsp;").cell().right();
	    table.addCell(sreq.getServerName());
	    table.newRow();
	    table.addHeading("getServerPort:&nbsp;").cell().right();
	    table.addCell(Integer.toString(sreq.getServerPort()));
	    table.newRow();
	    table.addHeading("getRemoteUser:&nbsp;").cell().right();
	    table.addCell(sreq.getRemoteUser());
	    table.newRow();
	    table.addHeading("getRemoteAddr:&nbsp;").cell().right();
	    table.addCell(sreq.getRemoteAddr());
	    table.newRow();
	    table.addHeading("getRemoteHost:&nbsp;").cell().right();
	    table.addCell(sreq.getRemoteHost());	    
	    table.newRow();
	    table.addHeading("getInitParameter(\"param\"):&nbsp;").cell().right();
	    table.addCell(getInitParameter("param"));

	    table.newRow();
	    table.newHeading()
		.cell().nest(new Font(2,true))
		.add("<BR>Other HTTP Headers")
		.attribute("COLSPAN","2")
		.left();
	    String name;
	    Enumeration h = sreq.getHeaderNames();
	    while (h.hasMoreElements())
	    {
		name=(String)h.nextElement();
		table.newRow();
		table.addHeading(name+":&nbsp;").cell().right();
		table.addCell(sreq.getHeader(name));
	    }
	    
	    table.newRow();
	    table.newHeading()
		.cell().nest(new Font(2,true))
		.add("<BR>Request Parameters")
		.attribute("COLSPAN","2")
		.left();
	    h = sreq.getParameterNames();
	    while (h.hasMoreElements())
	    {
		name=(String)h.nextElement();
		table.newRow();
		table.addHeading(name+":&nbsp;").cell().right();
		table.addCell(sreq.getParameter(name));
		String[] values = sreq.getParameterValues(name);
		if (values!=null && values.length>1)
		{
		    for (int i=0;i<values.length;i++)
		    {
			table.newRow();
			table.addHeading(name+"["+i+"]:&nbsp;")
			    .cell().right();
			table.addCell(values[i]);
		    }
		}
	    }

	    page.add(Break.para);
	    
	    page.add(new Heading(1,"Form to generate Dump content"));
	    TableForm tf = new TableForm(sreq.getRequestURI());
	    tf.method("POST");
	    page.add(tf);
	    tf.addTextField("TextField","TextField",20,"value");
	    Select select = tf.addSelect("Select","Select",true,3);
	    select.add("ValueA");
	    select.add("ValueB1,ValueB2");
	    select.add("ValueC");
	    tf.addButton("Action","Submit");
	}
	catch (Exception e)
	{
	    Code.warning("Dump",e);
	}
    
	page.write(pout);
	pout.flush();
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo() {
        return "Dump Servlet";
    }

}
