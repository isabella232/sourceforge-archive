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
    public int flush(Buffer header) throws IOException
    {
        // TODO lots of efficiency stuff here to avoid double write
        
        int total=0;
        int length=header.length();
        if (length>0)
            _out.write(header.array(),header.getIndex(),length);
        total=length;
        header.clear();
        
        length=length();
        if (length>0)
            _out.write(array(),getIndex(),length);
        total+=length;
        clear();
        
        return total;
    }
}
