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
import org.mortbay.io.View;

/* ------------------------------------------------------------------------------- */
/**
 * @version $Revision$
 * @author gregw
 */
public class HttpParser implements HttpTokens
{
    // States
    public static final int STATE_START = -11;
    public static final int STATE_FIELD0 = -10;
    public static final int STATE_SPACE1 = -9;
    public static final int STATE_FIELD1 = -8;
    public static final int STATE_SPACE2 = -7;
    public static final int STATE_END0 = -6;
    public static final int STATE_END1 = -5;
    public static final int STATE_FIELD2 = -4;
    public static final int STATE_HEADER = -3;
    public static final int STATE_HEADER_NAME = -2;
    public static final int STATE_HEADER_VALUE = -1;
    public static final int STATE_END = 0;
    public static final int STATE_EOF_CONTENT = 1;
    public static final int STATE_CONTENT = 2;
    public static final int STATE_CHUNKED_CONTENT = 3;
    public static final int STATE_CHUNK_SIZE = 4;
    public static final int STATE_CHUNK_PARAMS = 5;
    public static final int STATE_CHUNK = 6;

    /* ------------------------------------------------------------------------------- */
    protected int _state = STATE_START;
    protected byte _eol;
    protected int _length;
    protected int _contentLength;
    protected int _contentPosition;
    protected int _chunkLength;
    protected int _chunkPosition;

    private EndPoint _endp;
    private Buffer _buffer;
    private boolean _close = false;
    private boolean _content = false;
    private Handler _handler;
    private View _tok0;   // Saved token: header name, request method or response version
    private View _tok1;   // Saved token: header value, request URI or response code
    private String _continuation;
    private boolean _response = false;

    /* ------------------------------------------------------------------------------- */
    /**
     * Constructor.
     */
    public HttpParser(Buffer buffer, EndPoint io, Handler handler)
    {
        this._buffer = buffer;
        this._endp = io;
        this._handler = handler;
        
        if (buffer!=null)
        {
            _tok0 = new View(buffer);
            _tok1 = new View(buffer);
            _tok0.setPutIndex(_tok0.getIndex());
            _tok1.setPutIndex(_tok1.getIndex());
        }
    }

    /* ------------------------------------------------------------------------------- */
    public int getState()
    {
        return _state;
    }    
    
    /* ------------------------------------------------------------------------------- */
    public boolean isState(int state)
    {
        return _state==state;
    }

    /* ------------------------------------------------------------------------------- */
    /** Set the buffer.
     * The builder must be in START or END. A reset is done when a new buffer is passed.
     * @param buffer The buffer or null to remove the buffer.
     */
    public void setBuffer(Buffer buffer)
    {
        if (_state!=STATE_END && _state != STATE_START)
            Portable.throwIllegalState("!START&&!END");
        
        _buffer=buffer;
        if (_buffer!=null)
        {
            _buffer.clear();
            
            if (_tok0==null)
            {
                _tok0 = new View(buffer);
                _tok1 = new View(buffer);
            }
            else
            {
                _tok0.update(buffer);
                _tok1.update(buffer);
            }
            if (_state==STATE_END)
                reset();
        }
    }

    /* ------------------------------------------------------------------------------- */
    public Buffer getBuffer()
    {
        return _buffer;
    }

    /* ------------------------------------------------------------------------------- */
    public void setState(int state)
    {
        this._state = state;
        _contentLength = UNKNOWN_CONTENT;
    }

    /* ------------------------------------------------------------------------------- */
    public boolean inHeaderState()
    {
        return _state < 0;
    }

    /* ------------------------------------------------------------------------------- */
    public boolean inContentState()
    {
        return _state > 0;
    }

    /* ------------------------------------------------------------------------------- */
    public int getContentLength()
    {
        return _contentLength;
    }

