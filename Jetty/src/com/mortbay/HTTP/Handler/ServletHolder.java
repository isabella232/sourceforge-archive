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
public abstract class ServletHolder
{
    /* ------------------------------------------------------------ */
    String _name ;
    public String getName()
    {
	return _name;
    }
    public void setName(String name)
    {
	_name = name;
    }

    /* ------------------------------------------------------------ */
    String _className ;
    public String getClassName()
    {
	return _className;
    }
    public void setClassName(String className)
    {
	_className = className;
    }

    /* ------------------------------------------------------------ */
    Map _properties ;
    public Map getProperties()
    {
	return _properties;
    }
    public void setProperties(Map properties)
    {
	_properties = properties;
    }

    /* ------------------------------------------------------------ */
    protected ServletHolder(String className)
    {
	_className=className;
    }
    
    /* ------------------------------------------------------------ */
    public abstract void initialize();
   

}

 
