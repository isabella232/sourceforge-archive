// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.HTTP.*;
import java.io.*;
import java.net.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.util.*;


/* ------------------------------------------------------------------------ */
/** Servlet Context wrapper class.
 * This class wraps the context provided by the HttpServer in order to
 * allow servlet specific resource bases 
 */
public class ServletContextWrapper implements javax.servlet.ServletContext
{
    HttpServer context;
    String resourceBase;

    /* ---------------------------------------------------------------- */
    public ServletContextWrapper(HttpServer context, String resourceBase)
    {
        this.context=context;
        this.resourceBase=resourceBase;
    }

    /* ---------------------------------------------------------------- */
    /**
     * Returns the servlet for the specified name.
     * @param name the name of the servlet
     * @return the Servlet, or null if not found
     * @exception ServletException if the servlet could not be initialized
     * @deprecated
     */
    public Servlet getServlet(String name)
    {
        return null;
    }

    /* ---------------------------------------------------------------- */
    /**
     * Enumerates the servlets in this context (server). Only servlets
     * that are accessible will be returned. The enumeration always
     * includes the servlet itself.
     * @deprecated Use getServletNames & getServlet
     */
    public Enumeration getServlets()
    {
        return null;
    }
    
    /* ---------------------------------------------------------------- */
    /**
     * @deprecated
     */
    public Enumeration getServletNames()
    {
        return null;
    }

    /* ---------------------------------------------------------------- */
    public javax.servlet.ServletContext getContext(String url)
    {
        // XXX this is wrong as it may be this context
        return context.getContext(url);
    }

    /* ---------------------------------------------------------------- */
    public int getMajorVersion()
    {
        return 2;
    }
    
    /* ---------------------------------------------------------------- */
    public int getMinorVersion()
    {
        return 1;
    }
    
    /* ------------------------------------------------------------ */
    /** Get a resource.
     * This is implemented with a standard URL instance that will make
     * a socket connection back to the server. This is less than
     * efficient and a better implementation is provided for
     * getResourceAsStream.
     * @param path URL path of resource
     * @return null
     * @exception MalformedURLException 
     */
    public URL getResource(String path)
        throws MalformedURLException
    {
        if (resourceBase!=null)
            return new URL(resourceBase+path);
        return context.getResource(path);
    }


    /* ------------------------------------------------------------ */
    /** Get a resource as a Stream.
     * Creates a simulated request to the local server and returns
     * the content of the simulated response.
     * @see getResource(String path)
     * @param path URL path of resource
     * @return InputStream
     */
    public InputStream getResourceAsStream(String path)
    {
        try {
            if (resourceBase!=null)
            {
                URL url = getResource(path);
                return url.openStream();
            }
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
        return context.getResourceAsStream(path);
    }
    
    /* ------------------------------------------------------------ */
    /** Get a RequestDispatcher.
     * @param path URL path of resource 
     * @return null
     */
    public RequestDispatcher getRequestDispatcher(String path)
    {
        return context.getRequestDispatcher(path);
    }
    
    /* ---------------------------------------------------------------- */
    /**
     * Writes a message to the servlet log file.
     * @param message the message to be written
     */
    public void log(String message)
    {
        context.log(message);
    }
    
    /* ---------------------------------------------------------------- */
    /**
     * Writes a message to the servlet log file.
     * @param message the message to be written
     * @param th Throwable 
     */
    public void log(String message, Throwable th)
    {
        Code.warning(message,th);
    }
    
    /* ---------------------------------------------------------------- */
    /**
     * Writes an exception & message to the servlet log file.
     * @param message the message to be written
     * @deprecated
     */
    public void log(Exception e, String message)
    {
        Code.warning(message,e);
    }

    /* ---------------------------------------------------------------- */
    /**
     * Applies alias rules to the specified virtual path and returns the
     * corresponding real path. Returns null if the translation could not
     * be performed.
     * @param path the real path to be translated
     */
    public String getRealPath(String path)
    {
        return context.getRealPath(path);
    }
    

    /* ---------------------------------------------------------------- */
    /**
     * Returns the mime type of the specified file, or null if not known.
     * @param file file name whose mime type is required
     */
    public String getMimeType(String file)
    {
        return context.getMimeType(file);
    }


    /* ---------------------------------------------------------------- */
    /**
     * Returns the name and version of the Web server under which the
     * servlet is running. Same as the CGI variable SERVER_SOFTWARE.
     */
    public String getServerInfo()
    {
        return Version.__jetty;
    }

    /* ---------------------------------------------------------------- */
    /**
     * Returns an attribute of the server given the specified key name.
     * This allows access to additional information about the server not
     * already provided by the other methods in this interface. Attribute
     * names should follow the same convention as package names, and those
     * beginning with 'com.sun.*' are reserved for use by Sun Microsystems.
     *
     * This implementation maps the attribute requests to properties of
     * HttpConfiguration.  The Jetty configuration does not adhere to
     * the naming conventions described above.
     * @param name the attribute key name
     * @return the value of the attribute, or null if not defined
     */
    public Object getAttribute(String name)
    {
        return context.getAttribute(name);
    }
    
    /* ---------------------------------------------------------------- */
    /** .
     * This implementation maps the attribute requests to properties of
     * HttpConfiguration.  The Jetty configuration does not adhere to
     * the naming conventions described above.
     */
    public Enumeration getAttributeNames()
    {
        return context.getAttributeNames();
    }
    
    /* ---------------------------------------------------------------- */
    /** .
     * This implementation maps the attribute requests to properties of
     * HttpConfiguration.  The Jetty configuration does not adhere to
     * the naming conventions described above.
     */
    public void setAttribute(String name, Object value)
    {
        context.setAttribute(name,value);
    }
    
    /* ---------------------------------------------------------------- */
    /** .
     * This implementation maps the attribute requests to properties of
     * HttpConfiguration.  The Jetty configuration does not adhere to
     * the naming conventions described above.
     */
    public void removeAttribute(String name)
    {
        context.removeAttribute(name);
    }
}
