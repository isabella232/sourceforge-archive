// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Servlet;
import com.mortbay.Util.Code;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.GenericServlet;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/* ------------------------------------------------------------ */
/** Test Servlet RequestDispatcher.
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class RequestDispatchTest extends HttpServlet
{
    /* ------------------------------------------------------------ */
    String pageType;

    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);
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
        String prefix = sreq.getContextPath()!=null
            ? sreq.getContextPath()+sreq.getServletPath()
            : sreq.getServletPath();
        
        sres.setContentType("text/html");

        String info ;

        if (sreq.getAttribute("javax.servlet.include.servlet_path")!=null)
            info=(String)sreq.getAttribute("javax.servlet.include.path_info");
        else
            info=sreq.getPathInfo();
        
        if (info==null)
            info="NULL";

        if (info.startsWith("/include/"))
        {
            info=info.substring(8);
            if (info.indexOf('?')<0)
                info+="?Dispatch=includeWriter";
            else
                info+="&Dispatch=includeWriter";

            if (System.currentTimeMillis()%2==0)
            {
                PrintWriter pout = sres.getWriter();
                pout.write("<H1>Include: "+info+"</H1><HL>");
                pout.flush();
                
                RequestDispatcher dispatch = getServletContext()
                    .getRequestDispatcher(info);
                dispatch.include(sreq,sres);
                
                pout.write("<HL><H1>-- Included (writer)</H1>");
            }
            else 
            {
                OutputStream out = sres.getOutputStream();
                PrintWriter pout = new PrintWriter(out);
                pout.write("<H1>Include: "+info+"</H1><HL>");
                pout.flush();
                
                RequestDispatcher dispatch = getServletContext()
                    .getRequestDispatcher(info);
                dispatch.include(sreq,sres);
                
                pout.write("<HL><H1>-- Included (outputstream)</H1>");
                pout.flush();
            }
        }
        else if (info.startsWith("/forward/"))
        {
            info=info.substring(8);
            if (info.indexOf('?')<0)
                info+="?Dispatch=forward";
            else
                info+="&Dispatch=forward";
            RequestDispatcher dispatch =
                getServletContext().getRequestDispatcher(info);
            if (dispatch!=null)
                dispatch.forward(sreq,sres);
            else
                sres.sendError(404);
        }
        else if (info.startsWith("/includeN/"))
        {
            info=info.substring(10);
            PrintWriter pout = sres.getWriter();
            pout.write("<H1>Include named: "+info+"</H1><HL>");
            pout.flush();

            RequestDispatcher dispatch = getServletContext()
                .getNamedDispatcher(info);
            if (dispatch!=null)
                dispatch.include(sreq,sres);
            else
                pout.write("<H1>No servlet named: "+info+"</H1>");

            pout.write("<HL><H1>Included ");
        }
        else if (info.startsWith("/forwardN/"))
        {
            info=info.substring(10);
            RequestDispatcher dispatch = getServletContext()
                .getNamedDispatcher(info);
            if (dispatch!=null)
                dispatch.forward(sreq,sres);
            else
            {
                PrintWriter pout = sres.getWriter();
                pout.write("<H1>No servlet named: "+info+"</H1>");
                pout.flush();
            }
        }
        else
        {
            PrintWriter pout = sres.getWriter();
            pout.write("<H1>Dispatch URL must be of the form: </H1>"+
                       "<PRE>"+prefix+"/include/path\n"+
                       prefix+"/forward/path\n"+
                       prefix+"/includeN/name\n"+
                       prefix+"/forwardN/name</PRE>"
                       );
            pout.flush();
        }
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Include Servlet";
    }

    /* ------------------------------------------------------------ */
    public synchronized void destroy()
    {
        Code.debug("Destroyed");
    }
    
}
