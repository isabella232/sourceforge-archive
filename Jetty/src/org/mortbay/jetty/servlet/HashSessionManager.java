// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import org.mortbay.util.Code;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.ConcurrentModificationException;
import java.util.EventListener;

public class HashSessionManager implements SessionManager
{
    /* ------------------------------------------------------------ */
    public final static int __distantFuture = 60*60*24*7*52*20;
    private static long __nextSessionId = System.currentTimeMillis();
    
    /* ------------------------------------------------------------ */
    // Setting of max inactive interval for new sessions
    // -1 means no timeout
    private int _dftMaxIdleSecs = -1;
    private SessionScavenger _scavenger = null;
    private Map _sessions;
    private int _scavengeDelay = 30000;
    private ServletHandler _handler;

    /* ------------------------------------------------------------ */
    HashSessionManager(ServletHandler handler)
    {
        _handler=handler;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param id
     * @return
     */
    public HttpSession getHttpSession(String id)
    {
        HttpSession s = (HttpSession)_sessions.get(id);
        return s;
    }

    /* ------------------------------------------------------------ */
    public synchronized HttpSession newHttpSession()
    {
        HttpSession session = new Session();
        session.setMaxInactiveInterval(_dftMaxIdleSecs);
        _sessions.put(session.getId(),session);
        return session;
    }

    /* -------------------------------------------------------------- */
    /** Set the default session timeout.
     *  @param  default The timeout in minutes
     */
    public synchronized void setSessionTimeout(int timeoutMinutes)
    {
        _dftMaxIdleSecs = timeoutMinutes*60;;

        // Adjust scavange delay to 25% of timeout
        _scavengeDelay=_dftMaxIdleSecs*250;
        if (_scavengeDelay>60000)
            _scavengeDelay=60000;
        
        // Start the session scavenger if we haven't already
        if (_scavenger == null && _scavengeDelay>0)
            _scavenger = new SessionScavenger();
    }

    public void addEventListener(EventListener listener)
    {
        // XXX
        Code.notImplemented();
    }
    
    /* ------------------------------------------------------------ */
    public boolean isStarted()
    {
        return _scavenger!=null;
    }
    
    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        if (_sessions==null)
            _sessions=new HashMap();
        
        // Start the session scavenger if we haven't already
        if (_scavenger == null)
            _scavenger = new SessionScavenger();
    }
    
    /* ------------------------------------------------------------ */
    public void stop()
    {
        // Invalidate all sessions to cause unbind events
        ArrayList sessions = new ArrayList(_sessions.values());
        for (Iterator i = sessions.iterator(); i.hasNext(); )
        {
            Session session = (Session)i.next();
            session.invalidate();
        }
        _sessions.clear();
        
        // stop the scavenger
        SessionScavenger scavenger = _scavenger;
        _scavenger=null;
        if (scavenger!=null)
            scavenger.interrupt();
    }


    /* ------------------------------------------------------------ */
    public boolean isDestroyed()
    {
        return _sessions==null;
    }
    
    /* ------------------------------------------------------------ */
    public void destroy()
    {
        stop();
        _sessions=null;
    }
    