    /* ------------------------------------------------------------------------------- */
    public String toString(Buffer buf)
    {
        return "state=" + _state + " length=" + _length + " buf=" + buf.hashCode();
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Parse until END state.
     * 
     * @param handler
     * @param source
     * @return parser state
     */
    public void parse() throws IOException
    {
        _state = STATE_START;

        // continue parsing
        while (_state != STATE_END)
            parseNext();
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Parse until END state.
     * 
     * @param handler
     * @param source
     * @return parser state
     */
    public void parseAvailable() throws IOException
    {
        parseNext();
        // continue parsing
        while (_state != STATE_END && _buffer.length() > 0)
        {
            parseNext();
        }
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * Parse until next Event.
     *  
     */
    public void parseNext() throws IOException
    {
        if (_state == STATE_END) Portable.throwIllegalState("STATE_END");
        if (_state == STATE_CONTENT && _contentPosition == _contentLength)
        {
            _state = STATE_END;
            _handler.messageComplete(_contentPosition);
            return;
        }
        if (_buffer.length() == 0)
        {
            if (_buffer.markIndex() == 0 && _buffer.putIndex() == _buffer.capacity())
                    Portable.throwIO("FULL");
            int filled = -1;
            if (_endp != null)
            {
                if (_buffer.space() == 0)
                {
                    // Compress buffer if handling content, starting or TODO
                    if (_state >= STATE_END || _state == STATE_START || _tok0.length() == 0)
                    {
                        _buffer.compact();
                    }
                    else
                    {
                        int m = _buffer.markIndex();
                        if (m < 0) m = _buffer.getIndex();

                        if (m > _tok0.getIndex())
                        {
                            // TODO - maybe only do this before parsedStartLine call??
                            // compact and adjust remembered tokens
                            int mi = _buffer.markIndex();
                            m = _tok0.getIndex();
                            _buffer.setMarkIndex(m);
                            _buffer.compact();
                            if (mi >= 0)
                                _buffer.setMarkIndex(mi - m);
                            else
                                _buffer.setMarkIndex(-1);

                            _tok0.setMarkIndex(-1);
                            if (_tok0.getIndex() >= m) _tok0.setGetIndex(_tok0.getIndex() - m);
                            if (_tok0.putIndex() >= m) _tok0.setPutIndex(_tok0.putIndex() - m);
                            _tok1.setMarkIndex(-1);
                            if (_tok1.getIndex() >= m) _tok1.setGetIndex(_tok1.getIndex() - m);
                            if (_tok1.putIndex() >= m) _tok1.setPutIndex(_tok1.putIndex() - m);
                        }
                        else
                        {
                            Portable.throwIO("FULL");
                        }
                    }
                }
                filled = _endp.fill(_buffer);
            }
            if (filled < 0 && _state == STATE_EOF_CONTENT)
            {
                _state = STATE_END;
                _handler.messageComplete(_contentPosition);
                return;
            }
            if (filled < 0) Portable.throwIO("EOF");
        }
        byte ch;

        // Handler header
        while (_state < STATE_END && _buffer.length() > 0)
        {
            ch = _buffer.get();
            if (_eol == CARRIAGE_RETURN && ch == LINE_FEED)
            {
                _eol = LINE_FEED;
                continue;
            }
            _eol = 0;
            switch (_state)
            {
                case STATE_START:
                    _contentLength = UNKNOWN_CONTENT;
                    if (ch > SPACE)
                    {
                        _buffer.mark();
                        _state = STATE_FIELD0;
                    }
                    break;

                case STATE_FIELD0:
                    if (ch == SPACE)
                    {
                        _tok0.update(_buffer.markIndex(), _buffer.getIndex() - 1);
                        _state = STATE_SPACE1;
                        continue;
                    }
                    else if (ch < SPACE) 
                    { 
                        Portable.throwIO("BAD");
                    }
                    break;

                case STATE_SPACE1:
                    if (ch > SPACE)
                    {
                        _buffer.mark();
                        _state = STATE_FIELD1;
                        _response = ch >= '1' && ch <= '5';
                    }
                    else if (ch < SPACE) 
                    { 
                        Portable.throwIO("BAD");
                    }
                    break;

                case STATE_FIELD1:
                    if (ch == SPACE)
                    {
                        _tok1.update(_buffer.markIndex(), _buffer.getIndex() - 1);
                        _state = STATE_SPACE2;
                        continue;
                    }
                    else if (ch < SPACE)
                    {
                        // HTTP/0.9
                        _handler.startRequest(HttpMethods.CACHE.lookup(_tok0),
                                _buffer.sliceFromMark(), null);
                        _state = STATE_END;
                        _handler.headerComplete();
                        _handler.messageComplete(_contentPosition);
                        return;
                    }
                    break;

                case STATE_SPACE2:
                    if (ch > SPACE)
                    {
                        _buffer.mark();
                        _state = STATE_FIELD2;
                    }
                    else if (ch < SPACE)
                    {
                        // HTTP/0.9
                        _handler.startRequest(HttpMethods.CACHE.lookup(_tok0), _tok1, null);
                        _state = STATE_END;
                        _handler.headerComplete();
                        _handler.messageComplete(_contentPosition);
                        return;
                    }
                    break;

                case STATE_FIELD2:
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        if (_response)
                            _handler.startResponse(HttpVersions.CACHE.lookup(_tok0), BufferUtil
                                    .toInt(_tok1), _buffer.sliceFromMark());
                        else
                            _handler.startRequest(HttpMethods.CACHE.lookup(_tok0), _tok1,
                                    HttpVersions.CACHE.lookup(_buffer.sliceFromMark()));
                        _eol = ch;
                        _state = STATE_HEADER;
                        _tok0.setPutIndex(_tok0.getIndex());
                        _tok1.setPutIndex(_tok1.getIndex());
                        _continuation=null;
                        return;
                    }
                    break;

                case STATE_HEADER:

                    if (ch == COLON || ch == SPACE || ch == TAB)
                    {
                        // header value without name - continuation?
                        _length = -1;
                        _state = STATE_HEADER_VALUE;
                    }
                    else
                    {
                        // handler last header if any
                        if (_tok0.length()>0 || _tok1.length()>0 || _continuation!=null)
                        {
                            Buffer header = HttpHeaders.CACHE.lookup(_tok0);
                            Buffer value = _continuation==null?(Buffer)_tok1:
                                (Buffer)new ByteArrayBuffer(_continuation);
                            
                            int ho = HttpHeaders.CACHE.getOrdinal(header);
                            if (ho>=0) 
                            {
                                value=HttpHeaderValues.CACHE.lookup(value);
                                int vo = HttpHeaderValues.CACHE.getOrdinal(value);
                            
                                switch (ho)
                                {
                                    case HttpHeaders.CONTENT_LENGTH_ORDINAL:
                                        if (_contentLength != CHUNKED_CONTENT)
                                        {
                                            _contentLength = BufferUtil.toInt(value);
                                            if (_contentLength <= 0) 
                                                _contentLength = HttpParser.NO_CONTENT;
                                        }
                                    break;
                                    
                                    case HttpHeaders.CONNECTION_ORDINAL:
                                        // TODO comma list of connections !!!
                                        _close = (HttpHeaderValues.CLOSE_ORDINAL == vo);
                                    break;
                                    
                                    case HttpHeaders.TRANSFER_ENCODING_ORDINAL:
                                        if (HttpHeaderValues.CHUNKED_ORDINAL == vo)
                                            _contentLength = CHUNKED_CONTENT;
                                        else
                                        {
                                            // TODO avoid string conversion here
                                            String c = value.toString();
                                            if (c.endsWith(HttpHeaderValues.CHUNKED))
                                                _contentLength = CHUNKED_CONTENT;
                                            else if (c.indexOf(HttpHeaderValues.CHUNKED) >= 0) 
                                                Portable.throwIO("BAD");
                                        }
                                    break;
                                    
                                    case HttpHeaders.CONTENT_TYPE_ORDINAL:
                                        _content = true;
                                    break;
                                }
                            }

                            _handler.parsedHeader(header,value);
                            _tok0.setPutIndex(_tok0.getIndex());
                            _tok1.setPutIndex(_tok1.getIndex());
                            _continuation=null;
                        }

                        // now handle ch
                        if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                        {
                            // End of header

                            // work out the content demarcation
                            if (_contentLength == UNKNOWN_CONTENT)
                            {
                                if (_content || _response)
                                    _contentLength = EOF_CONTENT;
                                else
                                    _contentLength = NO_CONTENT;
                            }
                            
                            _contentPosition = 0;
                            _eol = ch;
                            switch (_contentLength)
                            {
                                case HttpParser.EOF_CONTENT:
                                    _state = STATE_EOF_CONTENT;
                                	_handler.headerComplete();
                                    break;
                                case HttpParser.CHUNKED_CONTENT:
                                    _state = STATE_CHUNKED_CONTENT;
                                	_handler.headerComplete();
                                    break;
                                case HttpParser.NO_CONTENT:
                                    _state = STATE_END;
                                	_handler.headerComplete();
                                    _handler.messageComplete(_contentPosition);
                                    break;
                                default:
                                    _state = STATE_CONTENT;
                                	_handler.headerComplete();
                                    break;
                            }
                            return;
                        }
                        else
                        {
                            // New header
                            _length = 1;
                            _buffer.mark();
                            _state = STATE_HEADER_NAME;
                        }
                    }
                    break;

                case STATE_HEADER_NAME:
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        if (_length > 0) _tok0.update(_buffer.markIndex(), _buffer.markIndex()+_length);
                        _eol = ch;
                        _state = STATE_HEADER;
                    }
                    else if (ch == COLON)
                    {
                        if (_length > 0) _tok0.update(_buffer.markIndex(), _buffer.markIndex()+_length);
                        _length = -1;
                        _state = STATE_HEADER_VALUE;
                    }
                    else if (ch != SPACE && ch != TAB)
                    {
                        if (_length == -1) _buffer.mark();
                        _length = _buffer.getIndex() - _buffer.markIndex();
                    }
                    break;

                case STATE_HEADER_VALUE:
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        if (_length > 0) 
                        {
                            if (_tok1.length()==0)
                                _tok1.update(_buffer.markIndex(), _buffer.markIndex()+_length);
                            else
                            {
                                // Continuation line!
                                // TODO - deal with CR LF and COLON?
                                if (_continuation==null)
                                    _continuation=_tok1.toString();
                                _tok1.update(_buffer.markIndex(), _buffer.markIndex()+_length);
                                _continuation+=" "+_tok1.toString();
                            }
                        }
                        _eol = ch;
                        _state = STATE_HEADER;
                    }
                    else if (ch != SPACE && ch != TAB)
                    {
                        if (_length == -1) _buffer.mark();
                        _length = _buffer.getIndex() - _buffer.markIndex();
                    }
                    break;
            }
        } // end of HEADER states loop

        // Handle content
        Buffer chunk;
        while (_state > STATE_END && _buffer.length() > 0)
        {
            if (_eol == CARRIAGE_RETURN && _buffer.peek() == LINE_FEED)
            {
                _eol = _buffer.get();
                continue;
            }
            _eol = 0;
            switch (_state)
            {
                case STATE_EOF_CONTENT:
                    chunk = _buffer.get(_buffer.length());
                    _handler.content(_contentPosition, chunk);
                    _contentPosition += chunk.length();
                    return;

                case STATE_CONTENT:
                    {
                        int length = _buffer.length();
                        int remaining = _contentLength - _contentPosition;
                        if (remaining == 0)
                        {
                            _state = STATE_END;
                            _handler.messageComplete(_contentPosition);
                            return;
                        }
                        else if (length > remaining) length = remaining;
                        chunk = _buffer.get(length);
                        _handler.content(_contentPosition, chunk);
                        _contentPosition += chunk.length();
                    }
                    return;

                case STATE_CHUNKED_CONTENT:
                    ch = _buffer.peek();
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                        _eol = _buffer.get();
                    else if (ch <= SPACE)
                        _buffer.get();
                    else
                    {
                        _chunkLength = 0;
                        _chunkPosition = 0;
                        _state = STATE_CHUNK_SIZE;
                    }
                    break;

                case STATE_CHUNK_SIZE:
                    ch = _buffer.get();
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        _eol = ch;
                        if (_chunkLength == 0)
                        {
                            _state = STATE_END;
                            _handler.messageComplete(_contentPosition);
                            return;
                        }
                        else
                            _state = STATE_CHUNK;
                    }
                    else if (ch <= SPACE || ch == SEMI_COLON)
                        _state = STATE_CHUNK_PARAMS;
                    else if (ch >= '0' && ch <= '9')
                        _chunkLength = _chunkLength * 16 + (ch - '0');
                    else if (ch >= 'a' && ch <= 'f')
                        _chunkLength = _chunkLength * 16 + (10 + ch - 'a');
                    else if (ch >= 'A' && ch <= 'F')
                        _chunkLength = _chunkLength * 16 + (10 + ch - 'A');
                    else
                        Portable.throwRuntime("bad chunk char: " + ch);
                    break;

                case STATE_CHUNK_PARAMS:
                    ch = _buffer.get();
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        _eol = ch;
                        if (_chunkLength == 0)
                        {
                            _state = STATE_END;
                            _handler.messageComplete(_contentPosition);
                            return;
                        }
                        else
                            _state = STATE_CHUNK;
                    }
                    break;

                case STATE_CHUNK:
                    {
                        int length = _buffer.length();
                        int remaining = _chunkLength - _chunkPosition;
                        if (remaining == 0)
                        {
                            _state = STATE_CHUNKED_CONTENT;
                            break;
                        }
                        else if (length > remaining) length = remaining;
                        chunk = _buffer.get(length);
                        _handler.content(_contentPosition, chunk);
                        _contentPosition += chunk.length();
                        _chunkPosition += chunk.length();
                    }
                    return;
            }
        }
    }

    /* ------------------------------------------------------------------------------- */
    public void reset()
    {
        _state = STATE_START;
        _contentLength = UNKNOWN_CONTENT;
        _contentPosition = 0;
        _length = 0;
        _close = false;
        _content = false;
        _tok0.clear();
        _tok1.clear();
        _response = false;
    }

    public static abstract class Handler
    {
        /**
         * This is the method called by parser when the HTTP request line is parsed
         */
        public abstract void startRequest(Buffer method, Buffer url, Buffer version)
    	throws IOException;

        /**
         * This is the method called by parser when the HTTP request line is parsed
         */
        public abstract void startResponse(Buffer version, int status, Buffer reason)
    	throws IOException;

        /**
         * This is the method called by parser when a HTTP Header name and value is found
         */
        public void parsedHeader(Buffer name,Buffer value)
    	throws IOException
        {
        }

        public void headerComplete()
    	throws IOException
        {
        }

        public void content(int index, Buffer ref)
        	throws IOException
        {
        }

        public void messageComplete(int contextLength)
        	throws IOException
        {
        }
    }
}
