// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Util.*;

import java.util.Observer;
import java.io.*;

/* ---------------------------------------------------------------- */
/** HTTP Chunkable OutputStream
 * Acts as a BufferedOutputStream until setChunking(true) is called.
 * Once chunking is enabled, the raw stream is chunk encoded as per RFC2616.
 *
 * Implements a callback to an Observer before the first output.  
 * @version $Id$
 * @author Greg Wilkins
*/
public class ChunkableOutputStream extends FilterOutputStream
{
    /* ------------------------------------------------------------ */
    final static byte[]
        __CRLF_B      ={(byte)'\015',(byte)'\012'},
        __CHUNK_EOF_B ={(byte)'0',(byte)';',(byte)'\015',(byte)'\012'};

    
    /* ------------------------------------------------------------ */
    OutputStream _realOut;
    ByteArrayOutputStream _buffer;
    byte[] _chunkSize;
    Observer _observer;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param outputStream The outputStream to buffer or chunk to.
     * @param notifyFirstWrite An observer that is called when the first
     *                         write is called.
     */
    public ChunkableOutputStream(OutputStream outputStream,
                                 Observer notifyFirstWrite)
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
    /** Insert FilterOutputStream.
     * Place a FilterOutputStream into this stream, but before the
     * chunking stream.
     * @param filter 
     */
    public void insertFilter(OutputStream filter)
    {
        out=filter;
    }
    
    /* ------------------------------------------------------------ */
    /** Set chunking mode.
     * @param on 
     */
    public void setChunking(boolean on)
        throws IOException
    {
        flush();
        if (on)
            _chunkSize=new byte[16];
        else
            _chunkSize=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Get chunking mode 
     */
    public boolean getChunking()
    {
        return _chunkSize!=null;
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
            if (_chunkSize!=null)
            {
                String size = Integer.toString(_buffer.size(),16);
                byte[] b = size.getBytes();
                int i;
                for (i=0;i<b.length;i++)
                    _chunkSize[i]=b[i];
                _chunkSize[i++]=(byte)';';
                _chunkSize[i++]=__CRLF_B[0];
                _chunkSize[i++]=__CRLF_B[1];
                _realOut.write(_chunkSize,0,i);
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
    /** Close the stream.
     * In chunking mode, the underlying stream is not closed.
     * All filters are closed and discarded.
     * @exception IOException 
     */
    public synchronized void close()
        throws IOException
    {
        try {
            flush();

            // close filters
            out.close();
            
            // If chunking
            if (_chunkSize!=null)
            {
                // send last chunk and revert to normal output
                _realOut.write(__CHUNK_EOF_B);
                _realOut.write(__CRLF_B);
                _realOut.flush();
                _chunkSize=null;
                out=_buffer=new ByteArrayOutputStream(4096);
            }
            else
                _realOut.close();
        }
        catch (IOException e)
        {
            Code.ignore(e);
        }
    }

    
    /* ------------------------------------------------------------ */
    /** Close the stream.
     * In chunking mode, the underlying stream is not closed.
     * All filters are closed and discarded.
     * @param footers Chunked footers
     * @exception IOException 
     */
    public synchronized void close(HttpFields footers)
        throws IOException
    {
        if (_chunkSize==null)
            throw new IllegalStateException("Footers only in Chunked mode");
        
        flush();
        out.close();
        _realOut.write(__CHUNK_EOF_B);
        footers.write(_realOut);
        _realOut.flush();
        _chunkSize=null;
        out=_buffer=new ByteArrayOutputStream(4096);
    }
}


