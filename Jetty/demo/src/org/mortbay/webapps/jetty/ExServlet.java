// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.webapps.jetty;
import org.mortbay.http.HttpException;
import org.mortbay.util.Code;
import org.mortbay.util.Loader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.UnavailableException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/* ------------------------------------------------------------ */
/** Dump Servlet Request.
 * 
 */
public class ExServlet extends HttpServlet
{
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest sreq, HttpServletResponse sres) 
        throws ServletException, IOException
    {
        String info=sreq.getPathInfo();
        try
        {
            throw (Throwable)(Loader.loadClass(this.getClass(),
                                               info.substring(1)).newInstance());
        }
        catch(Throwable th)
        {
            throw new ServletException(th);
        }
        
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Exception Servlet";
    }    
}
