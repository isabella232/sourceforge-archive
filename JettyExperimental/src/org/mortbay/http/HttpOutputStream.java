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
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.OutBuffer;
import org.mortbay.io.Portable;

/* ------------------------------------------------------------------------------- */
/**
 * 
 */
public class HttpOutputStream extends OutputStream
{

    /**
     * 
     */
    public HttpOutputStream(Buffer buffer, int maxHeaderSize)
    {
        _buffer=buffer;
        if (buffer instanceof OutBuffer)
            _outBuffer=(OutBuffer)buffer;
        _header=new HttpHeader();
        _headerBuffer=new ByteArrayBuffer(maxHeaderSize);
        _buffer.setPutIndex(maxHeaderSize);
        _buffer.setGetIndex(maxHeaderSize);
    }

    /* 
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException
    {
        if (_closed)
            return;
            
        flush(true); 
        _closed=true;
    }

    /**
     */
    private void completeHeader(boolean closing)
    {
        if (_contentLength!=UNKNOWN_CONTENT)
            return;
            
        // From RFC 2616  4.4:
        // 1. No body for 1xx, 204, 304 & HEAD response
        // 2. If Transfer-Encoding!=identity && HTTP/1.1 && !Connection==close then chunk
        // 3. Content-Length
        // 4. multipart/byteranges
        // 5. close

        // get common values
        int version = _header.getVersionOrdinal();
        int status= _header.getStatus();
        Buffer connection = _header.get(HttpHeaders.CONNECTION_BUFFER);
        _close=HttpHeaderValues.CLOSE_BUFFER.equals(connection);
        Buffer transferEncoding = _header.get(HttpHeaders.TRANSFER_ENCODING_BUFFER);
        boolean identity=HttpHeaderValues.IDENTITY_BUFFER.equals(transferEncoding);
        _chunking=HttpHeaderValues.CHUNKED_BUFFER.equals(transferEncoding);
        
        // 1. check status
        if (status>=100 && status<=199 || status==204 || status==304)
            _contentLength=NO_CONTENT;
            
        // 2. check transfer encoding
        else if (!identity && version>=HttpVersions.HTTP_1_1_ORDINAL && !_close)
        {
            _contentLength=CHUNKED_CONTENT;
            if (!_chunking)
                _header.put(HttpHeaders.TRANSFER_ENCODING_BUFFER, HttpHeaderValues.CHUNKED_BUFFER);
            _header.remove(HttpHeaders.CONTENT_LENGTH_BUFFER);
            _chunking=true;
        }
        
        // 3. check content-length
        else
        {
            int content_length=_header.getIntField(HttpHeaders.CONTENT_LENGTH_BUFFER);
            if (content_length>=0)
                _contentLength=content_length;
            else if (content_length<0 && closing)
            {
                // We can force the content length
                _contentLength=_buffer.length();
                ByteArrayBuffer len = new ByteArrayBuffer(12);
                BufferUtil.putDecInt(len, _contentLength);
                _header.put(HttpHeaders.CONTENT_LENGTH_BUFFER,len);
            }
            // 4. multipart/byteranges
            else
            {
                Buffer content_type=_header.get(HttpHeaders.CONTENT_TYPE_BUFFER);
                if (HttpHeaderValues.MULTIPART_BYTERANGES_BUFFER.equals(content_type))
                    _contentLength=SELF_DEFINING_CONTENT;
                    
                // 5. EOF
                else
                {
                    _contentLength=EOF_CONTENT;
                    if (!_close)
                    {
                        _header.put(HttpHeaders.CONNECTION_BUFFER, HttpHeaderValues.CLOSE_BUFFER);
                        _closed=true;
                    }
                }
            }
        }
        
        // 1. HEAD response
        if (_headResponse)
            _contentLength=DISCARD_CONTENT;
    }
    
    /* 
     * @see java.io.OutputStream#flush()
     */
    public void flush() throws IOException
    {
        flush(false);
    }

