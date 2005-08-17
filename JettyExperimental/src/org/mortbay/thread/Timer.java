//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.thread;

import org.mortbay.util.LogSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/* ------------------------------------------------------------ */
/** Timer.
 * This class implements a timer queue for timers that are at least as likely to be cancelled as they are to expire.
 * 
 * @author gregw
 *
 */
public class Timer
{
    private final static Logger log = LoggerFactory.getLogger(TimerTest.class);
    
    private long _duration;
    private Queue _queue=new Queue();
    private long _now;
    private long _lastTick;

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the duration.
     */
    public long getDuration()
    {
        return _duration;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param duration The duration to set.
     */
    public void setDuration(long duration)
    {
        _duration = duration;
    }

    /* ------------------------------------------------------------ */
    public void setNow()
    {
        _now=System.currentTimeMillis();
    }

    /* ------------------------------------------------------------ */
    public void setNow(long now)
    {
        _now=now;
    }

    /* ------------------------------------------------------------ */
    public void tick()
    {
        if (_now==0 || _lastTick==_now)
            throw new IllegalStateException();
        _lastTick=_now;
        
        long _expiry = _now-_duration;
        
        while (_queue.size()>0 && _queue.head()._timestamp<_expiry)
        {
            Timeout entry = _queue.head();
            if (entry._timestamp>_expiry)
                break;
            entry.unlink();
            try
            {
                entry.doExpire();
            }
            catch(Throwable th)
            {
                log.warn(LogSupport.EXCEPTION,th);
            }
        }
    }

    /* ------------------------------------------------------------ */
    public void add(Timeout entry)
    {
        if (entry._timestamp!=0)
        {
            entry.unlink();
            entry._timestamp=0;
        }
        
        entry._timestamp = _now;
        _queue.add(entry);
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static class Link
    {
        Link _next;
        Link _prev;
        
        Link()
        {
            _next=_prev=this;
        }
        
        public void unlink()
        {
            _next._prev=_prev;
            _prev._next=_next;
            _next=_prev=this;
        }

        public void setNext(Timeout entry)
        {
            if (entry._next!=entry)
                throw new IllegalStateException();
            Link next_next = _next;
            _next._prev=entry;
            _next=entry;
            _next._next=next_next;
            _next._prev=this;   
        }
    }
    

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static class Timeout extends Link 
    {
        long _timestamp=0;
        boolean _expired=false;

        
        public void cancel()
        {
            _timestamp=0;
            unlink();
        }
        
        public boolean isExpired() { return _expired; }
        
        public void expire(){}
        
        private void doExpire()
        {
            _expired=true;
            expire();
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static class Queue
    {
        Link _head=new Link();

        public int size()
        {
            int size=0;
            Link link = _head;
            while(link._next!=_head)
            {
                size++;
                link=link._next;
            }
            return size;
        }

        public void clear()
        {
            _head._next=_head._prev=_head;
        }

        public boolean isEmpty()
        {
            return _head._next==_head;
        }

        public boolean add(Timeout entry)
        {
            _head._prev.setNext(entry);
            return true;
        }
        
        public Timeout head()
        {
            if (_head._next==_head)
                return null;
            return (Timeout)_head._next;
        }

        
    }
    
}
