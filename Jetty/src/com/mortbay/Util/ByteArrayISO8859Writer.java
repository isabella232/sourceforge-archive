// ===========================================================================
// Copyright (c) 2001 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.io.OutputStream;


public class ByteArrayISO8859Writer extends Writer
{
    private byte[] _buf;
    private int _size;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ByteArrayISO8859Writer(){this(4096);}
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param capacity Buffer capacity
     */
    public ByteArrayISO8859Writer(int capacity)
    {
        _buf=new byte[capacity];
    }
    

    /* ------------------------------------------------------------ */
    public int size()
    {
        return _size;
    }

    /* ------------------------------------------------------------ */
    public byte[] getBuf()
    {
        return _buf;
    }
    
    /* ------------------------------------------------------------ */
    public int getCapacity()
    {
        return _buf.length;
    }
    
    /* ------------------------------------------------------------ */
    public void writeTo(OutputStream out)
        throws IOException
    {
        out.write(_buf,0,_size);
    }

    /* ------------------------------------------------------------ */
    public void write(char c)
    {
        ensureCapacity(1);
        _buf[_size++]=(byte)c;
    }
    
    /* ------------------------------------------------------------ */
    public void write(char[] c)
    {
        ensureCapacity(c.length);
        for (int i=0;i<c.length;i++)
            _buf[_size+i]=(byte)c[i]; 
        _size+=c.length;
    }
    
    /* ------------------------------------------------------------ */
    public void write(char[] c,int offset, int length)
    {
        ensureCapacity(length);
        for (int i=0;i<length;i++)
            _buf[_size+i]=(byte)c[offset+i];            
        _size+=length;
    }
    
    /* ------------------------------------------------------------ */
    public void write(String s)
    {
        int length=s.length();
        ensureCapacity(length);
        for (int i=0;i<length;i++)
            _buf[_size+i]=(byte)(s.charAt(i));            
        _size+=length;
    }
    
    /* ------------------------------------------------------------ */
    public void write(String s,int offset, int length)
    {
        ensureCapacity(length);
        for (int i=0;i<length;i++)
            _buf[_size+i]=(byte)(s.charAt(offset+i));            
        _size+=length;
    }
    
    /* ------------------------------------------------------------ */
    public void flush()
    {}

    /* ------------------------------------------------------------ */
    public void reset()
    {
        _size=0;
    }

    /* ------------------------------------------------------------ */
    public void close()
    {}

    /* ------------------------------------------------------------ */
    public void ensureCapacity(int n)
    {
        if (_size+n>_buf.length)
        {
            byte[] buf = new byte[(_buf.length+n)*4/3];
            System.arraycopy(_buf,0,buf,0,_size);
            _buf=buf;
        }
    }
}
    
    
