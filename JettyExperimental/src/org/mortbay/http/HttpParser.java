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
    protected int state = STATE_START;
    protected byte eol;
    protected int length;
    protected int contentLength;
    protected int contentPosition;
    protected int chunkLength;
    protected int chunkPosition;

    private EndPoint endp;
    private Buffer buffer;
    private boolean close = false;
    private boolean content = false;
    private Handler handler;
    private View tok0;
    private View tok1;
    private String continuation;
    private boolean response = false;

    /* ------------------------------------------------------------------------------- */
    /**
     * Constructor.
     */
    public HttpParser(Buffer buffer, EndPoint io, Handler handler)
    {
        this.buffer = buffer;
        this.endp = io;
        this.handler = handler;
        tok0 = new View(buffer);
        tok1 = new View(buffer);
        tok0.setPutIndex(tok0.getIndex());
        tok1.setPutIndex(tok1.getIndex());
    }

    /* ------------------------------------------------------------------------------- */
    public int getState()
    {
        return state;
    }

    /* ------------------------------------------------------------------------------- */
    public void setState(int state)
    {
        this.state = state;
        contentLength = UNKNOWN_CONTENT;
    }

    /* ------------------------------------------------------------------------------- */
    public boolean inHeaderState()
    {
        return state < 0;
    }

    /* ------------------------------------------------------------------------------- */
    public boolean inContentState()
    {
        return state > 0;
    }

    /* ------------------------------------------------------------------------------- */
    public int getContentLength()
    {
        return contentLength;
    }

    /* ------------------------------------------------------------------------------- */
    public String toString(Buffer buf)
    {
        return "state=" + state + " length=" + length + " buf=" + buf.hashCode();
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
        state = STATE_START;

        // continue parsing
        while (state != STATE_END)
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
        while (state != STATE_END && buffer.length() > 0)
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
        if (state == STATE_END) Portable.throwIllegalState("STATE_END");
        if (state == STATE_CONTENT && contentPosition == contentLength)
        {
            state = STATE_END;
            handler.messageComplete(contentPosition);
            return;
        }
        if (buffer.length() == 0)
        {
            if (buffer.markIndex() == 0 && buffer.putIndex() == buffer.capacity())
                    Portable.throwIO("FULL");
            int filled = -1;
            if (endp != null)
            {
                if (buffer.space() == 0)
                {
                    if (state >= STATE_END || state == STATE_START || tok0.length() == 0)
                    {
                        buffer.compact();
                    }
                    else
                    {
                        int m = buffer.markIndex();
                        if (m < 0) m = buffer.getIndex();

                        if (m > tok0.getIndex())
                        {
                            // TODO - maybe only do this before parsedStartLine call??
                            // compact and adjust remembered tokens
                            int mi = buffer.markIndex();
                            m = tok0.getIndex();
                            buffer.setMarkIndex(m);
                            buffer.compact();
                            if (mi >= 0)
                                buffer.setMarkIndex(mi - m);
                            else
                                buffer.setMarkIndex(-1);

                            tok0.setMarkIndex(-1);
                            if (tok0.getIndex() >= m) tok0.setGetIndex(tok0.getIndex() - m);
                            if (tok0.putIndex() >= m) tok0.setPutIndex(tok0.putIndex() - m);
                            tok1.setMarkIndex(-1);
                            if (tok1.getIndex() >= m) tok1.setGetIndex(tok1.getIndex() - m);
                            if (tok1.putIndex() >= m) tok1.setPutIndex(tok1.putIndex() - m);
                        }
                        else
                        {
                            System.err.println(((ByteArrayBuffer) buffer).toDetailString());
                            Portable.throwIO("FULL");
                        }
                    }
                }
                filled = endp.fill(buffer);
            }
            if (filled < 0 && state == STATE_EOF_CONTENT)
            {
                state = STATE_END;
                handler.messageComplete(contentPosition);
                return;
            }
            if (filled < 0) Portable.throwIO("EOF");
        }
        byte ch;

        // Handler header
        while (state < STATE_END && buffer.length() > 0)
        {
            ch = buffer.get();
            if (eol == CARRIAGE_RETURN && ch == LINE_FEED)
            {
                eol = LINE_FEED;
                continue;
            }
            eol = 0;
            switch (state)
            {
                case STATE_START:
                    contentLength = UNKNOWN_CONTENT;
                    if (ch > SPACE)
                    {
                        buffer.mark();
                        state = STATE_FIELD0;
                    }
                    break;

                case STATE_FIELD0:
                    if (ch == SPACE)
                    {
                        tok0.update(buffer.markIndex(), buffer.getIndex() - 1);
                        state = STATE_SPACE1;
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
                        buffer.mark();
                        state = STATE_FIELD1;
                        response = ch >= '1' && ch <= '5';
                    }
                    else if (ch < SPACE) 
                    { 
                        Portable.throwIO("BAD");
                    }
                    break;

                case STATE_FIELD1:
                    if (ch == SPACE)
                    {
                        tok1.update(buffer.markIndex(), buffer.getIndex() - 1);
                        state = STATE_SPACE2;
                        continue;
                    }
                    else if (ch < SPACE)
                    {
                        // HTTP/0.9
                        handler.startRequest(HttpMethods.CACHE.lookup(tok0),
                                buffer.sliceFromMark(), null);
                        handler.headerComplete();
                        state = STATE_END;
                        handler.messageComplete(contentPosition);
                        return;
                    }
                    break;

                case STATE_SPACE2:
                    if (ch > SPACE)
                    {
                        buffer.mark();
                        state = STATE_FIELD2;
                    }
                    else if (ch < SPACE)
                    {
                        // HTTP/0.9
                        handler.startRequest(HttpMethods.CACHE.lookup(tok0), tok1, null);
                        handler.headerComplete();
                        state = STATE_END;
                        handler.messageComplete(contentPosition);
                        return;
                    }
                    break;

                case STATE_FIELD2:
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        if (response)
                            handler.startResponse(HttpVersions.CACHE.lookup(tok0), BufferUtil
                                    .toInt(tok1), buffer.sliceFromMark());
                        else
                            handler.startRequest(HttpMethods.CACHE.lookup(tok0), tok1,
                                    HttpVersions.CACHE.lookup(buffer.sliceFromMark()));
                        eol = ch;
                        state = STATE_HEADER;
                        tok0.setPutIndex(tok0.getIndex());
                        tok1.setPutIndex(tok1.getIndex());
                        continuation=null;
                        return;
                    }
                    break;

                case STATE_HEADER:

                    if (ch == COLON || ch == SPACE || ch == TAB)
                    {
                        // header value without name - continuation?
                        length = -1;
                        state = STATE_HEADER_VALUE;
                    }
                    else
                    {
                        // handler last header if any
                        if (tok0.length()>0 || tok1.length()>0 || continuation!=null)
                        {
                            Buffer header = HttpHeaders.CACHE.lookup(tok0);
                            Buffer value = continuation==null?(Buffer)tok1:
                                (Buffer)new ByteArrayBuffer(continuation);
                            
                            int ho = HttpHeaders.CACHE.getOrdinal(header);
                            if (ho>=0) 
                            {
                                value=HttpHeaderValues.CACHE.lookup(value);
                                int vo = HttpHeaderValues.CACHE.getOrdinal(value);
                            
                                switch (ho)
                                {
                                    case HttpHeaders.CONTENT_LENGTH_ORDINAL:
                                        if (contentLength != CHUNKED_CONTENT)
                                        {
                                            contentLength = BufferUtil.toInt(value);
                                            if (contentLength <= 0) 
                                                contentLength = HttpParser.NO_CONTENT;
                                        }
                                    break;
                                    
                                    case HttpHeaders.CONNECTION_ORDINAL:
                                        // TODO comma list of connections !!!
                                        close = (HttpHeaderValues.CLOSE_ORDINAL == vo);
                                    break;
                                    
                                    case HttpHeaders.TRANSFER_ENCODING_ORDINAL:
                                        if (HttpHeaderValues.CHUNKED_ORDINAL == vo)
                                            contentLength = CHUNKED_CONTENT;
                                        else
                                        {
                                            // TODO avoid string conversion here
                                            String c = value.toString();
                                            if (c.endsWith(HttpHeaderValues.CHUNKED))
                                                contentLength = CHUNKED_CONTENT;
                                            else if (c.indexOf(HttpHeaderValues.CHUNKED) >= 0) 
                                                Portable.throwIO("BAD");
                                        }
                                    break;
                                    
                                    case HttpHeaders.CONTENT_TYPE_ORDINAL:
                                        content = true;
                                    break;
                                }
                            }

                            handler.parsedHeader(header,value);
                            tok0.setPutIndex(tok0.getIndex());
                            tok1.setPutIndex(tok1.getIndex());
                            continuation=null;
                        }

                        // now handle ch
                        if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                        {
                            // End of header

                            // work out the content demarcation
                            if (contentLength == UNKNOWN_CONTENT)
                            {
                                if (content || response)
                                    contentLength = EOF_CONTENT;
                                else
                                    contentLength = NO_CONTENT;
                            }
                            handler.headerComplete();
                            contentPosition = 0;
                            eol = ch;
                            switch (contentLength)
                            {
                                case HttpParser.EOF_CONTENT:
                                    state = STATE_EOF_CONTENT;
                                    break;
                                case HttpParser.CHUNKED_CONTENT:
                                    state = STATE_CHUNKED_CONTENT;
                                    break;
                                case HttpParser.NO_CONTENT:
                                    state = STATE_END;
                                    handler.messageComplete(contentPosition);
                                    break;
                                default:
                                    state = STATE_CONTENT;
                                    break;
                            }
                            return;
                        }
                        else
                        {
                            // New header
                            length = 1;
                            buffer.mark();
                            state = STATE_HEADER_NAME;
                        }
                    }
                    break;

                case STATE_HEADER_NAME:
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        if (length > 0) tok0.update(buffer.markIndex(), buffer.markIndex()+length);
                        eol = ch;
                        state = STATE_HEADER;
                    }
                    else if (ch == COLON)
                    {
                        if (length > 0) tok0.update(buffer.markIndex(), buffer.markIndex()+length);
                        length = -1;
                        state = STATE_HEADER_VALUE;
                    }
                    else if (ch != SPACE && ch != TAB)
                    {
                        if (length == -1) buffer.mark();
                        length = buffer.getIndex() - buffer.markIndex();
                    }
                    break;

                case STATE_HEADER_VALUE:
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        if (length > 0) 
                        {
                            if (tok1.length()==0)
                                tok1.update(buffer.markIndex(), buffer.markIndex()+length);
                            else
                            {
                                // Continuation line!
                                // TODO - deal with CR LF and COLON?
                                if (continuation==null)
                                    continuation=tok1.toString();
                                tok1.update(buffer.markIndex(), buffer.markIndex()+length);
                                continuation+=" "+tok1.toString();
                            }
                        }
                        eol = ch;
                        state = STATE_HEADER;
                    }
                    else if (ch != SPACE && ch != TAB)
                    {
                        if (length == -1) buffer.mark();
                        length = buffer.getIndex() - buffer.markIndex();
                    }
                    break;
            }
        } // end of HEADER states loop

        // Handle content
        Buffer chunk;
        while (state > STATE_END && buffer.length() > 0)
        {
            if (eol == CARRIAGE_RETURN && buffer.peek() == LINE_FEED)
            {
                eol = buffer.get();
                continue;
            }
            eol = 0;
            switch (state)
            {
                case STATE_EOF_CONTENT:
                    chunk = buffer.get(buffer.length());
                    handler.foundContent(contentPosition, chunk);
                    contentPosition += chunk.length();
                    return;

                case STATE_CONTENT:
                    {
                        int length = buffer.length();
                        int remaining = contentLength - contentPosition;
                        if (remaining == 0)
                        {
                            state = STATE_END;
                            handler.messageComplete(contentPosition);
                            return;
                        }
                        else if (length > remaining) length = remaining;
                        chunk = buffer.get(length);
                        handler.foundContent(contentPosition, chunk);
                        contentPosition += chunk.length();
                    }
                    return;

                case STATE_CHUNKED_CONTENT:
                    ch = buffer.peek();
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                        eol = buffer.get();
                    else if (ch <= SPACE)
                        buffer.get();
                    else
                    {
                        chunkLength = 0;
                        chunkPosition = 0;
                        state = STATE_CHUNK_SIZE;
                    }
                    break;

                case STATE_CHUNK_SIZE:
                    ch = buffer.get();
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        eol = ch;
                        if (chunkLength == 0)
                        {
                            state = STATE_END;
                            handler.messageComplete(contentPosition);
                            return;
                        }
                        else
                            state = STATE_CHUNK;
                    }
                    else if (ch <= SPACE || ch == SEMI_COLON)
                        state = STATE_CHUNK_PARAMS;
                    else if (ch >= '0' && ch <= '9')
                        chunkLength = chunkLength * 16 + (ch - '0');
                    else if (ch >= 'a' && ch <= 'f')
                        chunkLength = chunkLength * 16 + (10 + ch - 'a');
                    else if (ch >= 'A' && ch <= 'F')
                        chunkLength = chunkLength * 16 + (10 + ch - 'A');
                    else
                        Portable.throwRuntime("bad chunk char: " + ch);
                    break;

                case STATE_CHUNK_PARAMS:
                    ch = buffer.get();
                    if (ch == CARRIAGE_RETURN || ch == LINE_FEED)
                    {
                        eol = ch;
                        if (chunkLength == 0)
                        {
                            state = STATE_END;
                            handler.messageComplete(contentPosition);
                            return;
                        }
                        else
                            state = STATE_CHUNK;
                    }
                    break;

                case STATE_CHUNK:
                    {
                        int length = buffer.length();
                        int remaining = chunkLength - chunkPosition;
                        if (remaining == 0)
                        {
                            state = STATE_CHUNKED_CONTENT;
                            break;
                        }
                        else if (length > remaining) length = remaining;
                        chunk = buffer.get(length);
                        handler.foundContent(contentPosition, chunk);
                        contentPosition += chunk.length();
                        chunkPosition += chunk.length();
                    }
                    return;
            }
        }
    }

    /* ------------------------------------------------------------------------------- */
    public void reset()
    {
        state = STATE_START;
        contentLength = UNKNOWN_CONTENT;
        contentPosition = 0;
        length = 0;
        close = false;
        content = false;
        tok0.clear();
        tok1.clear();
        response = false;
    }

    public static abstract class Handler
    {

        /**
         * This is the method called by parser when the HTTP request line is parsed
         */
        public abstract void startRequest(Buffer method, Buffer url, Buffer version);

        /**
         * This is the method called by parser when the HTTP request line is parsed
         */
        public abstract void startResponse(Buffer version, int status, Buffer reason);


        /**
         * This is the method called by parser when a HTTP Header name and value is found
         */
        public void parsedHeader(Buffer name,Buffer value)
        {
        }

        public void headerComplete()
        {
        }

        public void foundContent(int index, Buffer ref)
        {
        }

        public void messageComplete(int contextLength)
        {
        }
    }
}
