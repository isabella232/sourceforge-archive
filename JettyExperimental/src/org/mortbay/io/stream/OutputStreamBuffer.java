/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.io.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.OutBuffer;

/* ------------------------------------------------------------------------------- */
/**
 * 
 */
public class OutputStreamBuffer extends ByteArrayBuffer implements OutBuffer
{
    private Socket _socket;
    private OutputStream _out;
    
    public OutputStreamBuffer(Socket socket, int size) throws IOException
    {
        super(size);
        _socket=socket;
        _out=socket.getOutputStream();
    }
    
    public OutputStreamBuffer(OutputStream out, int size) throws IOException
    {
        super(size);
        _out=out;
    }

    /* 
     * @see org.mortbay.io.OutBuffer#isClosed()
     */
    public boolean isClosed()
    {
        if (_socket!=null)
            return _socket.isClosed() || _socket.isOutputShutdown();
        return _out==null;
    }

    /* 
     * @see org.mortbay.io.OutBuffer#close()
     */
    public void close() throws IOException
    {
        _out.close();
        _out=null;
    }

    /* 
     * @see org.mortbay.io.OutBuffer#flush()
     */
    public int flush() throws IOException
    {
        if (_out==null)
            return -1;
        int length=length();
        if (length>0)
            _out.write(array(),getIndex(),length);
        clear();
        return length;
    }

    /* 
     * @see org.mortbay.io.OutBuffer#flush(org.mortbay.io.Buffer)
     */
    public int flush(Buffer header, Buffer trailer) throws IOException
    {
        // TODO lots of efficiency stuff here to avoid double write

        int total=0;
        
        // See if the header buffer will fit in front of buffer content.
        int length=header==null?0:header.length();
        if (length>0 && length<=getIndex())
        {
            int pi=putIndex();
            setGetIndex(getIndex()-length);
            setPutIndex(getIndex());
            put(header);
            setPutIndex(pi);
        }
        else if (length>0)
        {
            _out.write(header.array(),header.getIndex(),length);
            total=length;
        }
        header.clear();

        // See if the trailer buffer will fit in front of buffer content.
        length=trailer==null?0:trailer.length();
        if (length>0 && length<=space())
        {
            put(trailer); 
            trailer.clear();
        }
        
        length=length();
        if (length>0)
            _out.write(array(),getIndex(),length);
       
        total+=length;
        clear();
        
        // write trailer if it was not stuffed in the buffer.
        length=trailer==null?0:trailer.length();
        if (length>0 && length<=space())
        {
            _out.write(trailer.array(),trailer.getIndex(),length);
            total+=length;
            trailer.clear();
        }
        
        return total;
    }
}
