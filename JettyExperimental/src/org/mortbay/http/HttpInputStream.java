package org.mortbay.http;

import java.io.IOException;
import java.io.InputStream;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.Portable;

/**
 *
 */
public class HttpInputStream extends InputStream
{
    private Buffer _buffer;
    private Parser _parser= new Parser();

    public HttpInputStream(Buffer buffer)
    {
        _buffer= buffer;
        _parser= new Parser();
    }
    
    public HttpHeader readHeader()
        throws IOException
    {
        if (_parser.inContentState())
            Portable.throwIllegalState("in context");
        while(_parser.inHeaderState())
            _parser.parseNext(_buffer);
        return _parser._header;
    }

    /*
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException
    {
        if (_parser.inContentState())
        {
            // Do we need to fill the buffer?
            if (_parser._content==null || !_parser._content.hasContent())
            {
                _parser._content=null;
                _parser.parseNext(_buffer);
            }
            
            if (_parser._content!=null && _parser._content.length()>=1)
                return _parser._content.get();
        } 
        return -1;
    }

    /* 
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (_parser.inContentState())
        {
            // Do we need to fill the buffer?
            if (_parser._content==null || !_parser._content.hasContent())
            {
                _parser._content=null;
                _parser.parseNext(_buffer);
            }
            
            if (_parser._content!=null)
                return _parser._content.get(b, off, len);
        }
        return -1;
    }

    /* 
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte[] b) throws IOException
    {
        if (_parser.inContentState())
        {
            // Do we need to fill the buffer?
            if (_parser._content==null || !_parser._content.hasContent())
            {
                _parser._content=null;
                _parser.parseNext(_buffer);
            }
            
            if (_parser._content!=null)
                return _parser._content.get(b, 0, b.length);
        }
        return -1;
    }

    /* 
     * @see java.io.InputStream#skip(long)
     */
    public long skip(long n) throws IOException
    {
        if (_parser.inContentState())
        {
            // Do we need to fill the buffer?
            if (_parser._content==null || !_parser._content.hasContent())
            {
                _parser._content=null;
                _parser.parseNext(_buffer);
            }
            
            if (_parser._content!=null)
                return _parser._content.skip((int)n);
        }
        return -1;
    }

    /* 
     * @see java.io.InputStream#available()
     */
    public int available() throws IOException
    {
        if (_parser.inContentState() && _parser._content!=null)
            return _parser._content.length();
        return -1;
    }

    /* 
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException
    {
        // Close of a HTTP stream must consume all the content.
        // TODO - may be able to do better here but not a really important case
        while (_parser.inContentState())
            _parser.parseNext(_buffer);
        _parser._content=null;
    }


    /**
     * 
     */
    private static class Parser extends HttpParser
    {
        boolean _request;
        HttpHeader _header;
        Buffer _headerName;
        Buffer _content;

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundField0(org.mortbay.io.Buffer)
         */
        public void foundField0(Buffer field)
        {
            _header.clear();
            // assume this is a request
            _request=true;
            Buffer method=HttpMethods.CACHE.get(field);
            if (method==null)
            {
                // maybe this is a response?
                Buffer version = HttpVersions.CACHE.lookup(field);
                if (version!=null)
                {
                    _request=false;
                    _header.setVersion(version);
                }
                else
                    method= HttpMethods.CACHE.lookup(field);
            }
            
            if (method!=null)
                _header.setMethod(method);
            _headerName=null;
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundField1(org.mortbay.io.Buffer)
         */
        public void foundField1(Buffer field)
        {
            if (_request)
                _header.setURI(field);
            else
                _header.setStatus(BufferUtil.toInt(field));
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundField2(org.mortbay.io.Buffer)
         */
        public void foundField2(Buffer field)
        {
            if (_request)
                _header.setVersion(HttpVersions.CACHE.lookup(field));
            else
                _header.setReason(field);
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundHttpHeader(org.mortbay.io.Buffer)
         */
        public void foundHttpHeader(Buffer header)
        {
            _headerName=header;
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundHttpValue(org.mortbay.io.Buffer)
         */
        public void foundHttpValue(Buffer value)
        {
            _header.add(_headerName,value);
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#headerComplete()
         */
        public void headerComplete()
        {
            // TODO Auto-generated method stub
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#foundContent(int, org.mortbay.io.Buffer)
         */
        public void foundContent(int index, Buffer content)
        {
            _content=content;
        }

        /* 
         * @see org.mortbay.http.HttpParser.Handler#messageComplete(int)
         */
        public void messageComplete(int contextLength)
        {
            // TODO Auto-generated method stub
        }

    }
}
