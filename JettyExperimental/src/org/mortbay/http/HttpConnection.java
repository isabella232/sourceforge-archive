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

import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;

import org.apache.ugli.ULogger;
import org.apache.ugli.LoggerFactory;
import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.Portable;
import org.mortbay.io.View;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.util.URI;

/**
 * @author gregw
 * 
 * To change the template for this generated type comment go to Window - Preferences - Java - Code
 * Generation - Code and Comments
 */
public class HttpConnection extends HttpParser.Handler
{
    private static ULogger log = LoggerFactory.getLogger(HttpConnection.class);
    private static int UNKNOWN = -2;
    HttpBuilder _builder;
    
    // TODO hack!
    HashMap _cache = new HashMap();
    transient int _connection = UNKNOWN;
    HttpConnector _connector;

    EndPoint _endp;

    private EndPoint _endpoint;
    transient int _expect = UNKNOWN;
    transient boolean _head = false;
    
    transient Buffer _headerBuffer;
    transient boolean _host = false;
    transient Buffer _inBuffer;
    HttpParser _parser;
    HttpRequest _request;
    HttpResponse _response;
    HttpFields _requestFields;
    HttpFields _responseFields;

    transient String _url;
    transient int _version = UNKNOWN;
    

    /**
     *  
     */
    public HttpConnection(HttpConnector connector, EndPoint endpoint)
    {
        _connector = connector;
        _endp = endpoint;
        _endpoint = endpoint;
        _parser = new HttpParser(null, endpoint, this);
        _requestFields=new HttpFields();
        _responseFields=new HttpFields();
        _request=new HttpRequest(this);
        _builder = new HttpBuilder(_connector,_endp);
    }

