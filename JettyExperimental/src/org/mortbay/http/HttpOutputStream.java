/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import java.io.IOException;
import java.io.OutputStream;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.OutBuffer;

/* ------------------------------------------------------------------------------- */
/**
 * 
 */
public class HttpOutputStream extends OutputStream
{

    /**
     * 
     */
    public HttpOutputStream()
    {
    }

    /* 
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException
    {
        if (!isCommitted())
            checkContentBoundary(true);
        flush();
    }

    /**
     */
    private void checkContentBoundary(boolean closed)
    {
        // check status
        
        // check hasContent
        // check connection header
        // check content-length header
        // check transfer-encoding header
        
        // TODO Auto-generated method stub
        
    }

    /* 
     * @see java.io.OutputStream#flush()
     */
    public void flush() throws IOException
    {
        if (!isCommitted())
            commit();
                
        if (_chunking)
            makeChunk();
            
        if (_headerBuffer.hasContent())
                _buffer.flush(_headerBuffer);
        else
                _buffer.flush();     
    }

    /**
     * 
     */
    private void commit() throws IOException
    {
        if (_committed)
            return;
        
        // calculate how to terminate connection.
        checkContentBoundary(false);
            
        // write the header.
        _headerBuffer.clear();
        _header.put(_headerBuffer);
        _committed=true;
    }

    /**
     * 
     */
    private void makeChunk()
    {
        BufferUtil.putHexInt(_headerBuffer,_buffer.length());
        BufferUtil.putCRLF(_headerBuffer);
        if (_buffer.space()>=2)
            BufferUtil.putCRLF(_buffer);
    }

    /**
     * @return
     */
    private boolean isCommitted()
    {
        return _committed;
    }

    /* 
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] b, int off, int len) throws IOException
    {
        if (_buffer.space()<len)
            flush();
        super.write(b, off, len);
    }

    /* 
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException
    {
        if (_buffer.space()<1)
            flush();
        _buffer.put((byte)b);
    }
    
    
    private HttpHeader _header;
    private OutBuffer _buffer;
    private Buffer _headerBuffer;
    private boolean _committed;
    private boolean _chunking;
}
