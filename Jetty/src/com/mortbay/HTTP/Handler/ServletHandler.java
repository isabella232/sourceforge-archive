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
    /** 
     * @param path 
     * @param servletClass 
     */
    public abstract void addServlet(String pathSpec,
				    String servletClass);

    
    public abstract void addServlet(String pathSpec,
				    String servletName,
				    String servletClass,
				    Map properties,
				    boolean initialize);
}
