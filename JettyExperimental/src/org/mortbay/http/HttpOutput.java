// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.http;


import java.io.IOException;
import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.Portable;

/* ------------------------------------------------------------------------------- */
/**
 * HTTP Output class. This implementation is written in non-blocking style, but will block if the
 * passed buffer has a blocking flush();
 */
public class HttpOutput
{
    private static final int CHUNK_HEADER_SIZE=8;
    private static Buffer CRLF=new ByteArrayBuffer("\015\012");
    private static Buffer CRLF_LAST_CHUNK=new ByteArrayBuffer("\015\0120\015\012\015\012");
    private static Buffer LAST_CHUNK=new ByteArrayBuffer("0\015\012\015\012");
    public static final int
            SELF_DEFINING_CONTENT=HttpParser.UNKNOWN_CONTENT-1,
            UNKNOWN_CONTENT=HttpParser.UNKNOWN_CONTENT,
            NO_CONTENT=HttpParser.NO_CONTENT,
            EOF_CONTENT=HttpParser.EOF_CONTENT,
            CHUNKED_CONTENT=HttpParser.CHUNKED_CONTENT;
    private static Buffer SEND_CONTINUE=new ByteArrayBuffer("HTTP/1.1 100 Continue\015\012\015\012");
 
    private Buffer _buffer;
    private EndPoint _endp;
    private boolean _chunking;
    private boolean _closed;
    private boolean _closing;
    private boolean _committed;
    private int _contentLength=UNKNOWN_CONTENT;
    private boolean _flushing;
    private HttpHeader _header;
    private Buffer _headerBuffer;
    private boolean _headResponse;
    private boolean _persistent=true;
    private Buffer _trailerBuffer;
    private int _version=HttpVersions.HTTP_1_1_ORDINAL;

    public HttpOutput(Buffer buffer,org.mortbay.io.EndPoint endp,HttpHeader header, int headerReserve)
    {
        _buffer=buffer;
        _endp=endp;
        _header=header;
        _headerBuffer=new ByteArrayBuffer(headerReserve);
        _trailerBuffer=new ByteArrayBuffer(LAST_CHUNK.length());
        _buffer.setPutIndex(headerReserve);
        _buffer.setGetIndex(headerReserve);
    }

    /*
     * @see java.io.OutputStream#close()
     */
    public void close() throws IOException
    {
        if(_closed)
            return;
        _closing=true;
        flush();
    }

