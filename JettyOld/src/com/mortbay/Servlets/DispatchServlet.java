// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Servlets;

import com.mortbay.Base.Code;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.mortbay.HTML.Page;
import com.mortbay.HTML.Heading;
import com.mortbay.HTML.Block;

public class DispatchServlet extends HttpServlet
    implements ServletDispatchHandler
{
    /* ------------------------------------------------------------ */
    public DispatchServlet(String name){
        this.name = name;
    }
    /* ------------------------------------------------------------ */
    protected String lookAndFeelName;
    protected String name;
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);

        lookAndFeelName = getInitParameter(Page.PageType);
        if (lookAndFeelName == null)
            lookAndFeelName = Page.getDefaultPageType();
    }
    /* ------------------------------------------------------------ */
    public void service(HttpServletRequest req, HttpServletResponse res) 
        throws ServletException, IOException
    {
        Page page = Page.getPage(lookAndFeelName, req, res);
        try {
            try {
                ServletDispatch disp = new ServletDispatch(req, res);
                page = (Page)disp.dispatch(this, page);
            } catch (java.lang.reflect.InvocationTargetException ex){
                Throwable t = ex;
                while (t instanceof
                       java.lang.reflect.InvocationTargetException){
                    t = ((java.lang.reflect.InvocationTargetException)t)
                        .getTargetException();
                }
                throw t;
            }
        } catch (Throwable e) {
            Code.debug(e);
            page = Page.getPage(lookAndFeelName, req, res);
            page.title("Exception Occurred...");
            page.nest(new Block(Block.Pre));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            page.add(sw.toString());
        }
        if (page != null){
            res.setContentType("text/html");
            OutputStream out = res.getOutputStream();
            PrintWriter pout = new PrintWriter(out);
            page.write(pout);
            pout.flush();
        }
    }
    /* ------------------------------------------------------------ */
    public String getServletInfo() {
        return name;
    }
    /* ------------------------------------------------------------ */
    public Object defaultDispatch(String method,
                                  ServletDispatch dispatch,
                                  Object context,
                                  HttpServletRequest req,
                                  HttpServletResponse res)
        throws Exception
    {
        Page page = Page.getPage(lookAndFeelName, req, res);
        page.title("Unknown Method");
        page.add(new Heading(2, "Bad URL Path: \"" + method + "\""));
        return page;
    }
    /* ------------------------------------------------------------ */
};

