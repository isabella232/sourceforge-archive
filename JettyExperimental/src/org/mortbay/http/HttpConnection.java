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
import java.io.InterruptedIOException;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

import org.apache.ugli.LoggerFactory;
import org.apache.ugli.ULogger;
import org.mortbay.http.handler.FileHandler;
import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.EndPoint;
import org.mortbay.io.Portable;
import org.mortbay.util.URI;

/**
 * @author gregw
 * 
 * To change the template for this generated type comment go to Window - Preferences - Java - Code
 * Generation - Code and Comments
 */
public class HttpConnection 
{
    private static ULogger log = LoggerFactory.getLogger(HttpConnection.class);
    private static int UNKNOWN = -2;

    private HttpConnector _connector;
    private EndPoint _endp;
    private HttpHandler _handler;
    
    private HttpParser _parser;
    private HttpFields _requestFields;
    private HttpRequest _request;
    private Input _in;
    
    private HttpBuilder _builder;
    private HttpFields _responseFields;
    private HttpResponse _response;
    private Output _out;
    
    private transient Buffer _content;
    private transient int _connection = UNKNOWN;
    private transient int _expect = UNKNOWN;
    private transient int _version = UNKNOWN;
    private transient boolean _head = false;
    private transient boolean _host = false;
    

    /**
     * @param handler TODO
     *  
     */
    public HttpConnection(HttpConnector connector, EndPoint endpoint, HttpHandler handler)
    {
        _connector = connector;
        _endp = endpoint;
        _parser = new HttpParser(_connector, endpoint, new Handler());
        _requestFields=new HttpFields();
        _responseFields=new HttpFields();
        _request=new HttpRequest(this);
        _response=new HttpResponse(this);
        _builder = new HttpBuilder(_connector,_endp);
        
        
        if (_handler==null)
            _handler=new FileHandler();
    }

