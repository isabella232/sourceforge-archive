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

package org.mortbay.http.temp;

import java.io.IOException;

import org.mortbay.http.HttpMethods;
import org.mortbay.http.HttpParser;
import org.mortbay.http.HttpVersions;
import org.mortbay.http.HttpParser.EventHandler;
import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.EndPoint;

/**
 * An input class that can process a HTTP stream, extracting headers, dechunking content and
 * handling persistent connections. The class is non-blocking.
 */
public class HttpInput extends HttpParser.EventHandler
{
    public final static int EOF=-1,NOP=0,HEADER=1,CONTENT=2;
    private HttpParser _parser;
    
    private Buffer _buffer;
    private EndPoint _endp;
    
    private Buffer _content;
    private Buffer _parsedContent;

    private boolean _headerParsed;
    private HttpHeader _header;
    private HttpHeader _parsedHeader;
    
    private Buffer _headerName;
    private boolean _messageComplete;
    private boolean _request;

    public HttpInput(Buffer buffer,EndPoint io,HttpHeader header)
    {
        _buffer=buffer;
        _endp=io;
        _header=header;
        _parser=new HttpParser(_buffer,io,this);
    }

    /*
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException
    {
        // Either close real stream or consume all the content.
        if(_parser.getContentLength()==HttpParser.EOF_CONTENT&&_endp!=null)
            _endp.close();
        else
            while(_parser.inContentState())
                _parser.parseNext();
        _content=null;
        _parser.setState(HttpParser.STATE_END);
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * destroy.
     */
    public void destroy()
    {
        _buffer=null;
        _endp=null;
        _parsedContent=null;
        _parsedHeader=null;
        _content=null;
        _header=null;
        _headerName=null;
        _parser=null;
    }

    public Buffer getBuffer()
    {
        return _buffer;
    }

    public HttpHeader getHttpHeader()
    {
        return _header;
    }

    public Buffer getParsedContent()
    {
        return _parsedContent;
    }

    public HttpHeader getParsedHeader()
    {
        return _parsedHeader;
    }

    public boolean inContentState()
    {
        return _parser.inContentState();
    }

    public boolean inHeaderState()
    {
        return _parser.inHeaderState();
    }

    public int parseNext() throws IOException
    {
        try
        {
            if(!_headerParsed&&_content==null&&!_messageComplete)
                _parser.parseNext();
            if(_headerParsed)
            {
                _headerParsed=false;
                _parsedHeader=_header;
                return HEADER;
            }
            this._parsedHeader=null;
            if(_content!=null)
            {
                _parsedContent=_content;
                _content=null;
                return CONTENT;
            }
            this._parsedContent=null;
            if(_messageComplete)
                return EOF;
            return NOP;
        }
        catch(IOException e)
        {
            if(_parser.getState()==HttpParser.STATE_START)
                return EOF;
            throw e;
        }
    }

    public void reset()
    {
        _parser.reset();
        _header.clear();
        _headerParsed=false;
        _messageComplete=false;
        _content=null;
        _parser.setState(HttpParser.STATE_START);
        _parsedHeader=null;
        _parsedContent=null;
    }

    public void foundContent(int index, Buffer content)
    {
        _content = content;
    }


    public void parsedStartLine(Buffer tok0, Buffer tok1, Buffer tok2)
    {
        reset();

        // assume this is a request
        _request = true;
        Buffer method = HttpMethods.CACHE.get(tok0);
        if (method == null)
        {
            // maybe this is a response?
            Buffer version = HttpVersions.CACHE.lookup(tok0);
            if (version != null)
            {
                _request = false;
                _header.setVersion(version);
            }
            else
                method = HttpMethods.CACHE.lookup(tok0).asReadOnlyBuffer();
        }
        if (method != null) 
        _header.setMethod(method);
        _headerName = null;
     
        // tok1
        if (_request)
            // TODO Maybe a toString is better?
            _header.setURI(tok1.asReadOnlyBuffer());
        else
            _header.setStatus(BufferUtil.toInt(tok1));
   
        // tok2
        if (tok2!=null)
        {
            if (_request)
                _header.setVersion(HttpVersions.CACHE.lookup(tok2).asReadOnlyBuffer());
            else
                _header.setReason(tok2.asReadOnlyBuffer());
        }
    }

    public void parsedHeaderName(Buffer header)
    {
        _headerName = header.asReadOnlyBuffer();
    }

    public void parsedHeaderValue(Buffer value)
    {
        _header.add(_headerName, value);
    }

    public void headerComplete()
    {
        _headerParsed = true;
    }

    public void messageComplete(int contextLength)
    {
        _messageComplete = true;
    }
}
