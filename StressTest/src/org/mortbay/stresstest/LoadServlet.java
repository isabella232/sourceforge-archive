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
public class LoadServlet extends HttpServlet
{
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);
    }

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
        String info=request.getPathInfo();
        String encoding=request.getParameter("encoding");
        if (encoding!=null)
            request.setCharacterEncoding(encoding);

        // Reader r = request.getReader();
        // while (r.read()!=-1);

        int lines=10;
        String l=request.getParameter("lines");
        if (l!=null)
            lines=Integer.parseInt(l);
        
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        for (int i=0;i<10;i++)
        {
            out.write("Now ");
            out.write("is the time for all good men to come to the aid of the party\n");
        }
        out.flush();
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Load Servlet";
    }
    
}
