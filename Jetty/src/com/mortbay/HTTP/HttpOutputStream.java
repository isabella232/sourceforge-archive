// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Util.*;

import java.util.Observer;
import java.io.*;

/* ---------------------------------------------------------------- */
/** HTTP output stream
 * <p> Implements ServletOutputStream
 *
 * <p><h4>Notes</h4>
 * Implements a callback to the HttpResponse to trigger a writeHeader
 * on the first output.  Not if HttpFilters are used, this may result
 * in replaceOut being called on this HttpOutputStream.
 * <p>
 * The path that a single call to write on HttpOutputStream
 * can be delegated to the following output streams:
 * <PRE>
 *  ---> HttpOutputSteam.out --> Filter out 1 --> ... -> Filter out N
 *                                                               |
 *             HttpOutputStream$SwitchedOutStream switchOut <----+
 *                                   /     \
 *                                  /       \
 *                                 V         V
 * socket <---  HttpOutputStream.realOut <-- ByteArrayOutputStream chunk
 *
 * </PRE>
 * @version $Id$
 * @author Greg Wilkins
*/
public class HttpOutputStream extends FilterOutputStream
{
    /* ------------------------------------------------------------ */
    final static byte[]
        __CRLF_B      ={(byte)'\015',(byte)'\012'},
        __CHUNK_EOF_B ={(byte)'0',(byte)';',(byte)'\015',
                        (byte)'\012',(byte)'\015',(byte)'\012'};

    
    /* ------------------------------------------------------------ */
    OutputStream _realOut;
    ByteArrayOutputStream _buffer;
    byte[] _chunkHead;
    Observer _observer;
    
    /* ------------------------------------------------------------ */
    public HttpOutputStream(OutputStream outputStream, Observer notifyFirstWrite)
    {
        super(new ByteArrayOutputStream(4096));
        _buffer=(ByteArrayOutputStream)out;
        _realOut=outputStream;
        _observer=notifyFirstWrite;
    }

    /* ------------------------------------------------------------ */
    public OutputStream getOutputStream()
    {
        return out;
    }
    
    /* ------------------------------------------------------------ */
    public OutputStream insertFilter(OutputStream newOut)
    {
        OutputStream oldOut = out;
        out=newOut;
        return oldOut;
    }
    
    /* ------------------------------------------------------------ */
    /** Switch chunking on an off
     * @param on 
     */
    public void setChunking(boolean on)
        throws IOException
    {
        flush();
        if (on)
            _chunkHead=new byte[16];
        else
            _chunkHead=null;
    }
    
    /* ------------------------------------------------------------ */
    public void write(int b) throws IOException
    {
        if (_observer!=null)
        {
            _observer.update(null,this);
            _observer=null;
        }

        out.write(b);
        if (_buffer.size()>4000)
            flush();
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[]) throws IOException
    {
        if (_observer!=null)
        {
            _observer.update(null,this);
            _observer=null;
        }
        
        out.write(b);
        if (_buffer.size()>4000)
            flush();
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[], int off, int len) throws IOException
    {
        if (_observer!=null)
        {
            _observer.update(null,this);
            _observer=null;
        }
        
        out.write(b,off,len);
        if (_buffer.size()>4000)
            flush();
    }

    /* ------------------------------------------------------------ */
    public synchronized void flush() throws IOException
    {
        out.flush();
        if (_buffer.size()>0)
        {
            if (_chunkHead!=null)
            {
                String size = Integer.toString(_buffer.size(),16);
                byte[] b = size.getBytes();
                int i;
                for (i=0;i<b.length;i++)
                    _chunkHead[i]=b[i];
                _chunkHead[i++]=(byte)';';
                _chunkHead[i++]=__CRLF_B[0];
                _chunkHead[i++]=__CRLF_B[1];
                _realOut.write(_chunkHead,0,i);
                _buffer.writeTo(_realOut);
                _buffer.reset();
                _realOut.write(__CRLF_B);
            }
            else
            {
                _buffer.writeTo(_realOut);
                _buffer.reset();
            }
        }
        _realOut.flush();
    }

    /* ------------------------------------------------------------ */
    public synchronized void close()
        throws IOException
    {
        try {
            flush();

            // close filters
            out.close();
            
            // If chunking
            if (_chunkHead!=null)
            {
                // send last chunk and revert to normal output
                _realOut.write(__CHUNK_EOF_B);
                _realOut.flush();
                _chunkHead=null;
            }
            else
                _realOut.close();
        }
        catch (IOException e)
        {
            Code.ignore(e);
        }
    }
}









