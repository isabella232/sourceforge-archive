// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.PropertyTree;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;


/* --------------------------------------------------------------------- */
/** Null HttpHandler
 * Conveniance base class with null handlers for all methods
 *
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class NullHandler implements HttpHandler 
{
    /* ----------------------------------------------------------------- */
    protected HttpServer httpServer=null;

    public void setProperties(Properties p)
	throws IOException
    {
	Code.notImplemented();
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
			 HttpResponse response)
	 throws Exception
    {}
    
    /* ----------------------------------------------------------------- */
    public String translate(String path)
    {
	return path;
    }
	 
    /* ----------------------------------------------------------------- */
    public Enumeration servletNames()
    {
	return null;
    } 
	
    /* ----------------------------------------------------------------- */
    public Servlet servlet(String name)
    {
	return null;
    } 

    /* ------------------------------------------------------------ */
    public void setServer(HttpServer server)
	 throws Exception
    {
	httpServer=server;
    }

    /* ------------------------------------------------------------ */
    /** Exract property sub tree.
     * Extract sub tree from file name PROPERTIES key merged with property
     * tree below PROPERTY key.
     * @param props PropertyTree
     * @return PropertyTree
     */
    protected static PropertyTree getProperties(PropertyTree props)
	throws IOException, FileNotFoundException
    {
	PropertyTree properties=new PropertyTree();
	String filename = props.getProperty("PROPERTIES");
	if (filename!=null&&filename.length()>0)
	{
	    Code.debug("Load ",filename);
	    properties.load(new BufferedInputStream(new FileInputStream(filename)));
	}
	PropertyTree property= props.getTree("PROPERTY");
	if (property!=null)
	    properties.load(property);
	return properties;
    }
    
    
}



