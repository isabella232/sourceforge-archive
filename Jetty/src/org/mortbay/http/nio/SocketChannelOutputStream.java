/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 9/02/2004
 * $Id$
 * ============================================== */
 
package org.mortbay.http.nio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.LogSupport;

/* ------------------------------------------------------------------------------- */
/** 
 * Blocking output stream on non-blocking SocketChannel.  Makes the 
 * assumption that writes will rarely need to block.
 * All writes flush to the channel, and no additional buffering is done.
 * @version $Revision$
 * @author gregw
 */
public class SocketChannelOutputStream extends OutputStream
{
    private static Log log= LogFactory.getLog(SocketChannelOutputStream.class);
    
    ByteBuffer _buffer;
    ByteBuffer _flush;
    SocketChannel _channel;
    Selector _selector;
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     * 
     */
    public SocketChannelOutputStream(SocketChannel channel,
                                                                             int bufferSize)
    {
        _channel=channel;
        _buffer=ByteBuffer.allocateDirect(bufferSize);
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException
    {
        _buffer.clear();
        _buffer.put((byte)b);
        _buffer.flip();
        _flush=_buffer;
        flushBuffer();
    }

    
    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException
    {
        _channel.close();
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.OutputStream#flush()
     */
    public void flush() throws IOException
    {
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] buf, int offset, int length) throws IOException
    {
        if (length>_buffer.capacity())
            _flush=ByteBuffer.wrap(buf,offset,length);
        else
         {
             _buffer.clear();
             _buffer.put(buf,offset,length);
             _buffer.flip();
             _flush=_buffer;
         }
         flushBuffer();
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see java.io.OutputStream#write(byte[])
     */
    public void write(byte[] buf) throws IOException
    {
        if (buf.length>_buffer.capacity())
            _flush=ByteBuffer.wrap(buf);
        else
         {
             _buffer.clear();
             _buffer.put(buf);
             _buffer.flip();
             _flush=_buffer;
         }
         flushBuffer();
    }


    /* ------------------------------------------------------------------------------- */
    private void flushBuffer() throws IOException
    {
        while (_flush.hasRemaining())
        {
            int len=_channel.write(_flush);
            if (len<0)
                throw new IOException("EOF");
            if (len==0)
            {
                // write channel full.  Try letting other threads have a go.
                Thread.yield();
                len=_channel.write(_flush);
                if (len<0)
                    throw new IOException("EOF");
                if (len==0)
                {
                    // still full.  need to  block until it is writable.
                    if (_selector==null)
                     {
                            _selector=Selector.open();
                            _channel.register(_selector,SelectionKey.OP_WRITE);
                     }
                     _selector.select();
                }
            }
        }
    }

    /* ------------------------------------------------------------------------------- */
    public void destroy()
    {
        if (_selector!=null)
        {
            try{_selector.close();}
            catch(IOException e){ LogSupport.ignore(log,e);}
            _selector=null;
            _buffer=null;
            _flush=null;
            _channel=null;
        }
    }
}
