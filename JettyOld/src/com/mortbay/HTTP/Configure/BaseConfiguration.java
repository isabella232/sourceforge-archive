// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Configure;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import java.util.*;


/** Base Http Configuration
 * <p> This implementation of HttpConfiguration provides a minimal
 * implementation of all methods and is a good base for real
 * configuration classes
 *
 * @see com.mortbay.HTTP.HttpConfiguration
 * @version $Id$
 * @author Greg Wilkins
*/
public class BaseConfiguration implements HttpConfiguration
{

    /* ------------------------------------------------------------ */
    protected Hashtable mimeMap = null;
    protected InetAddrPort[] addresses = null;
    protected Class[] listenerClasses = null;
    protected PathMap httpHandlersMap = null;
    protected PathMap exceptionHandlersMap=null;
    protected Properties properties=null;
    
    /**
     * @deprecated use properties
     */
    protected Hashtable attributes=new Hashtable();

    /* ------------------------------------------------------------ */
    public String toString()
    {
        StringBuffer buf = new StringBuffer(1024);
        buf.append(DataClass.toString(addresses));
        buf.append("\n");
        buf.append(httpHandlersMap);
        buf.append("\n");
        buf.append(exceptionHandlersMap);
        buf.append("\n");
        buf.append(properties);
        return buf.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** The IP addresses and ports the HTTP server listens on
     */
    public InetAddrPort[] addresses()
    {
        return addresses;
    }

    /* ------------------------------------------------------------ */
    /** The HttpListener classes.
     * The classes derived from HttpListener (or HttpListener) used
     * to listen to the corresponding address from addresses().
     */
    public Class[] listenerClasses()
    {
        if (listenerClasses==null)
        {
            listenerClasses = new Class[addresses.length];
            for (int i=listenerClasses.length;i-->0;)
                listenerClasses[i]=com.mortbay.HTTP.HttpListener.class;
        }
        return listenerClasses;
    }

    /* ------------------------------------------------------------ */
    /** The HttpHandlers
     */
    public PathMap httpHandlersMap()
    {
        return httpHandlersMap;
    }
    
    
    /* ------------------------------------------------------------ */
    /** The ExceptionHandlers
     */
    public PathMap exceptionHandlersMap()
    {
        if (exceptionHandlersMap==null)
        {
            exceptionHandlersMap = new PathMap();
            ExceptionHandler[] exceptionHandlers = new ExceptionHandler[1];
            exceptionHandlers[0] = new DefaultExceptionHandler();
            exceptionHandlersMap.put("/",exceptionHandlers);
        }
        return exceptionHandlersMap ;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Translate Mime type
     */
    public String getMimeType(String file)
    {
        if (file!=null)
        {
            int i = file.lastIndexOf(".");
            if (i>0)
            {
                String ext = file.substring(i+1);
                if (ext!=null && ext.length()>0)
                    return getMimeByExtension(ext);
            }
        }
        return getMimeByExtension("default");
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns an attribute of the server given the specified key name.
     * @deprecated Use getProperty
     */
    public Object getAttribute(String name)
    {
        if (attributes!=null)
        {
            Object a = attributes.get(name);
            if (null != a)
                return a;
        }
        if (properties==null)
            return null;
        return properties.get(name);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns an attribute of the server given the specified key name.
     */
    public String getProperty(String name)
    {
        if (properties==null)
            return null;
        return properties.getProperty(name);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns properties object.
     */
    public Properties getProperties()
    {
        if (properties==null)
            properties=new Properties();
        return properties;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Writes a message to the servlet log file.
     */
    public void log(String message)
    {
        Log.event(message,4);
    }
    
    /* ------------------------------------------------------------ */
    /** Lookup mime type by filename extension.
     * This class provides a basic mapping of class, html, txt,
     * java, gif, jpg and default extensions.   Derived classes should
     * specialize this to provide a more complete mime mapping
     */
    protected String getMimeByExtension(String ext)
    {
        ext=ext.toLowerCase();
        
        if (mimeMap==null)
        {
            mimeMap = new Hashtable();
            mimeMap.put("default","application/octet-stream");
            mimeMap.put("class","application/octet-stream");
            mimeMap.put("html","text/html");
            mimeMap.put("htm","text/html");
            mimeMap.put("txt","text/plain");
            mimeMap.put("java","text/plain");
            mimeMap.put("gif","image/gif");
            mimeMap.put("jpg","image/jpeg");
            mimeMap.put("jpeg","image/jpeg");
            mimeMap.put("au","audio/basic");
            mimeMap.put("snd","audio/basic");
            mimeMap.put("ra","audio/x-pn-realaudio");
            mimeMap.put("ram","audio/x-pn-realaudio");
            mimeMap.put("rm","audio/x-pn-realaudio");
            mimeMap.put("rpm","audio/x-pn-realaudio");
            mimeMap.put("mov","video/quicktime");
            mimeMap.put("jsp","text/plain");
        }
        
        String type = (String)mimeMap.get(ext);
        if (type==null)
            type = (String)mimeMap.get("default");

        return type;
    }

};