    /*
     * @see org.mortbay.http.HttpParser.Handler#foundContent(int, org.mortbay.io.Buffer)
     */
    public void content(int index, Buffer ref)
    {
        _content=ref;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the requestFields.
     */
    public HttpFields getRequestFields()
    {
        return _requestFields;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the responseFields.
     */
    public HttpFields getResponseFields()
    {
        return _responseFields;
    }

    /* ------------------------------------------------------------ */
    public void handle() throws IOException
    {
        try
        {
            // If we are not ended then parse available
            if (!_parser.isState(HttpParser.STATE_END) && (_content==null || _content.length()==0)) 
                _parser.parseAvailable();

            // Do we have more writting to do?
            if (_builder.isState(HttpBuilder.STATE_FLUSHING) || _builder.isState(HttpBuilder.STATE_CONTENT))
                _builder.flushBuffers();
        }
        finally
        {
            // TODO - maybe do this at start of handle aswell or instead?
            if (_parser.isState(HttpParser.STATE_END) && _builder.isState(HttpBuilder.STATE_END))
            {
                _parser.reset();  // TODO return header buffer???
                _requestFields.clear();
                _request.recycle();
                
                _builder.reset(!_builder.isPersistent());  // TODO true or false?
                _responseFields.clear();
                _response.recycle();
            }
        }
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
    

    /* ------------------------------------------------------------ */
    public boolean isResponseCommitted()
    {
        return _builder.isCommitted();
    }

    /* ------------------------------------------------------------ */
    public void commitResponse(boolean last)
     throws IOException	
    {
        if (!_builder.isCommitted())
        {
            _builder.setResponse(_response.getStatus(), _response.getReason());
            _builder.completeHeader(_responseFields, last);
        }
        if (last)
            _builder.complete();
    }
    
    /* ------------------------------------------------------------ */
    public void completeResponse()
    	throws IOException	
    {
        if (!_builder.isCommitted())
        {
            _builder.setResponse(_response.getStatus(), _response.getReason());
            _builder.completeHeader(_responseFields, HttpBuilder.LAST);
        }
        
        if (!_builder.isComplete())
        {
            _builder.complete();
        }
    }

    /* ------------------------------------------------------------ */
    public void flushResponse()
     throws IOException	
    {
        commitResponse(HttpBuilder.MORE);
        _builder.flushBuffers();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public ServletInputStream getInputStream()
    {
        if (_in==null)
            _in=new Input();
        return _in;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public ServletOutputStream getOutputStream()
    {
        if (_out==null)
            _out=new Output();
        return _out;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Handler extends HttpParser.Handler
    {

        /*
         * @see org.mortbay.http.HttpParser.Handler#headerComplete()
         */
        public void headerComplete() throws IOException
        {
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
                            _builder.sendError(417,null,null,true);
                            return;
                        }
                    }
                    break;
                default:
            }
            
            if (_handler!=null)
            {
                try
                {
                    _handler.handle(_request, _response);
                }
                catch (ServletException e)
                {
                    // TODO Auto-generated catch block
                    log.warn("handling",e);
                    _builder.sendError(500,null,null,true);
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.mortbay.http.HttpParser.Handler#messageComplete(int)
         */
        public void messageComplete(int contextLength) throws IOException
        {
            if (!_builder.isComplete())
            {
                _builder.completeHeader(_responseFields, HttpBuilder.LAST);
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
                    _responseFields.put(HttpHeaders.CONNECTION_BUFFER,value);
                	// TODO something with this???
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
            _request.setUri(new URI(uri.toString())); // TODO more efficient???
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
        
    }
    
    private class Input extends ServletInputStream
    {
        /* ------------------------------------------------------------ */
        /* 
         * @see java.io.InputStream#read()
         */
        public int read() throws IOException
        {
            while (_content==null || _content.length()==0)
            {
                if (_parser.isState(HttpParser.STATE_END))
                    return -1;
                
                // Try to get more content
                _parser.parseNext();
                
                // If unsuccessful and if we are we are not blocking then block
                if (!_parser.isState(HttpParser.STATE_END) && (_content==null || _content.length()==0) && (_endp!=null && !_endp.isBlocking()))
                {
                    _endp.blockWritable(60000); // TODO Configure from connector
                    _parser.parseNext();
                }
                
                // if still no content - this is a timeout
                if (!_parser.isState(HttpParser.STATE_END) && (_content==null || _content.length()==0))
                    throw new InterruptedIOException("timeout");
            }
            
            return _content.get();
        }
    }
    
    public class Output extends ServletOutputStream
    {
        ByteArrayBuffer _buf1=null;
        ByteArrayBuffer _bufn=null;
        
        /* ------------------------------------------------------------ */
        /* 
         * @see java.io.OutputStream#close()
         */
        public void close() throws IOException
        {
            commitResponse(HttpBuilder.LAST);
        }
        /* ------------------------------------------------------------ */
        /* 
         * @see java.io.OutputStream#flush()
         */
        public void flush() throws IOException
        {
            flushResponse();
        }
        
        /* ------------------------------------------------------------ */
        /* 
         * @see java.io.OutputStream#write(byte[], int, int)
         */
        public void write(byte[] b, int off, int len) throws IOException
        {
            if(_bufn==null)
                _bufn=new ByteArrayBuffer(b,off,len);
            else
                _bufn.wrap(b,off,len);
            write(_bufn);
        }
        
        /* ------------------------------------------------------------ */
        /* 
         * @see java.io.OutputStream#write(byte[])
         */
        public void write(byte[] b) throws IOException
        {
            if(_bufn==null)
                _bufn=new ByteArrayBuffer(b);
            else
                _bufn.wrap(b);
            write(_bufn);
        }
        
        /* ------------------------------------------------------------ */
        /* 
         * @see java.io.OutputStream#write(int)
         */
        public void write(int b) throws IOException
        {
            if (_buf1==null)
                _buf1=new ByteArrayBuffer(1);
            else
                _buf1.compact();
            _buf1.put((byte)b);
            write(_buf1);
        }
        
        private void write(Buffer buffer)
        	throws IOException
        {
            // Block until we can add content.
            while (_builder.isBufferFull() && !_endp.isClosed() && !_endp.isBlocking())
            {
                _endp.blockWritable(60000);  // TODO Configure timeout
                _builder.flushBuffers();
            }
            
            // Add the content
            _builder.addContent(buffer,HttpBuilder.MORE);
            
            //  Have to flush and complete headers?
            if (_builder.isBufferFull())
            {
                // Buffers are full so flush.
                commitResponse(HttpBuilder.MORE);
                _builder.flushBuffers();
            }
            
            // Block until our buffer is free
            while (buffer.length()>0 && !_endp.isClosed() && !_endp.isBlocking())
            {
                _endp.blockWritable(60000);  // TODO Configure timeout
                _builder.flushBuffers();
            }
        }
        
        public void sendContent(Object content)
        	throws IOException
        {
            if (_builder.getContentAdded()>0)
                Portable.throwIllegalState("!empty");
            
            if (content instanceof Buffer)
            {
                _builder.addContent((Buffer)content,HttpBuilder.LAST);
                commitResponse(HttpBuilder.LAST);
            }
            else
                Portable.throwIllegalArgument("type?");
        }
    
    }

}
