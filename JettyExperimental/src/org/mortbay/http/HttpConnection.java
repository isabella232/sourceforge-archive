//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.http;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.View;
import org.mortbay.io.nio.NIOBuffer;

/**
 * @author gregw
 * 
 * To change the template for this generated type comment go to Window - Preferences - Java - Code
 * Generation - Code and Comments
 */
public class HttpConnection extends HttpParser.Handler
{
    private static Log log = LogFactory.getLog(HttpConnection.class);
    private static int UNKNOWN = -2;

    private EndPoint _endpoint;

    EndPoint _endp;
    HttpListener _listener;
    HttpParser _parser;
    HttpBuilder _builder;

    transient String _url;
    transient int _version = UNKNOWN;
    transient boolean _head = false;
    transient boolean _host = false;
    transient int _expect = UNKNOWN;
    transient int _connection = UNKNOWN;
    
    // TODO hack!
    HashMap _cache = new HashMap();
    

    /**
     *  
     */
    public HttpConnection(HttpListener listener, EndPoint endpoint)
    {
        _listener = listener;
        _endp = endpoint;
        _endpoint = endpoint;
        _parser = new HttpParser(null, endpoint, this);
        _builder = new HttpBuilder(null, endpoint);
    }

    public void handle() throws IOException
    {
        try
        {
            // check read buffer buffers
            if (_parser.isState(HttpParser.STATE_START))
            {
                if (_parser.getBuffer() == null) _parser.setBuffer(_listener.getBuffer());
            }

            if (!_parser.isState(HttpParser.STATE_END)) _parser.parseAvailable();

            // Do we have more writting to do?
            if (_builder.isState(HttpBuilder.STATE_FLUSHING) || _builder.isState(HttpBuilder.STATE_CONTENT))
                _builder.flushBuffers();
            
        }
        finally
        {
            if (!_endp.isBlocking())
            {
                if (_parser.isState(HttpParser.STATE_END) && _parser.getBuffer() != null)
                {
                    _listener.returnBuffer(_parser.getBuffer());
                    _parser.setBuffer(null);
                }
                
                if (_builder.isState(HttpBuilder.STATE_END) && _builder.getBuffer() != null)
                {
                    _listener.returnBuffer(_builder.getBuffer());
                    _builder.setBuffer(null);
                }
            }

            if (_parser.isState(HttpParser.STATE_END) && _builder.isState(HttpBuilder.STATE_END))
            {
                _parser.reset();
                _builder.reset();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mortbay.http.HttpParser.Handler#startRequest(org.mortbay.io.Buffer,
     *      org.mortbay.io.Buffer, org.mortbay.io.Buffer)
     */
    public void startRequest(Buffer method, Buffer url, Buffer version)
    {
        _version = version == null ? HttpVersions.HTTP_0_9_ORDINAL : HttpVersions.CACHE
                .getOrdinal(version);
        if (_version <= 0) _version = HttpVersions.HTTP_1_0_ORDINAL;
        _url=url.toString();
        _host = false;
        _expect = UNKNOWN;
        _connection = UNKNOWN;
        _head = HttpVersions.CACHE.getOrdinal(method) == HttpMethods.HEAD_ORDINAL;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mortbay.http.HttpParser.Handler#startResponse(org.mortbay.io.Buffer, int,
     *      org.mortbay.io.Buffer)
     */
    public void startResponse(Buffer version, int status, Buffer reason)
    {
        _host = false;
    }

    /*
     * @see org.mortbay.http.HttpParser.Handler#parsedHeaderValue(org.mortbay.io.Buffer)
     */
    public void parsedHeader(Buffer name, Buffer value)
    {
        int ho = HttpHeaders.CACHE.getOrdinal(name);
        switch (ho)
        {
            case HttpHeaders.HOST_ORDINAL:
                _host = true;
                break;

            case HttpHeaders.EXPECT_ORDINAL:
                _expect = HttpHeaderValues.CACHE.getOrdinal(value);

            case HttpHeaders.CONNECTION_ORDINAL:
                // TODO comma list of connections !!!
                _connection = HttpHeaderValues.CACHE.getOrdinal(value);
        }
    }

    /*
     * @see org.mortbay.http.HttpParser.Handler#headerComplete()
     */
    public void headerComplete() throws IOException
    {
        if (_parser.getState() == HttpParser.STATE_END)
        {
            // Reuse input buffer
            _builder.setBuffer(_parser.getBuffer());
            _parser.setBuffer(null);
        }
        else
        {
            // get builder buffer from pool
            _builder.setBuffer(_listener.getBuffer());
        }

        switch (_version)
        {
            case HttpVersions.HTTP_0_9_ORDINAL:
                break;
            case HttpVersions.HTTP_1_0_ORDINAL:
                _builder.setHead(_head);
                break;
            case HttpVersions.HTTP_1_1_ORDINAL:
                _builder.setHead(_head);
                if (!_host)
                {
                    // TODO prebuilt response
                    _builder.buildResponse(_version, 400, null);
                    _builder.addHeader(HttpHeaders.CONNECTION_BUFFER, HttpHeaderValues.CLOSE_BUFFER);
                    _builder.complete();
                    return;
                }

                if (_expect != UNKNOWN)
                {
                    if (_expect == HttpHeaderValues.CONTINUE_ORDINAL)
                    {
                        _builder.buildResponse(_version, 100, null);
                        _builder.complete();
                        _builder.reset();
                    }
                    else
                    {
                        _builder.buildResponse(_version, 417, null);
                        _builder.addHeader(HttpHeaders.CONNECTION_BUFFER,
                                HttpHeaderValues.CLOSE_BUFFER);
                        _builder.complete();
                        return;
                    }
                }
                break;
            default:
        }

    }

    /*
     * @see org.mortbay.http.HttpParser.Handler#foundContent(int, org.mortbay.io.Buffer)
     */
    public void content(int index, Buffer ref)
    {
        // TODO
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mortbay.http.HttpParser.Handler#messageComplete(int)
     */
    public void messageComplete(int contextLength) throws IOException
    {
        _parser.setBuffer(null);
        if (_builder.isState(HttpBuilder.STATE_START))
        {
            _builder.buildResponse(_version, 200, null);

            if (_connection >= 0)
                    _builder.addHeader(HttpHeaders.CONNECTION_BUFFER, HttpHeaderValues.CACHE
                            .get(_connection));

            _builder.addHeader(HttpHeaders.CONTENT_TYPE_BUFFER, HttpHeaderValues.TEXT_HTML_BUFFER);

            
            Buffer content = (Buffer) _cache.get(_url);
            
            if (content==null)
            {
                File file = new File(".",_url);
                if (file.exists() && !file.isDirectory())
                {
                    content = new NIOBuffer(file);
                    ByteBuffer bbuf = ((NIOBuffer)content).getByteBuffer();
                    _cache.put(_url, content);
                }
            }
            
            if (content==null)
                content = new ByteArrayBuffer("<h1>Hello World: "+_url+"</h1>\n" + "" + "");
            
            _builder.addContent(new View(content), true);
            _builder.complete();
        }

    }

}
