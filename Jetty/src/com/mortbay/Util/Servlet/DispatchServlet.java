// ========================================================================
// Copyright (c) 1999, 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util.Servlet;

import com.mortbay.Util.Code;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
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
    protected Class pageType;
    protected String name;
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);

        String pageTypeName = getInitParameter(Page.PageType);
        if (pageTypeName == null)
            pageType = Page.class;
        else {
            Throwable t = null;
            try {
                pageType = Class.forName(pageTypeName);
                Page pageTest = (Page)pageType.newInstance();
            } catch (RuntimeException ex){
                t = ex;
            } catch (LinkageError le){
                t = le;
            } catch (Exception ex){
                t = ex;
            }
            if (t != null)
                throw new ServletException("Bad Page Type:" +
                                           pageType.getName() + ":" +
                                           t.getMessage());
        }
    }
    /* ------------------------------------------------------------ */
    public Page getPage(Class type,
                        HttpServletRequest req,
                        HttpServletResponse res)
    {
        Page p = null;
        try {
            p = (Page)pageType.newInstance();
        } catch (Throwable t){
            Code.assert(false, "NEVER!");
        }
        p.properties().put(Page.Request, req);
        p.properties().put(Page.Response, res);
        return p;
    }
    /* ------------------------------------------------------------ */
    public void service(HttpServletRequest req, HttpServletResponse res) 
        throws ServletException, IOException
    {
        Page page = getPage(pageType, req, res);
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
            page = getPage(pageType, req, res);
            page.title("Exception Occurred...");
            page.nest(new Block(Block.Pre));
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            page.add(sw.toString());
        }
        if (page != null){
            res.setContentType("text/html");
            Writer writer = res.getWriter();
            page.write(writer);
            writer.flush();
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
        Page page = getPage(pageType, req, res);
        page.title("Unknown Method");
        page.add(new Heading(2, "Bad URL Path: \"" + method + "\""));
        return page;
    }
    /* ------------------------------------------------------------ */
}

