// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.util.EventListener;
import javax.servlet.http.HttpSession;
import org.mortbay.util.LifeCycle;

    
/* --------------------------------------------------------------------- */
/**
 *
 * @version $Id$
 * @author Greg Wilkins
 */
public interface SessionManager extends LifeCycle
{
    /* ------------------------------------------------------------ */
    public final static String __SessionId  = "jsessionid";
    public final static String __SessionUrlPrefix = ";"+__SessionId+"=";
    
    /* ------------------------------------------------------------ */
    public HttpSession getHttpSession(String id);
    
    /* ------------------------------------------------------------ */
    public HttpSession newHttpSession();

    /* ------------------------------------------------------------ */
    public void setSessionTimeout(int timeoutMinutes);

    /* ------------------------------------------------------------ */
    /** Add an event listener.
     * @param listener An Event Listener. Individual SessionManagers
     * implemetations may accept arbitrary listener types, but they
     * are expected to at least handle
     *   HttpSessionActivationListener,
     *   HttpSessionAttributeListener,
     *   HttpSessionBindingListener,
     *   HttpSessionListener
     * @exception IllegalArgumentException If an unsupported listener
     * is passed.
     */
    public void addEventListener(EventListener listener)
        throws IllegalArgumentException;
    
    /* ------------------------------------------------------------ */
    public void removeEventListener(EventListener listener);
    
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public interface Session extends HttpSession
    {
        /* ------------------------------------------------------------ */
        public boolean isValid();

        /* ------------------------------------------------------------ */
        public void access();
    }
    
}
