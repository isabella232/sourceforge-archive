// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.mortbay.util.Log;
import org.mortbay.util.Code;
import org.mortbay.util.LazyList;


/* ------------------------------------------------------------ */
/** An in-memory implementation of SessionManager
 * <p>
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
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
    private int _scavengePeriodMs = 30000;
    private ServletHandler _handler;

    private ArrayList _sessionListeners=new ArrayList();
    private ArrayList _sessionAttributeListeners=new ArrayList();
    
    
    /* ------------------------------------------------------------ */
    protected HashSessionManager(ServletHandler handler)
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
        Session session = new Session();
        session.setMaxInactiveInterval(_dftMaxIdleSecs);
        _sessions.put(session.getId(),session);

        for(int i=0;i<_sessionListeners.size();i++)
            ((HttpSessionListener)_sessionListeners.get(i))
                .sessionCreated(session.getHttpSessionEvent());
        return session;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param seconds 
     */
    public synchronized void setMaxInactiveInterval(int seconds)
    {
        _dftMaxIdleSecs = seconds;
        if (_dftMaxIdleSecs>0 && _scavengePeriodMs>_dftMaxIdleSecs*100)
            setScavangePeriod((_dftMaxIdleSecs+9)/10);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param seconds 
     */
    public synchronized void setScavangePeriod(int seconds)
    {
        if (seconds==0)
            seconds=60;
        
        int old_period=_scavengePeriodMs;
        int period = seconds*1000;
        if (period>60000)
            period=60000;
        if (period<1000)
            period=1000;
        
        // Start the session scavenger if we haven't already
        if (_scavenger == null && period>0)
            _scavenger = new SessionScavenger();
        
        if (period!=old_period)
        {
            _scavengePeriodMs=period;
            _scavenger.interrupt();
        }
    }
    
    
    /* ------------------------------------------------------------ */
    public void addEventListener(EventListener listener)
        throws IllegalArgumentException
    {
        boolean known =false;
        if (listener instanceof HttpSessionAttributeListener)
        {
            _sessionAttributeListeners.add(listener);
            known=true;
        }
        if (listener instanceof HttpSessionListener)
        {
            _sessionListeners.add(listener);
            known=true;
        }

        if (!known)
            throw new IllegalArgumentException("Unknown listener "+listener);
    }
    
    /* ------------------------------------------------------------ */
    public void removeEventListener(EventListener listener)
    {
        boolean known =false;
        if (listener instanceof HttpSessionAttributeListener)
            _sessionAttributeListeners.remove(listener);
        if (listener instanceof HttpSessionListener)
            _sessionListeners.remove(listener);
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
    
    /* -------------------------------------------------------------- */
    /** Find sessions that have timed out and invalidate them.
     *  This runs in the SessionScavenger thread.
     */
    private void scavenge()
    {
        long now = System.currentTimeMillis();

        // Since Hashtable enumeration is not safe over deletes,
        // we build a list of stale sessions, then go back and invalidate them
        LazyList stale=null;

        // For each session
        try
        {
            for (Iterator i = _sessions.values().iterator(); i.hasNext(); )
            {
                Session session = (Session)i.next();
                long idleTime = session._maxIdleMs;
                if (idleTime > 0 && session._accessed + idleTime < now) {
                    // Found a stale session, add it to the list
                    stale=LazyList.add(stale,session);
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
                    stale=null;
                    scavenge();
                }
            }
            finally {Thread.currentThread().setPriority(oldPriority);}
        }

        // Remove the stale sessions
        for (int i = LazyList.size(stale); i-->0;)
        {
            ((Session)LazyList.get(stale,i)).invalidate();
        }
    }
    

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* -------------------------------------------------------------- */
    /** SessionScavenger is a background thread that kills off old sessions */
    class SessionScavenger extends Thread
    {
        public void run()
        {
            int period=-1;
            try{
                while (isStarted())
                {
                    try {
                        if (period!=_scavengePeriodMs)
                        {
                            Log.event("Session scavenger period = "+_scavengePeriodMs/1000+"s");
                            period=_scavengePeriodMs;
                        }
                        sleep(period>1000?period:1000);
                        HashSessionManager.this.scavenge();
                    }
                    catch (InterruptedException ex){continue;}
                    catch (Error e) {Code.warning(e);}
                    catch (Exception e) {Code.warning(e);}
                }
            }
            finally
            {
                HashSessionManager.this._scavenger=null;
            }
        }

        SessionScavenger()
        {
            super("SessionScavenger");
            setDaemon(true);
            this.start();
        }

    }   // SessionScavenger
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    protected class Session
        implements SessionManager.Session
    {
        HashMap _values = new HashMap(11);
        boolean _invalid=false;
        boolean _newSession=true;
        long _created=System.currentTimeMillis();
        long _accessed=_created;
        long _maxIdleMs = _dftMaxIdleSecs*1000;
        String _id;
        HttpSessionEvent _event;

        /* ------------------------------------------------------------- */
        protected Session()
        {
            synchronized(Session.class)
            {
                do
                {
                    long newId = __nextSessionId;
                    __nextSessionId+=this.hashCode();
                    if (newId<0)newId=-newId;
                    this._id=Long.toString(newId,30+(int)(_created%7));
                }
                while (_sessions.containsKey(this._id));
            }
            if (_dftMaxIdleSecs>=0)
                _maxIdleMs=_dftMaxIdleSecs*1000;
        }

        /* ------------------------------------------------------------ */
        HttpSessionEvent getHttpSessionEvent()
        {
            if (_event==null)
                _event=new HttpSessionEvent(this);
            return _event;
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
            return _id;
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
            if (_maxIdleMs>0 && (_maxIdleMs/10)<_scavengePeriodMs)
                HashSessionManager.this.setScavangePeriod((secs+9)/10);
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

                if (_sessionAttributeListeners.size()>0)
                {
                    HttpSessionBindingEvent event =
                        new HttpSessionBindingEvent(this,key,value);
                    
                    for(int i=0;i<_sessionAttributeListeners.size();i++)
                        ((HttpSessionAttributeListener)
                         _sessionAttributeListeners.get(i))
                            .attributeRemoved(event);
                }
            }
            synchronized (HashSessionManager.this)
            {
                _invalid=true;
                _sessions.remove(_id);
                for(int i=0;i<_sessionListeners.size();i++)
                    ((HttpSessionListener)_sessionListeners.get(i)).
                        sessionDestroyed(getHttpSessionEvent());       
            }
             
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

            if (value==null || !value.equals(oldValue))
            {
                unbindValue(name, oldValue);
                bindValue(name, value);
                
                if (_sessionAttributeListeners.size()>0)
                {
                    HttpSessionBindingEvent event =
                        new HttpSessionBindingEvent(this,name,
                                                    oldValue==null?value:oldValue);
                    
                    for(int i=0;i<_sessionAttributeListeners.size();i++)
                    {
                        HttpSessionAttributeListener l =
                            (HttpSessionAttributeListener)
                            _sessionAttributeListeners.get(i);
                        
                        if (oldValue==null)
                            l.attributeAdded(event);
                        else if (value==null)
                            l.attributeRemoved(event);
                        else
                            l.attributeReplaced(event);
                    }
                }
            }
        }

        /* ------------------------------------------------------------ */
        public void removeAttribute(String name)
        {
            if (_invalid) throw new IllegalStateException();
            Object old=_values.remove(name);
            if (old!=null)
            {
                unbindValue(name, old);
                if (_sessionAttributeListeners.size()>0)
                {
                    HttpSessionBindingEvent event =
                        new HttpSessionBindingEvent(this,name,old);
                    
                    for(int i=0;i<_sessionAttributeListeners.size();i++)
                    {
                        HttpSessionAttributeListener l =
                            (HttpSessionAttributeListener)
                            _sessionAttributeListeners.get(i);
                        l.attributeRemoved(event);
                    }
                }
            }
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
