// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/* --------------------------------------------------------------------- */
/**
 *
 * @version $Id$
 * @author Greg Wilkins
 */
public interface SessionManager 
{
    /* ------------------------------------------------------------ */
    public final static String __SessionId  = "jsessionid";
    public final static String __SessionUrlPrefix = ";"+__SessionId+"=";
    
    /* ------------------------------------------------------------ */
    boolean isValid(HttpSession session);

    /* ------------------------------------------------------------ */
    HttpSession getHttpSession(String id);
    
    /* ------------------------------------------------------------ */
    HttpSession newSession();

    /* ------------------------------------------------------------ */
    void stop();

    /* ------------------------------------------------------------ */
    void access(HttpSession session);

    /* ------------------------------------------------------------ */
    void setSessionTimeout(int timeoutMinutes);

}
