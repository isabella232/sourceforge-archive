// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;

import com.sun.java.util.collections.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.net.URL;


/* --------------------------------------------------------------------- */
/**
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public abstract class ServletHandler extends NullHandler 
{
    /* ------------------------------------------------------------ */
    private String _classPath ;
    public String getClassPath()
    {
	return _classPath;
    }
    public void setClassPath(String classPath)
    {
	_classPath = classPath;
    }

    /* ------------------------------------------------------------ */
    private String _fileBase ;
    public String getFileBase()
    {
	return _fileBase;
    }

    /* ------------------------------------------------------------ */
    /**
     * The '/' separator is converted to platform specific file
     * separator character.
     * @param fileBase 
     */
    public void setFileBase(String fileBase)
    {
	_fileBase = fileBase.replace('/',File.separatorChar);;
    }

    /* ------------------------------------------------------------ */
    private String _resourceBase ;
    public String getResourceBase()
    {
	return _resourceBase;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * If a relative file is passed, it is converted to a file
     * URL based on the current working directory.
     * @param resourceBase 
     */
    public void setResourceBase(String resourceBase)
    {
	if (resourceBase!=null)
	{
	    if( resourceBase.startsWith("./"))
		_resourceBase =
		    "file:"+
		    System.getProperty("user.dir")+
		    resourceBase.substring(1);
	    else if (resourceBase.startsWith("/"))
		_resourceBase = "file:"+resourceBase;
	    else
		_resourceBase = resourceBase;
	}
	else
	    _resourceBase = resourceBase;
    }
    
    /* ------------------------------------------------------------ */
    private boolean _autoReload ;
    public boolean isAutoReload()
    {
	return _autoReload;
    }
    public void setAutoReload(boolean autoReload)
    {
	_autoReload = autoReload;
    }
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @param path 
     * @param servletClass 
     */
    public abstract ServletHolder addServlet(String pathSpec,
					     String servletClass);
}








