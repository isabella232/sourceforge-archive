// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.webapps.jetty;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.util.Loader;

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
        catch(ServletException e)
        {
            throw e;
        }
        catch(IOException e)
        {
            throw e;
        }
        catch(RuntimeException e)
        {
            throw e;
        }
        catch(Throwable th)
        {
            throw new ServletException(th);
        }   
    }
    
    public void doPost(HttpServletRequest sreq, HttpServletResponse sres) 
    throws ServletException, IOException
    {
        doGet(sreq,sres);
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Exception Servlet";
    }    
}