    /**
     * 
     * @param last True if this is the last flush call.
     * @throws IOException
     */
    private void flush(boolean last) throws IOException
    {
        if (!_committed)
        {
            // calculate how to terminate connection.
            completeHeader(false);
            
            // generate the header buffer.
            // TODO - maybe do this directly to real buffer?
            _headerBuffer.clear();
            _header.put(_headerBuffer);
            _committed=true;
        }
                
        Buffer after_buffer=null;
        
        if (_chunking)
        {  
            BufferUtil.putHexInt(_headerBuffer,_buffer.length());
            BufferUtil.putCRLF(_headerBuffer);
            
            after_buffer=last?LAST_CHUNK:END_CHUNK;
            
            if (_buffer.space()>=after_buffer.length())
            {
                _buffer.put(after_buffer);
                after_buffer=null;
            }
        }
            
        // Actually do the flush & perhaps close
        if (_outBuffer != null)
        {
            if (_headerBuffer.hasContent())
                _outBuffer.flush(_headerBuffer);
            else
                _outBuffer.flush();

            if (last)
            {
                // Force all the data out.
                while (_buffer.length() > 0)
                    _outBuffer.flush();

                if (after_buffer!=null)
                {
                    _outBuffer.put(after_buffer);
                    while (_buffer.length() > 0)
                        _outBuffer.flush();
                    after_buffer=null;
                }
                
                if (_close)
                    _outBuffer.close();
                _closed=true;
            }
            
            if (_chunking && !_buffer.hasContent())
            {
                _buffer.setPutIndex(CHUNK_HEADER_SIZE);
                _buffer.setGetIndex(CHUNK_HEADER_SIZE);
            }
            
            
            // TODO write any remaining content-length (or just close??)
        }
    }

    public HttpHeader getHttpHeader()
    {
        return _header;
    }

    /**
     * @return True if header has been committed.
     */
    public boolean isCommitted()
    {
        return _committed;
    }
    
    public void reset()
    {
        // TODO - check if this is OK and if underlying stream is OK
        _buffer.clear();
        _headerBuffer.clear();
        _contentLength=UNKNOWN_CONTENT;
        _headResponse=false;
        _chunking=false;
        _committed=false;
        _closed=false;
        _close=false;
        _header.clear();

        _buffer.setPutIndex(_headerBuffer.capacity());
        _buffer.setGetIndex(_headerBuffer.capacity());
    }

    public void resetBuffer()
    {
        if (_contentLength>=0)
            _contentLength+=_buffer.length();
        _buffer.clear();
    }
    
    public void setHeadResponse(boolean h)
    {
        if (isCommitted())
            Portable.throwIllegalState("committed");
        _headResponse=h;
    }
    
    /* 
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    public void write(byte[] b, int offset, int length) throws IOException
    {
        if (_closed)
            Portable.throwIO("closed");
            
        // TODO do buffering bypass handling
        
        while (length >0)
        {
            int space=_buffer.space();
            if (space<=(_chunking?2:0))
            {
                flush(false);
                space=_buffer.space();
                if (space==0)
                    Portable.throwIO("no space in buffer");
            }
            
            int len=space<length?space:length;
            if (_contentLength>0 && len>_contentLength)
                len=_contentLength;
            else if (_contentLength==NO_CONTENT)
                Portable.throwIO("too much content");
                
            _buffer.put(b,offset,len);
            length-=len;
            offset+=len;
            if (_contentLength>0)
                _contentLength-=len;
        }
    }

    /* 
     * @see java.io.OutputStream#write(int)
     */
    public void write(int b) throws IOException
    {
        if (_closed)
            Portable.throwIO("closed");
      
        if (_contentLength==NO_CONTENT)
            Portable.throwIO("too much content");
            
        if (_buffer.space()<=(_chunking?2:0))
            flush(false);
        _buffer.put((byte)b);

        if (_contentLength>0)
            _contentLength--;
    }
    
    private Buffer _buffer;
    private boolean _chunking;
    private boolean _close;
    private boolean _closed;
    private boolean _committed;
    private int _contentLength=UNKNOWN_CONTENT;
    private HttpHeader _header;
    private Buffer _headerBuffer;
    private boolean _headResponse;
    private OutBuffer _outBuffer;
    
    public static final int 
        DISCARD_CONTENT=HttpParser.UNKNOWN_CONTENT-2,
        SELF_DEFINING_CONTENT=HttpParser.UNKNOWN_CONTENT-1,
        UNKNOWN_CONTENT=HttpParser.UNKNOWN_CONTENT,
        NO_CONTENT=HttpParser.NO_CONTENT,
        EOF_CONTENT=HttpParser.EOF_CONTENT,
        CHUNKED_CONTENT=HttpParser.CHUNKED_CONTENT;
        
    private static final int CHUNK_HEADER_SIZE=8;

    private static Buffer LAST_CHUNK = new ByteArrayBuffer("0\015\012\015\012");
    private static Buffer END_CHUNK = new ByteArrayBuffer("\015\012");
}
