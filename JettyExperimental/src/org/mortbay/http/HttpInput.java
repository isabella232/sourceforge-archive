package org.mortbay.http;

import java.io.IOException;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.InBuffer;

/**
 * An input class that can process a HTTP stream, extracting
 * headers, dechunking content and handling persistent connections.
 * The class is non-blocking.
 */
public class HttpInput
{
    public final static int 
      EOF=-1,
      NOP=0,
      HEADER=1,
      CONTENT=2;
      
    public HttpInput(Buffer buffer)
    {
        _buffer= buffer;
        _parser= new Parser(_buffer);
    }

    public Buffer getBuffer()
    {
        return _buffer;
    }
    
    public Buffer getContent()
    {
        return _content;
    }
    
    public HttpHeader getHeader()
    {
        return _header;
    }

    public boolean inContentState()
    {
        return _parser.inContentState();
    }

    public boolean inHeaderState()
    {
        return _parser.inHeaderState();
    }

    public int parseNext()
        throws IOException
    {
        if (!_parser._headerComplete && 
            _parser._content==null && 
            !_parser._messageComplete)
            _parser.parseNext();
            
        if (_parser._headerComplete)
        {
            _parser._headerComplete=false;
            _header=_parser._header;
            return HEADER;
        }
        this._header=null;
        
        if (_parser._content!=null)
        {
            _content=_parser._content;
            _parser._content=null;
            return CONTENT;
        }
        this._content=null;
        
        if (_parser._messageComplete)
            return EOF;
        
        return NOP;
    }
    
    
    /* 
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException
    {
        // Either close real stream or consume all the content.
        if (_parser.getContentLength()==HttpParser.EOF_CONTENT &&
            _buffer instanceof InBuffer)
            ((InBuffer)_buffer).close();
        else
            while (_parser.inContentState())
                _parser.parseNext();

        _parser._content=null;
        _parser.setState(HttpParser.STATE_END);
    }
        
    public void reset()
    {
        _parser.reset();
        _parser.setState(HttpParser.STATE_START);
        _header=null;
        _content=null;
    }
        
    private Buffer _buffer;
    private HttpHeader _header;
    private Buffer _content;
    private Parser _parser;


    private class Parser extends HttpParser
    {
        private Parser(Buffer source)
        {
            super(source);
            _header=new HttpHeader();
        }
        
        public void foundContent(int index, Buffer content)
        {
            _content=content;
        }

        public void foundField0(Buffer field)
        {
            reset();
            
            // assume this is a request
            _request=true;
            Buffer method=HttpMethods.CACHE.get(field);
            if (method==null)
            {
                // maybe this is a response?
                Buffer version=HttpVersions.CACHE.lookup(field);
                if (version!=null)
                {
                    _request=false;
                    _header.setVersion(version);
                }
                else
                    method=HttpMethods.CACHE.lookup(field).asReadOnlyBuffer();
            }
            
            if (method!=null)
                _header.setMethod(method);
            _headerName=null;
        }

        public void foundField1(Buffer field)
        {
            if (_request)
                _header.setURI(field.asReadOnlyBuffer());
            else
                _header.setStatus(BufferUtil.toInt(field));
        }

        public void foundField2(Buffer field)
        {
            if (_request)
                _header.setVersion(HttpVersions.CACHE.lookup(field).asReadOnlyBuffer());
            else
                _header.setReason(field.asReadOnlyBuffer());
        }

        public void foundHttpHeader(Buffer header)
        {
            _headerName=header.asReadOnlyBuffer();
        }

        public void foundHttpValue(Buffer value)
        {
            _header.add(_headerName,value);
        }

        public void headerComplete()
        {
            _headerComplete=true;
        }

        public void messageComplete(int contextLength)
        {
            _messageComplete=true;
        }
        
        public void reset()
        {
            super.reset();
            _header.clear();
            _headerComplete=false;
            _messageComplete=false;
            _content=null;
        }
        
        HttpHeader _header;
        Buffer _content;
        Buffer _headerName;
        boolean _request;
        boolean _headerComplete;
        boolean _messageComplete;
    }
}
