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

/**
 * This is an example of a simple Servlet
 */
public class Exit extends HttpServlet
{
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest sreq, HttpServletResponse sres) 
	throws ServletException, IOException
    {
	Code.warning("Exit requested");
	// XXX - Need to move this servlet to Jetty package to avoid the
	// cyclic dependency between packages.
	com.mortbay.Jetty.Server.shutdown();
    }    
}