    /**
     */
    private void completeHeader()
    {
        if(_contentLength!=UNKNOWN_CONTENT)
            return;

        // From RFC 2616 4.4:
        // 1. No body for 1xx, 204, 304 & HEAD response
        // 2. Force content-length?
        // 3. If Transfer-Encoding!=identity && HTTP/1.1 && !Connection==close then chunk
        // 4. Content-Length
        // 5. multipart/byteranges
        // 6. close

        // get common values
        int status=_header.getStatus();
        Buffer connection=_header.get(HttpHeaders.CONNECTION_BUFFER);
        _persistent=connection!=null
        ?(!HttpHeaderValues.CLOSE_BUFFER.equals(connection))
        :(_version>=HttpVersions.HTTP_1_1_ORDINAL);
        Buffer transferEncoding=_header.get(HttpHeaders.TRANSFER_ENCODING_BUFFER);
        boolean identity=HttpHeaderValues.IDENTITY_BUFFER.equals(transferEncoding);
        _chunking=HttpHeaderValues.CHUNKED_BUFFER.equals(transferEncoding);
        _contentLength=_header.getIntField(HttpHeaders.CONTENT_LENGTH_BUFFER);

        // 1. check status
        if(status>=100&&status<=199||status==204||status==304)
            _contentLength=NO_CONTENT;

        // 2. Force content-length
        else if(_persistent&&_closing&&_contentLength<0)
        {
            _contentLength=_buffer.length();
            ByteArrayBuffer len=new ByteArrayBuffer(12);
            BufferUtil.putDecInt(len,_contentLength);
            _header.put(HttpHeaders.CONTENT_LENGTH_BUFFER,len);
        }
        // 3. check transfer encoding
        else if(_contentLength<0&&!identity&&_version>=HttpVersions.HTTP_1_1_ORDINAL&&_persistent)
        {
            _contentLength=CHUNKED_CONTENT;
            if(!_chunking)
                _header.put(HttpHeaders.TRANSFER_ENCODING_BUFFER,HttpHeaderValues.CHUNKED_BUFFER);
            _header.remove(HttpHeaders.CONTENT_LENGTH_BUFFER);
            _chunking=true;
        }
        // 4. check content-length
        else if(_contentLength<0)
        {
            // 5. multipart/byteranges
            Buffer content_type=_header.get(HttpHeaders.CONTENT_TYPE_BUFFER);
            if(HttpHeaderValues.MULTIPART_BYTERANGES_BUFFER.equals(content_type))
                _contentLength=SELF_DEFINING_CONTENT;

            // 6. EOF
            else
            {
                _contentLength=EOF_CONTENT;
                if(_persistent)
                {
                    _header.put(HttpHeaders.CONNECTION_BUFFER,HttpHeaderValues.CLOSE_BUFFER);
                    _persistent=false;
                }
            }
        }
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * destroy.
     */
    public void destroy()
    {
        _buffer=null;
        _header=null;
        _headerBuffer=null;
        _trailerBuffer=null;
        _endp=null;
        _persistent=false;
    }

    /*
     * @see java.io.OutputStream#flush()
     */
    public boolean flush() throws IOException
    {
        if(_closed&&!_flushing)
            return true;
        if(!_committed)
        {
            // calculate how to terminate connection.
            completeHeader();

            // generate the header buffer.
            // TODO - maybe do this directly to real buffer?
            _headerBuffer.clear();
            if(_version>HttpVersions.HTTP_0_9_ORDINAL)
                _header.put(_headerBuffer);
            _committed=true;
        }

        // handle HEAD
        if(_headResponse)
        {
            // TODO - this is almost certainly NOT correct!
            _buffer.clear();
            _trailerBuffer.clear();
        }

        // handle chunking
        else if(!_flushing&&_chunking)
        {
            Buffer trailer=null;
            if(_buffer.length()>0)
            {
                BufferUtil.putHexInt(_headerBuffer,_buffer.length());
                BufferUtil.putCRLF(_headerBuffer);
                trailer=_closing?CRLF_LAST_CHUNK:CRLF;
            }
            else if(_closing)
                trailer=LAST_CHUNK;

            // If there is space, stuff the trailer to avoid
            // copy to trailerBuffer
            if(trailer!=null)
            {
                if(_buffer.space()>=trailer.length())
                    _buffer.put(trailer);
                else
                    _trailerBuffer.put(trailer);
            }
        }

        // Actually do the flush & perhaps close
        _flushing=true;
        if(_endp!=null)
        {
            _endp.flush(_headerBuffer,_buffer,_trailerBuffer);
            _flushing=
            _headerBuffer.hasContent()||
            _buffer.hasContent()||
            _trailerBuffer.hasContent();
            if(!_flushing)
            {
                if(_closing)
                {
                    if(!_persistent)
                        _endp.close();
                    _closed=true;
                    _closing=false;
                    // TODO write any remaining content-length (or just close??)
                }
                if(_chunking&&!_buffer.hasContent())
                {
                    _buffer.setPutIndex(CHUNK_HEADER_SIZE);
                    _buffer.setGetIndex(CHUNK_HEADER_SIZE);
                }
            }
        }
        return !_flushing;
    }

    public Buffer getBuffer()
    {
        return _buffer;
    }

    public HttpHeader getHttpHeader()
    {
        return _header;
    }

    public boolean isClosed()
    {
        return _closed;
    }

    /**
     * @return True if header has been committed.
     */
    public boolean isCommitted()
    {
        return _committed;
    }

    public boolean isFlushing()
    {
        return _flushing;
    }

    public boolean isPersitent()
    {
        return _persistent;
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
        _persistent=true;
        _closed=false;
        _header.clear();
        _version=HttpVersions.HTTP_1_1_ORDINAL;
        _buffer.setPutIndex(_headerBuffer.capacity());
        _buffer.setGetIndex(_headerBuffer.capacity());
    }

    public void resetBuffer()
    {
        if(_contentLength>=0)
            _contentLength+=_buffer.length();
        _buffer.clear();
    }

    public void sendContinue()
    throws IOException
    {
        if(_endp!=null)
        {
            _buffer.put(SEND_CONTINUE);
            while(_buffer.length()>0)
                // TODO - need to fix these busy loops!
                _endp.flush(_buffer);
        }
    }

    public void setHeadResponse(boolean h)
    {
        if(isCommitted())
            Portable.throwIllegalState("committed");
        _headResponse=h;
    }

    /**
     * Set the output version. For responses, the version may differ from that indicated by the
     * header.
     * 
     * @param ordinal HttpVersions ordinal
     */
    public void setVersionOrdinal(int ordinal)
    {
        _version=ordinal;
    }

    public int write(byte[] b,int offset,int length) throws IOException
    {
        if(_closed)
            Portable.throwIO("closed");

        // TODO do buffering bypass handling

        int space=_buffer.space();
        if(_flushing||space<=(_chunking?2:0))
            return 0;
        int len=space<length?space:length;
        if(_contentLength>0&&len>_contentLength)
            len=_contentLength;
        else if(_contentLength==NO_CONTENT)
            Portable.throwIO("too much content");
        _buffer.put(b,offset,len);
        if(_contentLength>0)
            _contentLength-=len;
        return len;
    }
}
