// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Locale;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.html.Break;
import org.mortbay.html.Composite;
import org.mortbay.html.Element;
import org.mortbay.html.Font;
import org.mortbay.html.Form;
import org.mortbay.html.Heading;
import org.mortbay.html.Page;
import org.mortbay.html.Select;
import org.mortbay.html.Table;
import org.mortbay.html.TableForm;
import org.mortbay.http.HttpException;
import org.mortbay.util.Code;
import org.mortbay.util.URI;
import org.mortbay.util.Loader;

/* ------------------------------------------------------------ */
/** Dump Servlet Request.
 * 
 */
public class SendRedirect extends HttpServlet
{
    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException
    {
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache,no-store");

        String url=request.getParameter("URL");
        if (url!=null && url.length()>0)
        {
            response.sendRedirect(url);
        }
        else
        {
            PrintWriter pout = response.getWriter();
            Page page=null;
            
            try{
                page = new Page();
                page.title("SendRedirect Servlet");     
                
                page.add(new Heading(1,"SendRedirect Servlet"));
                
                page.add(new Heading(1,"Form to generate Dump content"));
                TableForm tf = new TableForm
                    (response.encodeURL(URI.addPaths(request.getContextPath(),
                                                     request.getServletPath())+
                                        "/action"));
                tf.method("GET");
                tf.addTextField("URL","URL",40,request.getContextPath()+"/dump");
                tf.addButton("Redirect","Redirect");
                page.add(tf);
                page.write(pout);
                pout.close();
            }
            catch (Exception e)
            {
                Code.warning(e);
            }
        }
    }

}
