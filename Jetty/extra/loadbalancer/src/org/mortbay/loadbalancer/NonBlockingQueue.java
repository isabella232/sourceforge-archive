// ========================================================================
// $Id$
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.loadbalancer;

/* ------------------------------------------------------------ */
public class NonBlockingQueue 
{
    private Object[] _queue;
    private int _pos, _size;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param capacity 
     */
    public NonBlockingQueue(int capacity)
    {
        _queue=new Object[capacity];
        _pos=0;
        _size=0;
    }

    /* ------------------------------------------------------------ */
    public int size()
    {
        return _size;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isFull()
    {
        return _size==_queue.length;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isEmpty()
    {
        return _size==0;
    }

    /* ------------------------------------------------------------ */
    public synchronized boolean queue(Object o)
    {
        if (isFull())
            return false;        
        _queue[(_pos+_size)%_queue.length]=o;
        _size++;
        return true;
    }

    /* ------------------------------------------------------------ */
    public synchronized Object peek()
    {
        if (_size==0)
            throw new IllegalStateException("Empty");
        
        return _queue[_pos];
    }
    
    /* ------------------------------------------------------------ */
    public synchronized Object next()
    {
        if (_size==0)
            throw new IllegalStateException("Empty");
        
        Object o=_queue[_pos];
        _size--;
        _pos=(_pos+1)%_queue.length;
        return o;
    }
}

