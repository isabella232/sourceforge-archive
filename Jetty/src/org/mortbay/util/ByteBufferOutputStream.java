// ===========================================================================
// Copyright (c) 2001 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.util;
import java.io.IOException;
import java.io.OutputStream;

/* ------------------------------------------------------------ */
/** ByteBuffer OutputStream.
 * This stream is similar to the java.io.ByteArrayOutputStream,
 * except that it maintains a reserve of bytes at the start of the
 * buffer and allows efficient prepending of data.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ByteBufferOutputStream extends OutputStream
{
    protected byte[] _buf;
    private int _start;
    private int _pos;
    private int _end;
    private int _preReserve;
    private int _postReserve;
    private boolean _resized;
    private boolean _fixed ;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ByteBufferOutputStream(){this(4096,0,0);}
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param capacity Buffer capacity
     */
    public ByteBufferOutputStream(int capacity)
    {
        this(capacity,0,0);
    }
    
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param capacity Buffer capacity.
     * @param fullAt The size of the buffer.
     * @param reserve The reserve of byte for prepending
     */
    public ByteBufferOutputStream(int capacity,int preReserve)
    {
        this(capacity,preReserve,0);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param BufferSize The size of the buffer == capacity+preReserve+postReserve
     * @param fullAt The size of the buffer.
     * @param preReserve The reserve of byte for prepending
     * @param postReserve The reserve of byte for appending
     */
    public ByteBufferOutputStream(int bufferSize,int preReserve,int postReserve)
    {
        _buf=ByteArrayPool.getByteArray(bufferSize);
        _end=_buf.length-postReserve;
        _preReserve=preReserve;
        _start=preReserve;
        _pos=preReserve;
        _postReserve=postReserve;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return  True if the buffer cannot be expanded 
     */
    public boolean isFixed()
    {
        return _fixed;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param fixed True if the buffer cannot be expanded 
     */
    public void setFixed(boolean fixed)
    {
        _fixed = fixed;
    }
    
    /* ------------------------------------------------------------ */
    public int size()
    {
        return _pos-_start;
    }
    
    /* ------------------------------------------------------------ */
    public int capacity()
    {
        return _end-_start;
    }
    
    /* ------------------------------------------------------------ */
    public int spareCapacity()
    {
        return _end-_pos;
    }
    
    /* ------------------------------------------------------------ */
    public int preReserve()
    {
        return _start;
    }
    
    /* ------------------------------------------------------------ */
    public int postReserve()
    {
        return _buf.length-_end;
    }
    
    /* ------------------------------------------------------------ */
    public void writeTo(OutputStream out)
        throws IOException
    {
        out.write(_buf,_start,_pos-_start);
    }

    /* ------------------------------------------------------------ */
    public void write(int b)
        throws IOException
    {
        ensureSpareCapacity(1);
        _buf[_pos++]=(byte)b;
    }
    
    /* ------------------------------------------------------------ */
    public void write(byte[] b)
        throws IOException
    {
        ensureSpareCapacity(b.length);
        System.arraycopy(b,0,_buf,_pos,b.length);
        _pos+=b.length;
    }
    
    /* ------------------------------------------------------------ */
    public void write(byte[] b,int offset, int length)
        throws IOException
    {
        ensureSpareCapacity(length);
        System.arraycopy(b,offset,_buf,_pos,length);
        _pos+=length;
    }
    
    /* ------------------------------------------------------------ */
    /** Write byte to start of the buffer.
     * @param b 
     */
    public void prewrite(int b)
    {
        ensureReserve(1);
        _buf[--_start]=(byte)b;
    }
    
    /* ------------------------------------------------------------ */
    /** Write byte array to start of the buffer.
     * @param b 
     */
    public void prewrite(byte[] b)
    {
        ensureReserve(b.length);
        System.arraycopy(b,0,_buf,_start-b.length,b.length);
        _start-=b.length;
    }
    
    /* ------------------------------------------------------------ */
    /** Write byte range to start of the buffer.
     * @param b 
     * @param offset 
     * @param length 
     */
    public void prewrite(byte[] b,int offset, int length)
    {
        ensureReserve(length);
        System.arraycopy(b,offset,_buf,_start-length,length);
        _start-=length;
    }

    /* ------------------------------------------------------------ */
    /** Write bytes into the postreserve.
     * The capacity is not checked.
     * @param b 
     * @param offset 
     * @param length 
     * @exception IOException 
     */
    public void postwrite(byte[] b,int offset, int length)
        throws IOException
    {
        System.arraycopy(b,offset,_buf,_pos,length);
        _pos+=length;
    }
    
    /* ------------------------------------------------------------ */
    public void flush()
        throws IOException
    {
    }
    
    /* ------------------------------------------------------------ */
    public void resetStream()
    {
        _pos=_preReserve;
        _start=_preReserve;
    }
    
    /* ------------------------------------------------------------ */
    public void reset(int reserve)
    {
        _preReserve=reserve;
        _pos=_preReserve;
        _start=_preReserve;
    }

    /* ------------------------------------------------------------ */
    public void close()
        throws IOException
    {
        flush();
    }
    
    /* ------------------------------------------------------------ */
    public void destroy()
    {
        if (!_resized)
            ByteArrayPool.returnByteArray(_buf);
        _buf=null;
    }

    /* ------------------------------------------------------------ */
    public void ensureReserve(int n)
    {
        if (n>_start)
        {
            if (Code.debug())Code.debug("Reserve: "+n+">"+_start);
            if ((_pos+n)<_end)
            {
                if (Code.debug())Code.debug("Shift reserve: "+_pos+"+"+n+"<"+_end);
                System.arraycopy(_buf,_start,_buf,n,_pos-_start);
                _pos=_pos+n-_start;
                _start=n;
            }
            else
            {
                if (Code.debug())Code.debug("New reserve: "+_pos+"+"+n+">="+_end);
                byte[] buf = new byte[_buf.length+n-_start];
                System.arraycopy(_buf,_start,buf,n,_pos-_start);
                _pos=n+_pos-_start;
                _start=n;
                _buf=buf;
                _end=_buf.length-_postReserve;
            }
        }
    }
    
    
    /* ------------------------------------------------------------ */
    public void ensureCapacity(int n,int pre, int post)
        throws IOException
    {
        // Do we have space?
        if (n>_end || pre > _preReserve || post > _postReserve)
        {
            // Make a bigger buffer if we are allowed.
            if (_fixed)
                throw new IllegalStateException("Fixed");
            if (_start!=_pos)
                throw new IllegalStateException("Not reset");
            
            if (!_resized)
                ByteArrayPool.returnByteArray(_buf);
                
            _buf=ByteArrayPool.getByteArray(n+pre+post);
            _end=_buf.length-post;
            _preReserve=pre;
            _start=pre;
            _pos=pre;
            _postReserve=post;
        }
    }
    
    /* ------------------------------------------------------------ */
    public void ensureSpareCapacity(int n)
        throws IOException
    {
        // Do we have space?
        if ((_pos+n)>_end)
        {
            // No, then try flushing what we do have
            flush();
            
            // Do we have space now?
            if ((_pos+n)>_end)
            {
                // Make a bigger buffer if we are allowed.
                if (_fixed)
                    throw new IllegalStateException("Buffer Full");
                
                int bl = ((_pos+n+4095)/4096)*4096;
                byte[] buf = new byte[bl];
                if (Code.debug())Code.debug("New buf for ensure: "+_pos+"+"+n+">"+_buf.length+" --> "+buf.length);
                System.arraycopy(_buf,_start,buf,_start,_pos-_start);
                if (!_resized)
                    ByteArrayPool.returnByteArray(_buf);
                _buf=buf;
                _end=_buf.length-_postReserve;
                _resized=true;
            }
        }
    }
}
    
    
