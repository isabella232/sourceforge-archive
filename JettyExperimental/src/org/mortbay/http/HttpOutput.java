/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import java.io.IOException;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.OutBuffer;
import org.mortbay.io.Portable;

/* ------------------------------------------------------------------------------- */
/**
 * HTTP Output class.
 * This implementation is written in non-blocking style, but will block
 * if the passed buffer has a blocking flush();
 */
public class HttpOutput
{
    public HttpOutput(Buffer buffer, int headerReserve)
    {
        _buffer=buffer;
        if (buffer instanceof OutBuffer)
            _outBuffer=(OutBuffer)buffer;
        _header=new HttpHeader();
        _headerBuffer=new ByteArrayBuffer(headerReserve);
        _trailerBuffer=new ByteArrayBuffer(LAST_CHUNK.length());
        _buffer.setPutIndex(headerReserve);
        _buffer.setGetIndex(headerReserve);
    }
    
    public Buffer getBuffer()
    {
        return _buffer;
    }

    /* 
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException
    {
        if (_closed)
            return;
        _closing=true;
        flush(); 
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
    public boolean flush() throws IOException
    {
        if (_closed && !_flushing)
            return true;
            
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
        
        // handle HEAD
        if (_headResponse)
        {   
            _outBuffer.clear();
            _trailerBuffer.clear();
        }
        
        // handle chunking
        else if (!_flushing && _chunking)
        {  
            BufferUtil.putHexInt(_headerBuffer,_buffer.length());
            BufferUtil.putCRLF(_headerBuffer);
            
            Buffer trailer=_closing?LAST_CHUNK:CRLF;
            
            // If there is space, stuff the trailer to avoid
            // copy to trailerBuffer
            if (_buffer.space()>=trailer.length())
                _buffer.put(trailer);
            else
                _trailerBuffer.put(trailer);
        }
            
        // Actually do the flush & perhaps close
        _flushing=true;
        if (_outBuffer != null)
        {    
            _outBuffer.flush(_headerBuffer,_trailerBuffer);
            _flushing= 
                _headerBuffer.hasContent() ||
                _outBuffer.hasContent() ||
                _trailerBuffer.hasContent();
            
            if (!_flushing)
            {
                if (_closing)
                {
                    if (_close)
                        _outBuffer.close();
                    _closed=true;
                    _closing=false;
                    // TODO write any remaining content-length (or just close??)
                }
            
                if (_chunking && !_buffer.hasContent())
                {
                    _buffer.setPutIndex(CHUNK_HEADER_SIZE);
                    _buffer.setGetIndex(CHUNK_HEADER_SIZE);
                }
            }      
        }
        
        return !_flushing;
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
    
    public boolean isClosed()
    {
        return _closed;
    }
    
    public boolean isFlushing()
    {
        return _flushing;
    }
    
    public void reset()
    {
        // TODO - check if this is OK and if underlying stream is OK
        _buffer.clear();
        _headerBuffer.clear();
        _trailerBuffer.clear();
        _contentLength=UNKNOWN_CONTENT;
        _headResponse=false;
        _chunking=false;
        _committed=false;
        _flushing=false;
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
    
    public int write(byte[] b, int offset, int length) throws IOException
    {
        if (_closed)
            Portable.throwIO("closed");
            
        // TODO do buffering bypass handling
        
        int space=_buffer.space();
        if (_flushing || space<=(_chunking?2:0))
            return 0;
            
        int len=space<length?space:length;
        if (_contentLength>0 && len>_contentLength)
            len=_contentLength;
        else if (_contentLength==NO_CONTENT)
            Portable.throwIO("too much content");
                
        _buffer.put(b,offset,len);
       
        if (_contentLength>0)
          _contentLength-=len;
        return len;
    }

    
    private Buffer _buffer;
    private boolean _chunking;
    private boolean _close;
    private boolean _closing;
    private boolean _closed;
    private boolean _committed;
    private boolean _flushing;
    private boolean _headResponse;
    private int _contentLength=UNKNOWN_CONTENT;
    private HttpHeader _header;
    private Buffer _headerBuffer;
    private Buffer _trailerBuffer;
    private OutBuffer _outBuffer;
    
    public static final int 
        DISCARD_CONTENT=HttpParser.UNKNOWN_CONTENT-2,
        SELF_DEFINING_CONTENT=HttpParser.UNKNOWN_CONTENT-1,
        UNKNOWN_CONTENT=HttpParser.UNKNOWN_CONTENT,
        NO_CONTENT=HttpParser.NO_CONTENT,
        EOF_CONTENT=HttpParser.EOF_CONTENT,
        CHUNKED_CONTENT=HttpParser.CHUNKED_CONTENT;
        
    private static final int CHUNK_HEADER_SIZE=8;

    private static Buffer LAST_CHUNK = new ByteArrayBuffer("\015\0120\015\012\015\012");
    private static Buffer CRLF = new ByteArrayBuffer("\015\012");
}
