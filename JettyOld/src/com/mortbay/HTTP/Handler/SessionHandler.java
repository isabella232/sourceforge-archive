// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.PropertyTree;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;

/* --------------------------------------------------------------------- */
/** Session Handler
 * Implements a combinations of the original Jetty session API and
 * the JDK1.2 standard sessions.
 */
public class SessionHandler extends NullHandler
{    
    
    /* ------------------------------------------------------------ */
    /** Constructor from properties.
     * Calls setProperties.
     * @param properties Configuration properties
     */
    public SessionHandler(Properties properties)
    {
        setProperties(properties);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public SessionHandler()
    {}
    
    /* ------------------------------------------------------------ */
    /** Configure from properties.
     * No configuration parameters for this handler
     * @param properties configuration.
     */
    public void setProperties(Properties properties)
    {
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
                       HttpResponse response)
         throws Exception
    {
        HttpSession session = request.getSession(false);
        if (session!=null)
            SessionContext.access(session);
    }

    /* ----------------------------------------------------------------- */
    public static Dictionary session(HttpServletRequest request)
    {
        return (Dictionary)request.getSession(true);
    }
    
}