    /*
     * @see org.mortbay.http.HttpParser.Handler#foundContent(int, org.mortbay.io.Buffer)
     */
    public void content(int index, Buffer ref)
    {
        // TODO make available to input stream OR other content sink OR maybe buffer it???
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the requestFields.
     */
    HttpFields getRequestFields()
    {
        return _requestFields;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the responseFields.
     */
    HttpFields getResponseFields()
    {
        return _responseFields;
    }

    /* ------------------------------------------------------------ */
    public void handle() throws IOException
    {
        try
        {
            // check read buffer buffers
            if (_parser.isState(HttpParser.STATE_START))
            {
                // start with a just a header buffer
                if (_parser.getBuffer() == null) 
                {
                    // TODO - move this buffer management stuff into HttpParser
                    
                    // get a new header buffer.
                    if (_headerBuffer==null)
                        _headerBuffer=_connector.getBuffer(false);
                    else
                        _headerBuffer.clear();
                    _parser.setBuffer(_headerBuffer);
                }
                else if (_parser.getBuffer()==_inBuffer)
                {
                    // reuse last header buffer
                    if (_inBuffer!=null)	
                    {
                        _connector.returnBuffer(_inBuffer);
                        _inBuffer=null;
                    }
                    _parser.setBuffer(_headerBuffer);
                    _headerBuffer.clear();
                }
                else
                    _headerBuffer.clear();
                    
            }

            // If we are not ended then parse available
            if (!_parser.isState(HttpParser.STATE_END)) 
                _parser.parseAvailable();

            // Do we have more writting to do?
            if (_builder.isState(HttpBuilder.STATE_FLUSHING) || _builder.isState(HttpBuilder.STATE_CONTENT))
                _builder.flushBuffers();
            
        }
        finally
        {
            // If we are non block
            if (!_endp.isBlocking())
            {
                // return input buffers while request ended.
                if (_parser.isState(HttpParser.STATE_END) && _parser.getBuffer() != null)
                {
                    if (_inBuffer!=null)
                    {
                        _connector.returnBuffer(_inBuffer);
                        _inBuffer=null;
                        _parser.setBuffer(null);
                    }
                    
                    // return header and output buffer while response ended
                    // TODO FIX THIS
                    /*
                    if (_builder.isState(HttpBuilder.STATE_END) && _builder.getSmallBuffer() != null)
                    {
                        if (_headerBuffer!=null)
                        {
                            _connector.returnBuffer(_headerBuffer);
                            _headerBuffer=null;
                            _parser.setBuffer(null);
                        }
                        if (_outBuffer!=null)
                        {
                            _connector.returnBuffer(_outBuffer);
                            _outBuffer=null;
                            _builder.setSmallBuffer(null);
                        }
                    }
                    */
                }
            }

            if (_parser.isState(HttpParser.STATE_END) && _builder.isState(HttpBuilder.STATE_END))
            {
                _parser.reset();
                _builder.reset(!_builder.isPersistent());  // TODO true or false?
            }
        }
    }

    /*
     * @see org.mortbay.http.HttpParser.Handler#headerComplete()
     */
    public void headerComplete() throws IOException
    {
        if (_parser.getState() == HttpParser.STATE_END)
        {
            _parser.setBuffer(null);
        }
        else
        {
            // Request content, so consider giving it a bigger buffer.
            // TODO may not need to do this swap for small content.
            if (_inBuffer==null)
                _inBuffer=_connector.getBuffer(false);
            _inBuffer.put(_headerBuffer);
            _parser.setBuffer(_inBuffer);
        }

        _builder.setVersion(_version);
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
                    _builder.setResponse(400, null);
                    _responseFields.put(HttpHeaders.CONNECTION_BUFFER, HttpHeaderValues.CLOSE_BUFFER);
                    _builder.complete();
                    return;
                }

                if (_expect != UNKNOWN)
                {
                    if (_expect == HttpHeaderValues.CONTINUE_ORDINAL)
                    {
                        _builder.setResponse(100, null);
                        _builder.complete();
                        _builder.reset(false);
                    }
                    else
                    {
                        _builder.setResponse(417, null);
                        _responseFields.put(HttpHeaders.CONNECTION_BUFFER,HttpHeaderValues.CLOSE_BUFFER);
                        _builder.complete();
                        return;
                    }
                }
                break;
            default:
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mortbay.http.HttpParser.Handler#messageComplete(int)
     */
    public void messageComplete(int contextLength) throws IOException
    {
        _parser.setBuffer(null);
        if (_builder.isState(HttpBuilder.STATE_HEADER))
        {
            _builder.setResponse(200, null);

            if (_connection >= 0)
                    _responseFields.put(HttpHeaders.CONNECTION_BUFFER, HttpHeaderValues.CACHE.get(_connection));

            _responseFields.put(HttpHeaders.CONTENT_TYPE_BUFFER, HttpHeaderValues.TEXT_HTML_BUFFER);

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
            _builder.completeHeader(_responseFields,HttpBuilder.LAST);
            _builder.complete();
        }

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
                // TODO comma list of connections ???
                _connection = HttpHeaderValues.CACHE.getOrdinal(value);
        }
        
        _requestFields.add(name, value);
    }
    
    
    /*
     * 
     * @see org.mortbay.http.HttpParser.Handler#startRequest(org.mortbay.io.Buffer,
     *      org.mortbay.io.Buffer, org.mortbay.io.Buffer)
     */
    public void startRequest(Buffer method, Buffer uri, Buffer version)
    {
        _request.setMethod(method.toString());
        _url=uri.toString();
        _request.setUri(new URI(_url));
        _version = version == null ? HttpVersions.HTTP_0_9_ORDINAL : HttpVersions.CACHE
                .getOrdinal(version);
        if (_version <= 0) _version = HttpVersions.HTTP_1_0_ORDINAL;
        _request.setProtocol(HttpVersions.CACHE.get(_version).toString());
        
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
        Portable.throwIllegalState("response");
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public boolean isConfidential()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public EndPoint getEndPoint()
    {
        return _endp;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public boolean useDNS()
    {
        // TODO Auto-generated method stub
        return false;
    }
    
    
    private class Input extends ServletInputStream
    {
        /* ------------------------------------------------------------ */
        /* 
         * @see java.io.InputStream#read()
         */
        public int read() throws IOException
        {
            // TODO Auto-generated method stub
            return 0;
        }
    
    }
}
