// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.stresstest;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/* ------------------------------------------------------------ */
public class ForwardServlet extends HttpServlet
{
      /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException
    {
        doGet(request,response);
    }
    
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException
    {
        RequestDispatcher rd = getServletContext().getRequestDispatcher(request.getPathInfo());
        rd.forward(request,response);
    }
}
