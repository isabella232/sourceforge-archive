// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.servlet;
import java.io.IOException;
import java.io.PrintWriter;
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

/* ------------------------------------------------------------ */
/** Dump Servlet Request.
 * 
 */
public class Dump extends HttpServlet
{
    /* ------------------------------------------------------------ */
    String pageType;
    String initParams="";

    /* ------------------------------------------------------------ */
    public void init(ServletConfig config)
         throws ServletException
    {
        super.init(config);
        Enumeration e=getInitParameterNames();
        while(e.hasMoreElements())
        {
            String name=(String)e.nextElement();
            initParams+=name+"="+getInitParameter(name)+" ";
        }
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
        String info=sreq.getPathInfo();
        if (info!=null && info.endsWith("Exception"))
        {
            try
            {
                throw (Throwable)(Class.forName(info.substring(1)).newInstance());
            }
            catch(Throwable th)
            {
                throw new ServletException(th);
            }
        }
        
        sres.setContentType("text/html");

        if (info!=null && info.indexOf("Locale/")>=0)
        {
            try
            {
                String locale_name=info.substring(info.indexOf("Locale/")+7);
                Field f=java.util.Locale.class.getField(locale_name);
                sres.setLocale((Locale)f.get(null));
            }
            catch(Exception e)
            {
                Code.ignore(e);
                sres.setLocale(Locale.getDefault());
            }
        }

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
            table.addHeading("getInitParams:&nbsp;").cell().right();
            table.addCell(initParams);            
                        
            table.newRow();
            table.addHeading("getLocale:&nbsp;").cell().right();
            table.addCell(""+sreq.getLocale());            
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

	    /* ------------------------------------------------------------ */
            table.newRow();
            table.newHeading()
                .cell().nest(new Font(2,true))
                .add("<BR>Request Attributes")
                .attribute("COLSPAN","2")
                .left();
            String name;
            Enumeration a = sreq.getAttributeNames();
            while (a.hasMoreElements())
            {
                name=(String)a.nextElement();
                table.newRow();
                table.addHeading(name+":&nbsp;")
		    .cell().attribute("VALIGN","TOP").right();
		table.addCell("<pre>" +
			      toString(sreq.getAttribute(name))
			      + "</pre>");
            }
            
            table.newRow();
            table.newHeading()
                .cell().nest(new Font(2,true))
                .add("<BR>Context Attributes")
                .attribute("COLSPAN","2")
                .left();
            a = getServletContext().getAttributeNames();
            while (a.hasMoreElements())
            {
                name=(String)a.nextElement();
                table.newRow();
                table.addHeading(name+":&nbsp;")
		    .cell().attribute("VALIGN","TOP").right();
                table.addCell("<pre>" +
			      toString(getServletContext()
				       .getAttribute(name))
			      + "</pre>");
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
                table.addHeading(name+":&nbsp;").cell().right();
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
                if (values==null)
                {
                    table.newRow();
                    table.addHeading(name+" Values:&nbsp;")
                        .cell().right();
                    table.addCell("NULL!!!!!!!!!");
                }
                else
                if (values.length>1)
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
    
    /* ------------------------------------------------------------ */
    private static String toString(Object o)
    {
	if (o == null)
	    return null;

	if (o.getClass().isArray())
	{
	    StringBuffer sb = new StringBuffer();
	    Object[] array = (Object[]) o;
	    for (int i=0; i<array.length; i++)
	    {
		if (i > 0)
		    sb.append("\n");
		sb.append(array.getClass().getComponentType().getName());
		sb.append("[");
		sb.append(i);
		sb.append("]=");
		sb.append(toString(array[i]));
	    }
	    return sb.toString();
	}
	else
	    return o.toString();
    }
    
}
