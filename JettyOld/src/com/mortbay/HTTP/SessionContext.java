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
 */
public class SessionContext extends Hashtable
    implements javax.servlet.http.HttpSessionContext	    
{
    public static final String BrowserId  = "JettyBrowserId";
    public static final String SessionId  = "JettySessionId";
    public static final String SessionStatus  = "JettySessionStatus";
    public static final String OldSession  = "Old";
    public static final String OkSession  = "OK";
    public static final String NewSession  = "New";
    public static final int distantFuture = 60*60*24*7*52*20; 

    static long nextSessionId = System.currentTimeMillis();
    static long nextBrowserId = System.currentTimeMillis();
    
    /* ------------------------------------------------------------ */
    class Session extends Hashtable
	implements HttpSession
    {
	boolean invalid=false;
	boolean old=false;
	long created=System.currentTimeMillis();
	long accessed=created;
	String id=null;

	/* ------------------------------------------------------------- */
	Session()
	{
	    this.id=Long.toString(nextSessionId++,36);
	    put(SessionId,id);
	    put(BrowserId,Long.toString(nextBrowserId++,36));
	    put(SessionStatus,NewSession);
	}

	/* ------------------------------------------------------------- */
	Session(String sid,String bid)
	{
	    id=sid;
	    put(SessionId,id);
	    put(BrowserId,bid);
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
	public HttpSessionContext getSessionContext()
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    return HttpSessionContext.this;
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
	public void invalidate()
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    invalid=true;
	    SessionContext.this.remove(Session.this);
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
	    put(name,value);
	    if (value !=null && value instanceof HttpSessionBindingListener)
		((HttpSessionBindingListener)value)
		    .valueBound(new HttpSessionBindingEvent(this,name));
	}
	
	/* ------------------------------------------------------------- */
	public void removeValue(java.lang.String name)
	    throws IllegalStateException
	{
	    if (invalid) throw new IllegalStateException();
	    Object value=remove(name);
	    if (value!=null && value instanceof HttpSessionBindingListener)
		((HttpSessionBindingListener)value)
		    .valueUnbound(new HttpSessionBindingEvent(this,name));
	}
    };

    /* ------------------------------------------------------------ */
    public Enumeration getIds()
    {
	return keys();
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession getSession(String id)
    {
	return (HttpSession)get(id);
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession newSession()
    {
	HttpSession session = new Session();
	put(session.getId(),session);
	return session;
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession oldSession(String sessionId,
				  String browserId)
    {
	HttpSession session = new Session(sessionId,browserId);
	put(sessionId,session);
	return session;
    }

    /* ------------------------------------------------------------ */
    public static void access(HttpSession session)
    {
	((Session)session).accessed();
    }
    
};
