// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;


public class ByteBufferPool
{
    private ArrayList _pool=new ArrayList();
    private int _capacity =4096;
    private boolean _direct =false;

    /* ------------------------------------------------------------ */
    public ByteBufferPool()
    {}
    
    /* ------------------------------------------------------------ */
    public ByteBufferPool(int capacity, boolean direct)
    {
        _capacity=capacity;
        _direct=direct;
    }
    
    /* ------------------------------------------------------------ */
    public int getCapacity()
    {
        return _capacity;
    }
    
    /* ------------------------------------------------------------ */
    public void setCapacity(int capacity)
    {
        _capacity = capacity;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isDirect()
    {
        return _direct;
    }
    
    /* ------------------------------------------------------------ */
    public void setDirect(boolean direct)
    {
        _direct = direct;
    }

    /* ------------------------------------------------------------ */
    public synchronized ByteBuffer get()
    {
        if (_pool.isEmpty())
        {
            if (_direct)
                return ByteBuffer.allocateDirect(_capacity);
            return ByteBuffer.allocate(_capacity);
        }

        ByteBuffer buffer = (ByteBuffer)_pool.remove(_pool.size()-1);
        buffer.clear();
        return buffer;
    }

    /* ------------------------------------------------------------ */
    public synchronized void add(ByteBuffer buffer)
    {
        if (buffer.capacity()==_capacity)
            _pool.add(buffer);
    }
    
}

