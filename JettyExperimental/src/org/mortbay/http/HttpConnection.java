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

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.EndPoint;
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
    private static int UNKNOWN=-2;
    
    private EndPoint _endpoint;

    // TODO recycle buffers
    NIOBuffer buffer = new NIOBuffer(4096, true);
    HttpParser _parser;
    HttpBuilder _builder;

    transient int _version=UNKNOWN;
    transient boolean _head = false;
    transient boolean _host = false;
    transient int _expect = UNKNOWN;
    transient int _connection = UNKNOWN;

    /**
     *  
     */
    public HttpConnection(EndPoint endpoint)
    {
        _endpoint = endpoint;
        _parser = new HttpParser(buffer, endpoint, this);
        _builder = new HttpBuilder(buffer, endpoint);
    }

    public void handle() throws IOException
    {
        _parser.parseAvailable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mortbay.http.HttpParser.Handler#startRequest(org.mortbay.io.Buffer,
     *      org.mortbay.io.Buffer, org.mortbay.io.Buffer)
     */
    public void startRequest(Buffer method, Buffer url, Buffer version)
    {
        System.err.println(method + " " + url + " " + version);

        _version = version==null 
        	? HttpVersions.HTTP_0_9_ORDINAL:HttpVersions.CACHE.getOrdinal(version);
        if (_version <= 0) _version = HttpVersions.HTTP_1_0_ORDINAL;
        _host = false;
        _expect=UNKNOWN;
        _connection=UNKNOWN;
        _head = HttpVersions.CACHE.getOrdinal(method)==HttpMethods.HEAD_ORDINAL;
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
        System.out.println(name + ": " + value);

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
    public void headerComplete()
    {
        System.out.println();
    }

    /*
     * @see org.mortbay.http.HttpParser.Handler#foundContent(int, org.mortbay.io.Buffer)
     */
    public void foundContent(int index, Buffer ref)
    {
        // TODO Auto-generated method stub
        super.foundContent(index, ref);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mortbay.http.HttpParser.Handler#messageComplete(int)
     */
    public void messageComplete(int contextLength)
    {
        System.err.println("message complete");
        buffer.clear();
        _builder.reset();

        try
        {
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
                            _builder.complete();
                            return;
                        }
                    }
                    break;
                default:
            }

            _builder.buildResponse(_version, 200, null);
            
            if (_connection>=0)
                _builder.header(HttpHeaders.CONNECTION_BUFFER,
                        HttpHeaderValues.CACHE.get(_connection));
            
            _builder.header(HttpHeaders.CONTENT_TYPE_BUFFER, HttpHeaderValues.TEXT_HTML_BUFFER);
            
            ByteArrayBuffer content = new ByteArrayBuffer("<h1>Hello World</h1>\n" + "" + "");
            NIOBuffer ncontent = new NIOBuffer(4096,true);
            ncontent.put(content);
            
            _builder.content(ncontent, true);
            
            _builder.complete();
            
            System.err.println(_builder);
        }
        catch (IOException e)
        {
            log.warn("???", e);
        }
        
        buffer.clear();
        _parser.reset();
    }

}
