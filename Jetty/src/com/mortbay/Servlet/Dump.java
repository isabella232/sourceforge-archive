// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Servlet;
import com.mortbay.HTML.Block;
import com.mortbay.HTML.Break;
import com.mortbay.HTML.Composite;
import com.mortbay.HTML.Element;
import com.mortbay.HTML.Font;
import com.mortbay.HTML.Heading;
import com.mortbay.HTML.Page;
import com.mortbay.HTML.Select;
import com.mortbay.HTML.Table;
import com.mortbay.HTML.TableForm;
import com.mortbay.HTTP.HttpException;
import com.mortbay.Util.Code;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import javax.servlet.UnavailableException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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

        String pi=sreq.getPathInfo();
        if (pi!=null)
        {
            if ("/ex0".equals(pi))
                throw new ServletException("test ex0",new Throwable());
            if ("/ex1".equals(pi))
                throw new IOException("test ex1");
            if ("/ex2".equals(pi))
                throw new UnavailableException("test ex2");
            if ("/ex3".equals(pi))
                throw new HttpException(501);
        }
        
        
        PrintWriter pout = sres.getWriter();
        Page page=null;

        try{
            page = new Page();
            page.title("Dump Servlet");     

            page.add(new Heading(1,"Dump Servlet"));
            Table table = new Table(0).cellPadding(0).cellSpacing(0);
            page.add(table);
            table.newRow();
            table.newHeading()
                .cell().nest(new Font(2,true))
                .add("Interface javax.servlet.* Request Methods")
                .attribute("COLSPAN","2")
                .left();

            table.newRow();
            table.addHeading("getMethod:&nbsp;").cell().right();
            table.addCell(""+sreq.getMethod());
            table.newRow();
            table.addHeading("getContentLength:&nbsp;").cell().right();
            table.addCell(Integer.toString(sreq.getContentLength()));
            table.newRow();
            table.addHeading("getContentType:&nbsp;").cell().right();
            table.addCell(""+sreq.getContentType());
            table.newRow();
            table.addHeading("getRequestURI:&nbsp;").cell().right();
            table.addCell(""+sreq.getRequestURI());
            table.newRow();
            table.addHeading("getContextPath:&nbsp;").cell().right();
            table.addCell(""+sreq.getContextPath());
            table.newRow();
            table.addHeading("getServletPath:&nbsp;").cell().right();
            table.addCell(""+sreq.getServletPath());
            table.newRow();
            table.addHeading("getPathInfo:&nbsp;").cell().right();
            table.addCell(""+sreq.getPathInfo());
            table.newRow();
            table.addHeading("getPathTranslated:&nbsp;").cell().right();
            table.addCell(""+sreq.getPathTranslated());
            table.newRow();
            table.addHeading("getQueryString:&nbsp;").cell().right();
            table.addCell(""+sreq.getQueryString());

            
            
            table.newRow();
            table.addHeading("getProtocol:&nbsp;").cell().right();
            table.addCell(""+sreq.getProtocol());
            table.newRow();
            table.addHeading("getScheme:&nbsp;").cell().right();
            table.addCell(""+sreq.getScheme());
            table.newRow();
            table.addHeading("getServerName:&nbsp;").cell().right();
            table.addCell(""+sreq.getServerName());
            table.newRow();
            table.addHeading("getServerPort:&nbsp;").cell().right();
            table.addCell(""+Integer.toString(sreq.getServerPort()));
            table.newRow();
            table.addHeading("getRemoteUser:&nbsp;").cell().right();
            table.addCell(""+sreq.getRemoteUser());
            table.newRow();
            table.addHeading("getRemoteAddr:&nbsp;").cell().right();
            table.addCell(""+sreq.getRemoteAddr());
            table.newRow();
            table.addHeading("getRemoteHost:&nbsp;").cell().right();
            table.addCell(""+sreq.getRemoteHost());            
            table.newRow();
            table.addHeading("getRequestedSessionId:&nbsp;").cell().right();
            table.addCell(""+sreq.getRequestedSessionId());            
                        
            table.newRow();
            table.addHeading("getLocales:&nbsp;").cell().right();
            Enumeration locales = sreq.getLocales();
            table.newCell();
            while(locales.hasMoreElements())
            {
                table.add(locales.nextElement());
                if (locales.hasMoreElements())
                    table.add(",&nbsp;");
            }

            table.newRow();
            table.newHeading()
                .cell().nest(new Font(2,true))
                .add("<BR>Interface javax.servlet.* Attributes")
                .attribute("COLSPAN","2")
                .left();
            String name;
            Enumeration a = sreq.getAttributeNames();
            while (a.hasMoreElements())
            {
                name=(String)a.nextElement();
                table.newRow();
                table.addHeading(name+":&nbsp;").cell().right();
                table.addCell(sreq.getAttribute(name));
            }
            
            table.newRow();
            table.newHeading()
                .cell().nest(new Font(2,true))
                .add("<BR>Other HTTP Headers")
                .attribute("COLSPAN","2")
                .left();
            Enumeration h = sreq.getHeaderNames();
            while (h.hasMoreElements())
            {
                name=(String)h.nextElement();
                table.newRow();
                table.addHeading(name+":&nbsp;").cell().right().top();
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
            TableForm tf = new TableForm(sres.encodeURL(sreq.getRequestURI()));
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
            Code.warning(e);
        }
    
        page.write(pout);
        pout.flush();
        if (pi!=null)
        {
            if ("/ex4".equals(pi))
                throw new ServletException("test ex4",new Throwable());
            if ("/ex5".equals(pi))
                throw new IOException("test ex5");
            if ("/ex6".equals(pi))
                throw new UnavailableException("test ex6");
            if ("/ex7".equals(pi))
                throw new HttpException(501);
        }
        
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Dump Servlet";
    }

    /* ------------------------------------------------------------ */
    public synchronized void destroy()
    {
        Code.debug("Destroyed");
    }
    
}
