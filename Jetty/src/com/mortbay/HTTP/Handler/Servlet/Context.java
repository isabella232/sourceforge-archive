// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler.Servlet;

import com.sun.java.util.collections.*;
import com.mortbay.HTTP.Handler.NullHandler;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.net.*;


/* --------------------------------------------------------------------- */
/** ServletHandler<p>
 * This handler maps requests to servlets that implement the
 * javax.servlet.http.HttpServlet API.
 * It is configured with a PathMap of paths to ServletHolder instances.
 *
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class Context implements ServletContext, HttpSessionContext
{
    /* ------------------------------------------------------------ */
    private ServletHandler _handler;
    ServletHandler getHandler(){return _handler;}

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param handler 
     */
    public Context(ServletHandler handler)
    {
	_handler=handler;
    }


    /* ------------------------------------------------------------ */
    /**
     * Returns a <code>String</code> containing the value of the named
     * context-wide initialization parameter, or <code>null</code> if the 
     * parameter does not exist.
     *
     * <p>This method can make available configuration information useful
     * to an entire "web application".  For example, it can provide a 
     * webmaster's email address or the name of a system that holds 
     * critical data.
     *
     * @param	name	a <code>String</code> containing the name of the
     *                  parameter whose value is requested
     * 
     * @return 		a <code>String</code> containing at least the 
     *			servlet container name and version number
     *
     * @see ServletConfig#getInitParameter
     */
    public String getInitParameter(String param)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns the names of the context's initialization parameters as an
     * <code>Enumeration</code> of <code>String</code> objects, or an
     * empty <code>Enumeration</code> if the context has no initialization
     * parameters.
     *
     * @return 		an <code>Enumeration</code> of <code>String</code> 
     *                  objects containing the names of the context's
     *                  initialization parameters
     *
     * @see ServletConfig#getInitParameter
     */
    public Enumeration getInitParameterNames()
    {
	Code.notImplemented();
	return null;
    }
    
    
    /* ------------------------------------------------------------ */
    String getRealPathInfo(String pathInfo)
    {
	String fileBase=_handler.getContext().getFileBase();
	if (fileBase==null)
	    return null;
	
	return
	    (fileBase+pathInfo).replace('/',File.separatorChar);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns a <code>ServletContext</code> object that 
     * corresponds to a specified URL on the server.
     *
     * <p>This method allows servlets to gain
     * access to the context for various parts of the server, and as
     * needed obtain {@link RequestDispatcher} objects from the context.
     * The given path must be absolute (beginning with "/") and is 
     * interpreted based on the server's document root. 
     * 
     * <p>In a security conscious environment, the servlet container may
     * return <code>null</code> for a given URL.
     *       
     * @param uripath 	a <code>String</code> specifying the absolute URL of 
     *			a resource on the server
     *
     * @return		the <code>ServletContext</code> object that
     *			corresponds to the named URL
     *
     * @see 		RequestDispatcher
     *
     */
    public ServletContext getContext(String uri)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public int getMajorVersion()
    {
	return 2;
    }
    
    /* ------------------------------------------------------------ */
    public int getMinorVersion()
    {
	return 1;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the MIME type of the specified file, or <code>null</code> if 
     * the MIME type is not known. The MIME type is determined
     * by the configuration of the servlet container, and may be specified
     * in a web application deployment descriptor. Common MIME
     * types are <code>"text/html"</code> and <code>"image/gif"</code>.
     *
     *
     * @param   file    a <code>String</code> specifying the name
     *			of a file
     *
     * @return 		a <code>String</code> specifying the file's MIME type
     *
     */
    public String getMimeType(String file)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public URL getResource(String uri)
	throws MalformedURLException
    {
	String resourceBase=_handler.getContext().getResourceBase();
	if (resourceBase==null)
	    return null;
	
	return new URL(resourceBase+uri);
    }
    
    /* ------------------------------------------------------------ */
    public InputStream getResourceAsStream(String uri)
    {
	try
	{
	    URL url = getResource(uri);
	    if (url!=null)
		return url.openStream();
	}
	catch(MalformedURLException e)
	{
	    Code.ignore(e);
	}
	catch(IOException e)
	{
	    Code.ignore(e);
	}
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     * Returns a {@link RequestDispatcher} object that acts
     * as a wrapper for the resource located at the given path.
     * A <code>RequestDispatcher</code> object can be used to forward 
     * a request to the resource or to include the resource in a response.
     * The resource can be dynamic or static.
     *
     * <p>The pathname must begin with a "/" and is interpreted as relative
     * to the current context root.  Use <code>getContext</code> to obtain
     * a <code>RequestDispatcher</code> for resources in foreign contexts.
     * This method returns <code>null</code> if the <code>ServletContext</code>
     * cannot return a <code>RequestDispatcher</code>.
     *
     * @param path 	a <code>String</code> specifying the pathname
     *			to the resource
     *
     * @return 		a <code>RequestDispatcher</code> object
     *			that acts as a wrapper for the resource
     *			at the specified path
     *
     * @see 		RequestDispatcher
     * @see 		ServletContext#getContext
     *
     */
    public RequestDispatcher getRequestDispatcher(String uri)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns a {@link RequestDispatcher} object that acts
     * as a wrapper for the named servlet.
     *
     * <p>Servlets (and JSP pages also) may be given names via server 
     * administration or via a web application deployment descriptor.
     * A servlet instance can determine its name using 
     * {@link ServletConfig#getServletName}.
     *
     * <p>This method returns <code>null</code> if the 
     * <code>ServletContext</code>
     * cannot return a <code>RequestDispatcher</code> for any reason.
     *
     * @param name 	a <code>String</code> specifying the name
     *			of a servlet to wrap
     *
     * @return 		a <code>RequestDispatcher</code> object
     *			that acts as a wrapper for the named servlet
     *
     * @see 		RequestDispatcher
     * @see 		ServletContext#getContext
     * @see 		ServletConfig#getServletName
     *
     */
    public RequestDispatcher getNamedDispatcher(String name)
    {
	Code.notImplemented();
	return null;
    }


    /* ------------------------------------------------------------ */
    public Servlet getServlet(String name)
    {
	Code.warning("No longer supported");
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getServlets()
    {
	Code.warning("No longer supported");
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getServletNames()
    {
	Code.warning("No longer supported");
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public void log(String msg)
    {
	Log.event(msg);
    }
    
    /* ------------------------------------------------------------ */
    public void log(Exception e, String msg)
    {
	Code.warning(msg,e);
    }
    
    /* ------------------------------------------------------------ */
    public void log(String msg, Throwable th)
    {
	Code.warning(msg,th);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns a <code>String</code> containing the real path 
     * for a given virtual path. For example, the virtual path "/index.html"
     * has a real path of whatever file on the server's filesystem would be
     * served by a request for "/index.html".
     *
     * <p>The real path returned will be in a form
     * appropriate to the computer and operating system on
     * which the servlet container is running, including the
     * proper path separators. This method returns <code>null</code>
     * if the servlet container cannot translate the virtual path
     * to a real path for any reason (such as when the content is
     * being made available from a <code>.war</code> archive).
     *
     *
     * @param path 	a <code>String</code> specifying a virtual path
     *
     *
     * @return 		a <code>String</code> specifying the real path,
     *                  or null if the translation cannot be performed
     *			
     *
     */
    public String getRealPath(String path)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public String getServerInfo()
    {
	return Version.__Version;
    }
    
    /* ------------------------------------------------------------ */
    /** Get context attribute.
     * Delegated to HandlerContext.
     */
    public Object getAttribute(String name)
    {
	return _handler.getContext().getAttribute(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Get context attribute names.
     * Delegated to HandlerContext.
     */
    public Enumeration getAttributeNames()
    {
	return _handler.getContext().getAttributeNames();
    }
    
    /* ------------------------------------------------------------ */
    /** Set context attribute names.
     * Delegated to HandlerContext.
     */
    public void setAttribute(String name, Object value)
    {
	if (name!=null && name.startsWith("com.mortbay."))
	    Code.warning("Servlet setting com.mortbay.* attribute: "+
			 name);
	_handler.getContext().setAttribute(name,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Remove context attribute.
     * Delegated to HandlerContext.
     */
    public void removeAttribute(String name)
    {
	if (name!=null && name.startsWith("com.mortbay."))
	    Code.warning("Servlet removing com.mortbay.* attribute: "+
			 name);
	_handler.getContext().removeAttribute(name);
    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public final static String __SessionUrlPrefix = ";sessionid=";
    public final static String __SessionUrlSuffix = "_";
    public final static String __SessionId  = "JettySessionId";
    public final static int __distantFuture = 60*60*24*7*52*20;
    private static long __nextSessionId = System.currentTimeMillis();
    
    // Setting of max inactive interval for new sessions
    // -1 means no timeout
    private int _defaultMaxIdleTime = -1;
    private SessionScavenger _scavenger = null;
    private Map _sessions = new HashMap();

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */   
    public Enumeration getIds()
    {
        return Collections.enumeration(_sessions.keySet());
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */   
    public HttpSession getSession(String id)
    {
        HttpSession s = (HttpSession)_sessions.get(id);
        return s;
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession newSession()
    {
        HttpSession session = new Session();
        session.setMaxInactiveInterval(_defaultMaxIdleTime);
        _sessions.put(session.getId(),session);
        return session;
    }

    /* ------------------------------------------------------------ */
    public static void access(HttpSession session)
    {
        ((Session)session).accessed();
    }
    
    /* ------------------------------------------------------------ */
    public static boolean isValid(HttpSession session)
    {
        return !(((Session)session).invalid);
    }
    
    /* -------------------------------------------------------------- */
    /** Set the default session timeout.
     *  @param  default The default timeout in seconds
     */
    public void setMaxInactiveInterval(int defaultTime)
    {   
        _defaultMaxIdleTime = defaultTime;
        
        // Start the session scavenger if we haven't already
        if (_scavenger == null)
            _scavenger = new SessionScavenger();
    }

    /* -------------------------------------------------------------- */
    /** Find sessions that have timed out and invalidate them. 
     *  This runs in the SessionScavenger thread.
     */
    private synchronized void scavenge()
    {
        // Set our priority high while we have the sessions locked
        int oldPriority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                
        long now = System.currentTimeMillis();
                
        // Since Hashtable enumeration is not safe over deletes,
        // we build a list of stale sessions, then go back and invalidate them
        ArrayList staleSessions = null;
                
        // For each session
        for (Iterator i = _sessions.values().iterator(); i.hasNext(); )
        {
            Session session = (Session)i.next();
            long idleTime = session.maxIdleTime;
            if (idleTime > 0 && session.accessed + idleTime < now) {
                // Found a stale session, add it to the list
                if (staleSessions == null)
                    staleSessions = new ArrayList(5);
                staleSessions.add(session);
            }
        }
                
        // Remove the stale sessions
        if (staleSessions != null)
	{
            for (int i = staleSessions.size() - 1; i >= 0; --i) {
                ((Session)staleSessions.get(i)).invalidate();
            }
        }
                
        Thread.currentThread().setPriority(oldPriority);
    }

    // how often to check - XXX - make this configurable
    final static int scavengeDelay = 30000;

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* -------------------------------------------------------------- */
    /** SessionScavenger is a background thread that kills off old sessions */
    class SessionScavenger extends Thread
    {
        public void run() {
            while (true) {
                try {
                    sleep(scavengeDelay); 
                } catch (InterruptedException ex) {}
                Context.this.scavenge();
            }
        }

        SessionScavenger() {
            super("SessionScavenger");
            setDaemon(true);
            this.start();
        }
        
    }   // SessionScavenger    


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    class Session implements HttpSession
    {
	HashMap _values = new HashMap(11);
        boolean invalid=false;
        boolean newSession=true;
        long created=System.currentTimeMillis();
        long accessed=created;
        long maxIdleTime = -1;
        String id=null;

        /* ------------------------------------------------------------- */
        Session()
        {
            synchronized(com.mortbay.HTTP.Handler.Servlet.Context.class)
            {
                long idtmp = __nextSessionId;
                __nextSessionId+=created%4096;
                this.id=Long.toString(idtmp,30+(int)(created%7));
            }
            if (_defaultMaxIdleTime>=0)
                maxIdleTime=_defaultMaxIdleTime*1000;
        }
        
        /* ------------------------------------------------------------- */
        void accessed()
        {
            newSession=false;
            accessed=System.currentTimeMillis();
        }
        
        /* ------------------------------------------------------------- */
        public String getId()
            throws IllegalStateException
        {
            if (invalid) throw new IllegalStateException();
            return id;
        }
        
        /* ------------------------------------------------------------- */
        public long getCreationTime()
            throws IllegalStateException
        {
            if (invalid) throw new IllegalStateException();
            return created;
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
            return Context.this;
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
            Iterator iter = _sessions.keySet().iterator();
            while (iter.hasNext())
            {
                String key = (String)iter.next();
                Object value = _values.get(key);
                unbindValue(key, value);
            }
            Context.this._sessions.remove(id);
        }
        
        /* ------------------------------------------------------------- */
        public boolean isNew()
            throws IllegalStateException
        {
            if (invalid) throw new IllegalStateException();
            return newSession;
        }
        

	/* ------------------------------------------------------------ */
	public Object getAttribute(String name)
	{
            if (invalid) throw new IllegalStateException();
            return _values.get(name);
	}

	/* ------------------------------------------------------------ */
	public Enumeration getAttributeNames()
	{
            if (invalid) throw new IllegalStateException();
	    return Collections.enumeration(_values.keySet());
	}
	
	/* ------------------------------------------------------------ */
	public void setAttribute(String name, Object value)
	{
            if (invalid) throw new IllegalStateException();
            Object oldValue = _values.put(name,value);

            if (value != oldValue)
            {
                unbindValue(name, oldValue);
                bindValue(name, value);
            }
	}
	
	/* ------------------------------------------------------------ */
	public void removeAttribute(String name)
	{
            if (invalid) throw new IllegalStateException();
            Object value=_values.remove(name);
            unbindValue(name, value);
	}
	
        /* ------------------------------------------------------------- */
	/**
	 * @deprecated 	As of Version 2.2, this method is
	 * 		replaced by {@link #getAttribute}
	 */
	public Object getValue(String name)
            throws IllegalStateException
        {
	    return getAttribute(name);
        }
        
        /* ------------------------------------------------------------- */
	/**
	 * @deprecated 	As of Version 2.2, this method is
	 * 		replaced by {@link #getAttributeNames}
	 */
        public synchronized String[] getValueNames()
            throws IllegalStateException
        {
            if (invalid) throw new IllegalStateException();
	    String[] a = new String[_values.size()];
	    return (String[])_values.keySet().toArray(a);
        }
        
        /* ------------------------------------------------------------- */
	/**
	 * @deprecated 	As of Version 2.2, this method is
	 * 		replaced by {@link #setAttribute}
	 */
        public void putValue(java.lang.String name,
                             java.lang.Object value)
            throws IllegalStateException
        {
	    setAttribute(name,value);
        }
        
        /* ------------------------------------------------------------- */
	/**
	 * @deprecated 	As of Version 2.2, this method is
	 * 		replaced by {@link #removeAttribute}
	 */
        public void removeValue(java.lang.String name)
            throws IllegalStateException
        {
	    removeAttribute(name);
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
	
    }   
}










