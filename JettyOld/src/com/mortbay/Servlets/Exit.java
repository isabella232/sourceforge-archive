// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Servlets;
import com.mortbay.Base.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/** Exit the server.
 * Need to move this servlet to Jetty package to avoid the	    
 * cyclic dependency between packages.
 */
public class Exit extends HttpServlet
{
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest sreq, HttpServletResponse sres) 
	throws ServletException, IOException
    {
	Code.warning("Exit requested");

	new Thread(new Runnable(){
	    public void run(){
		try{Thread.sleep(1000);}catch(Exception e){}
		com.mortbay.Jetty.Server.stopAll();
	    }
	}).start();

	sres.setContentType("text/html");
	PrintWriter out = new PrintWriter(sres.getWriter());
	out.println("<H1>HTTP Server exiting...</H1>");
    }    
}