    /* -------------------------------------------------------------- */
    /** Find sessions that have timed out and invalidate them.
     *  This runs in the SessionScavenger thread.
     */
    private void scavenge()
    {
        long now = System.currentTimeMillis();

        // Since Hashtable enumeration is not safe over deletes,
        // we build a list of stale sessions, then go back and invalidate them
        ArrayList staleSessions = null;

        // For each session
        try
        {
            for (Iterator i = _sessions.values().iterator(); i.hasNext(); )
            {
                Session session = (Session)i.next();
                long idleTime = session._maxIdleMs;
                if (idleTime > 0 && session._accessed + idleTime < now) {
                    // Found a stale session, add it to the list
                    if (staleSessions == null)
                        staleSessions = new ArrayList(5);
                    staleSessions.add(session);
                }
            }
        }
        catch(ConcurrentModificationException e)
        {
            Code.ignore(e);
            // Oops something changed while we were looking.
            // Lock the context and try again.
            // Set our priority high while we have the sessions locked
            int oldPriority = Thread.currentThread().getPriority();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            try
            {
                synchronized(this)
                {
                    staleSessions=null;
                    scavenge();
                }
            }
            finally {Thread.currentThread().setPriority(oldPriority);}
        }

        // Remove the stale sessions
        if (staleSessions != null)
        {
            for (int i = staleSessions.size() - 1; i >= 0; --i) {
                ((Session)staleSessions.get(i)).invalidate();
            }
        }
    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* -------------------------------------------------------------- */
    /** SessionScavenger is a background thread that kills off old sessions */
    class SessionScavenger extends Thread
    {
        public void run() {
            while (isStarted())
            {
                try {
                    sleep(_scavengeDelay);
                    HashSessionManager.this.scavenge();
                }
                catch (InterruptedException ex) { break; }
                catch (Exception ex) {Code.warning(ex);}
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
    class Session implements SessionManager.Session
    {
        HashMap _values = new HashMap(11);
        boolean _invalid=false;
        boolean _newSession=true;
        long _created=System.currentTimeMillis();
        long _accessed=_created;
        long _maxIdleMs = -1;
        String id=null;

        /* ------------------------------------------------------------- */
        Session()
        {
            synchronized(Session.class)
            {
                do
                {
                    long newId = __nextSessionId;
                    __nextSessionId+=this.hashCode();
                    if (newId<0)newId=-newId;
                    this.id=Long.toString(newId,30+(int)(_created%7));
                }
                while (_sessions.containsKey(this.id));
            }
            if (_dftMaxIdleSecs>=0)
                _maxIdleMs=_dftMaxIdleSecs*1000;
        }


        /* ------------------------------------------------------------ */
        public void access()
        {
            _newSession=false;
            _accessed=System.currentTimeMillis();
        }

        /* ------------------------------------------------------------ */
        public boolean isValid()
        {
            return !_invalid;
        }
        
        /* ------------------------------------------------------------ */
        public ServletContext getServletContext()
        {
            return _handler.getServletContext();
        }
        
        /* ------------------------------------------------------------- */
        public String getId()
            throws IllegalStateException
        {
            if (_invalid) throw new IllegalStateException();
            return id;
        }

        /* ------------------------------------------------------------- */
        public long getCreationTime()
            throws IllegalStateException
        {
            if (_invalid) throw new IllegalStateException();
            return _created;
        }

        /* ------------------------------------------------------------- */
        public long getLastAccessedTime()
            throws IllegalStateException
        {
            if (_invalid) throw new IllegalStateException();
            return _accessed;
        }

        /* ------------------------------------------------------------- */
        public int getMaxInactiveInterval()
        {
            if (_invalid) throw new IllegalStateException();
            return (int)(_maxIdleMs / 1000);
        }

        /* ------------------------------------------------------------- */
        /**
         * @deprecated
         */
        public HttpSessionContext getSessionContext()
            throws IllegalStateException
        {
            if (_invalid) throw new IllegalStateException();
            return SessionContext.NULL_IMPL;
        }

        /* ------------------------------------------------------------- */
        public void setMaxInactiveInterval(int secs)
        {
            _maxIdleMs = (long)secs * 1000;

            if (_maxIdleMs>0 && _maxIdleMs/4<_scavengeDelay)
            {
                synchronized(HashSessionManager.this)
                {
                    // Adjust scavange delay to 10% of timeout
                    _scavengeDelay=_dftMaxIdleSecs*100;
                    if (_scavengeDelay>60000)
                        _scavengeDelay=60000;
                    _scavenger.interrupt();
                }
            }
        }

        /* ------------------------------------------------------------- */
        public synchronized void invalidate()
            throws IllegalStateException
        {
            if (_invalid) throw new IllegalStateException();
            
            Iterator iter = _values.keySet().iterator();
            while (iter.hasNext())
            {
                String key = (String)iter.next();
                Object value = _values.get(key);
                iter.remove();
                unbindValue(key, value);
            }
            synchronized (HashSessionManager.this)
            {
                _sessions.remove(id);
            }
            _invalid=true;
        }

        /* ------------------------------------------------------------- */
        public boolean isNew()
            throws IllegalStateException
        {
            if (_invalid) throw new IllegalStateException();
            return _newSession;
        }


        /* ------------------------------------------------------------ */
        public Object getAttribute(String name)
        {
            if (_invalid) throw new IllegalStateException();
            return _values.get(name);
        }

        /* ------------------------------------------------------------ */
        public Enumeration getAttributeNames()
        {
            if (_invalid) throw new IllegalStateException();
            return Collections.enumeration(_values.keySet());
        }

        /* ------------------------------------------------------------ */
        public void setAttribute(String name, Object value)
        {
            if (_invalid) throw new IllegalStateException();
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
            if (_invalid) throw new IllegalStateException();
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
            if (_invalid) throw new IllegalStateException();
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
