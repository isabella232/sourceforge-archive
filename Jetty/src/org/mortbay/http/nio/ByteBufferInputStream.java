/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 9/02/2004
 * $Id$
 * ============================================== */
 
package org.mortbay.http.nio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.LazyList;
import org.mortbay.util.LogSupport;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class ByteBufferInputStream extends InputStream
{
    private static Log log= LogFactory.getLog(ByteBufferInputStream.class);
    
    int _bufferSize;
    ByteBuffer _buffer;
    Object _buffers;
    Object _recycle;
    boolean _closed=false;
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     */
    public ByteBufferInputStream(int bufferSize)
    {
        super();
        _bufferSize=bufferSize;
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.InputStream#read()
     */
    public synchronized int read() throws IOException
    {
        if (!waitForContent())
            return -1;
         return _buffer.get();
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.InputStream#available()
     */
    public synchronized int available() throws IOException
    {
        if (!waitForContent())
            return -1;
         return _buffer.remaining();
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.InputStream#close()
     */
    public synchronized void close() throws IOException
    {
        _closed=true;
        this.notify();
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.InputStream#mark(int)
     */
    public synchronized void mark(int arg0)
    {
        // TODO Auto-generated method stub
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.InputStream#markSupported()
     */
    public synchronized boolean markSupported()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public synchronized int read(byte[] buf, int offset, int length) 
        throws IOException
    {
        if (!waitForContent())
            return -1;
            
         if (length>_buffer.remaining())
            length=_buffer.remaining();
            
         _buffer.get(buf, offset, length);
        return length;
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.InputStream#read(byte[])
     */
    public synchronized int read(byte[] buf) throws IOException
    {
        if (!waitForContent())
            return -1;
         int length=buf.length;
         if (length>_buffer.remaining())
            length=_buffer.remaining();
            
         _buffer.get(buf, 0, length);
        return length;
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.InputStream#reset()
     */
    public synchronized void reset() throws IOException
    {
        // TODO Auto-generated method stub
        super.reset();
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * @see java.io.InputStream#skip(long)
     */
    public long skip(long length) throws IOException
    {
        if (!waitForContent())
            return -1;
         if (length>_buffer.remaining())
            length=_buffer.remaining();
         _buffer.position((int)(_buffer.position()+length));
        return length;
    }

    /* ------------------------------------------------------------------------------- */
     public synchronized void write(ByteBuffer buffer)
     {
         _buffers=LazyList.add(_buffers,buffer);
         this.notify();
     }

    /* ------------------------------------------------------------------------------- */
    private synchronized boolean waitForContent()
    {
        if (_buffer!=null)
        {
            if (_buffer.hasRemaining())
                return true;
             
             // recycle buffer
             _recycle=LazyList.add(_recycle,_buffer);
             _buffer=null;
        }        
        
        while(!_closed && LazyList.size(_buffers)==0)
        {
            try
            {
                this.wait();
            }
            catch(InterruptedException e)
            {
                LogSupport.ignore(log,e);
                return false;
            }
        }    
        
        if (LazyList.size(_buffers)==0)
            return false;
        _buffer=(ByteBuffer)LazyList.get(_buffers, 0);
        _buffers=LazyList.remove(_buffers, 0);
        
        return true;
    }
    

    /* ------------------------------------------------------------------------------- */
    /** Get a buffer to write to this InputStream.
     * The buffer wll either be a new direct buffer or a recycled buffer.
     */
    public synchronized ByteBuffer getBuffer()
    {
        ByteBuffer buf=null;
        int s=LazyList.size(_recycle);
        if (s>0)
        {
            s--;
             buf=(ByteBuffer)LazyList.get(_recycle, s);
             _recycle=LazyList.remove(_recycle,s);
        }
        else
            buf=ByteBuffer.allocateDirect(_bufferSize);
         return buf;     
    }
    

    /* ------------------------------------------------------------------------------- */
    public void destroy()
    {
        _buffer=null;
        _buffers=null;
        _recycle=null;
    }
}
