// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
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
    /* ----------------------------------------------------------------- */
    public SessionHandler()
    {}
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {
	HttpSession session = request.getSession(false);
	if (session!=null)
	    // XXX - Will have to use private interface?
	    SessionContext.access(session);
    }

    /* ----------------------------------------------------------------- */
    public static Dictionary session(HttpServletRequest request)
    {
	return (Dictionary)request.getSession(true);
    }
    
}




