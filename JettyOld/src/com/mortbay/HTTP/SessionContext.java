// ========================================================================
// Copyright (c) 1998 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Base.Code;
import java.util.*;
import javax.servlet.http.*;

/* ------------------------------------------------------------ */
/** Http Session Context
 * <p>
 *
 * @version $Revision$ $Date$
 * @author Greg Wilkins (gregw)
 * @deprecated
 */
public class SessionContext extends Hashtable
    implements javax.servlet.http.HttpSessionContext	    
{
    public static final String SessionId  = "JettySessionId";
    public static final String SessionStatus  = "JettySessionStatus";
    public static final String OldSession  = "Old";
    public static final String OkSession  = "OK";
    public static final String NewSession  = "New";
    public static final int distantFuture = 60*60*24*7*52*20; 

    static long nextSessionId = System.currentTimeMillis();
    
    // Setting of max inactive interval for new sessions
    // -1 means no timeout
    private int defaultMaxIdleTime = -1;
    
    private SessionScavenger scavenger = null;
    
    /* ------------------------------------------------------------ */
    class Session extends Hashtable
	implements HttpSession
    {
	boolean invalid=false;
	boolean old=false;
	long created=System.currentTimeMillis();
	long accessed=created;
	long maxIdleTime = -1;
	String id=null;

	/* ------------------------------------------------------------- */
	Session()
	{
	    this.id=Long.toString(nextSessionId++,36);
	    put(SessionId,id);
	    put(SessionStatus,NewSession);
	}

	/* ------------------------------------------------------------- */
	Session(String sid)
	{
	    id=sid;
	    put(SessionId,id);
	    put(SessionStatus,OldSession);
	}
	
	/* ------------------------------------------------------------- */
	void accessed()
	{
	    old=true;
	    accessed=System.currentTimeMillis();
	    put(SessionStatus,OkSession);
	}
	
	/* ------------------------------------------------------------- */
	public long getCreationTime()
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    return created;
	}
	
	/* ------------------------------------------------------------- */
	public String getId()
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    return id;
	}
	
	/* ------------------------------------------------------------- */
	public long getLastAccessedTime()
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    return accessed;
	}
	
	/* ------------------------------------------------------------- */
	public int getMaxInactiveInterval()
	{
	    return (int)(maxIdleTime / 1000);
	}
	
	/* ------------------------------------------------------------- */
	/**
	 * @deprecated
	 */   
	public HttpSessionContext getSessionContext()
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    return SessionContext.this;
	}
	
	/* ------------------------------------------------------------- */
	public Object getValue(String name)
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    return get(name);
	}
	
	/* ------------------------------------------------------------- */
	public  String[] getValueNames()
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    synchronized(this)
	    {
		String[] a = new String[size()];
		Enumeration e = keys();
		int i=0;
		while (e.hasMoreElements())
		    a[i++]=(String)e.nextElement();
		return a;
	    }
	}
	
	/* ------------------------------------------------------------- */
	public void setMaxInactiveInterval(int i)
	{
	    maxIdleTime = (long)i * 1000;
	}
	
	/* ------------------------------------------------------------- */
	public void invalidate()
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    invalid=true;
	    
	    // Call valueUnbound on all the HttpSessionBindingListeners
	    // To avoid iterator problems, don't actually remove them
		Enumeration e = keys();
		while (e.hasMoreElements()) {
		    String key = (String)e.nextElement();
		    Object value = get(key);
		    unbindValue(key, value);
		   }
	    SessionContext.this.remove(id);
	}
	
	/* ------------------------------------------------------------- */
	public boolean isNew()
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    return !old;
	}
	
	/* ------------------------------------------------------------- */
	public void putValue(java.lang.String name,
			     java.lang.Object value)
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    Object oldValue = put(name,value);

		// Don't rebind if the value is unchanged
		// XXX Is this right or should we always unbind & bind?
	    if (value != oldValue) {
	    	unbindValue(name, oldValue);
	    	bindValue(name, value);
		}
	}
	
	/* ------------------------------------------------------------- */
	public void removeValue(java.lang.String name)
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    Object value=remove(name);
	    unbindValue(name, value);
	}

	/* ------------------------------------------------------------- */
	/** If value implements HttpSessionBindingListener, call valueBound() */
	private void bindValue(java.lang.String name, Object value)
	{
	    if (value!=null && value instanceof HttpSessionBindingListener)
		((HttpSessionBindingListener)value)
		    .valueBound(new HttpSessionBindingEvent(this,name));
	}

	/* ------------------------------------------------------------- */
	/** If value implements HttpSessionBindingListener, call valueUnbound() */
	private void unbindValue(java.lang.String name, Object value)
	{
	    if (value!=null && value instanceof HttpSessionBindingListener)
		((HttpSessionBindingListener)value)
		    .valueUnbound(new HttpSessionBindingEvent(this,name));
	}
	
    }	// Session

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */   
    public Enumeration getIds()
    {
	return keys();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */   
    public HttpSession getSession(String id)
    {
	return (HttpSession)get(id);
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession newSession()
    {
	HttpSession session = new Session();
	session.setMaxInactiveInterval(defaultMaxIdleTime);
	put(session.getId(),session);
	return session;
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession oldSession(String sessionId)
    {
	HttpSession session = new Session(sessionId);
	session.setMaxInactiveInterval(defaultMaxIdleTime);
	put(sessionId,session);
	return session;
    }

    /* ------------------------------------------------------------ */
    public static void access(HttpSession session)
    {
	((Session)session).accessed();
    }
    
    /* -------------------------------------------------------------- */
    /** Set the default session timeout.
     *	@param	default	The default timeout in seconds
     */
    public void setDefaultSessionMaxIdleTime(int defaultTime)
    {
    	defaultMaxIdleTime = defaultTime;
    	
    	// Start the session scavenger if we haven't already
    	if (scavenger == null)
	    scavenger = new SessionScavenger();
    }

    /* -------------------------------------------------------------- */
    /** Find sessions that have timed out and invalidate them. 
     *	This runs in the SessionScavenger thread.
     */
    private synchronized void scavenge()
    {
	// Set our priority high while we have the sessions locked
	int oldPriority = Thread.currentThread().getPriority();
	Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		
	long now = System.currentTimeMillis();
		
	// Since Hashtable enumeration is not safe over deletes,
	// we build a list of stale sessions, then go back and invalidate them
	Vector staleSessions = null;
		
	// For each session
	for (Enumeration e = elements(); e.hasMoreElements(); )
	{
	    Session session = (Session)e.nextElement();
	    long idleTime = session.maxIdleTime;
	    if (idleTime > 0 && session.accessed + idleTime < now) {
		// Found a stale session, add it to the list
		if (staleSessions == null)
		    staleSessions = new Vector();
		staleSessions.addElement(session);
	    }
	}
		
	// Remove the stale sessions
	if (staleSessions != null) {
	    for (int i = staleSessions.size() - 1; i >= 0; --i) {
		((Session)staleSessions.elementAt(i)).invalidate();
	    }
	}
		
	Thread.currentThread().setPriority(oldPriority);
    }

    final static int scavangeDelay = 60000;	
    /** SessionScavenger is a background thread that kills off old sessions */

    class SessionScavenger extends Thread
    {
	// how often to check

	public void run() {
	    while (true) {
		try {
		    sleep(scavangeDelay); 
		} catch (InterruptedException ex) {}
		SessionContext.this.scavenge();
	    }
	}

	SessionScavenger() {
	    super("SessionScavenger");
	    setPriority(Thread.MIN_PRIORITY);
	    setDaemon(true);
	    this.start();
	}
	
    }	// SessionScavenger

}



