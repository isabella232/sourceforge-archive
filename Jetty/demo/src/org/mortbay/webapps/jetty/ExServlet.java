// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.webapps.jetty;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
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
    static int unavailable;
    static long started=System.currentTimeMillis();
    
    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#init()
     */
    public void init() throws ServletException
    {
        if (System.currentTimeMillis()-started<30000)
        {
            unavailable++;
            throw new UnavailableException("Test unavail "+unavailable,10);
        }
    }
    
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest sreq, HttpServletResponse sres) 
        throws ServletException, IOException
    {
        String info=sreq.getPathInfo();
        try
        {
            String except = info.substring(1);
            if (Character.isDigit(except.charAt(0)))
            {
                int sc = Integer.parseInt(except);
                sres.sendError(sc);
            }
            else if ("nestedSE".equals(except))
            {
                Exception ex = new Exception("InnerInner");
                ServletException se0=new ServletException(ex);
                ServletException se1=new ServletException(se0);
                throw se1;
            }
            else
                throw (Throwable)(Loader.loadClass(this.getClass(),except).newInstance());
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

    /* ------------------------------------------------------------ */
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
