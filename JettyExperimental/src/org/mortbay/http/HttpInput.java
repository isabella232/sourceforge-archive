package org.mortbay.http;

import java.io.IOException;
import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.InBuffer;

/**
 * An input class that can process a HTTP stream, extracting headers, dechunking content and
 * handling persistent connections. The class is non-blocking.
 */
public class HttpInput
{
    public final static int EOF=-1,NOP=0,HEADER=1,CONTENT=2;
    private Buffer _buffer;
    private Buffer _parsedContent;
    private HttpHeader _parsedHeader;
    private Parser _parser;

    public HttpInput(Buffer buffer,HttpHeader header)
    {
        _buffer=buffer;
        _parser=new Parser(_buffer,header);
    }

    /*
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException
    {
        // Either close real stream or consume all the content.
        if(_parser.getContentLength()==HttpParser.EOF_CONTENT&&_buffer instanceof InBuffer)
            ((InBuffer)_buffer).close();
        else
            while(_parser.inContentState())
                _parser.parseNext();
        _parser._content=null;
        _parser.setState(HttpParser.STATE_END);
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * destroy.
     */
    public void destroy()
    {
        _buffer=null;
        _parsedContent=null;
        _parsedHeader=null;
        if (_parser!=null)
            _parser.destroy();
        _parser=null;
    }

    public Buffer getBuffer()
    {
        return _buffer;
    }

    public HttpHeader getHttpHeader()
    {
        return _parser._header;
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
            if(!_parser._headerComplete&&_parser._content==null&&!_parser._messageComplete)
                _parser.parseNext();
            if(_parser._headerComplete)
            {
                _parser._headerComplete=false;
                _parsedHeader=_parser._header;
                return HEADER;
            }
            this._parsedHeader=null;
            if(_parser._content!=null)
            {
                _parsedContent=_parser._content;
                _parser._content=null;
                return CONTENT;
            }
            this._parsedContent=null;
            if(_parser._messageComplete)
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
        _parser.setState(HttpParser.STATE_START);
        _parsedHeader=null;
        _parsedContent=null;
    }



    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    private class Parser extends HttpParser
    {
        Buffer _content;
        HttpHeader _header;
        boolean _headerComplete;
        Buffer _headerName;
        boolean _messageComplete;
        boolean _request;

        private Parser(Buffer source,HttpHeader header)
        {
            super(source);
            _header=header;
        }

        /* ------------------------------------------------------------------------------- */
        /** destroy.
         * 
         */
        public void destroy()
        {
            _content=null;
            _header=null;
            _headerName=null;
        }

        public void foundContent(int index,Buffer content)
        {
            _content=content;
        }

        public void foundField0(Buffer field)
        {
            reset();

            // assume this is a request
            _request=true;
            Buffer method=HttpMethods.CACHE.get(field);
            if(method==null)
            {
                // maybe this is a response?
                Buffer version=HttpVersions.CACHE.lookup(field);
                if(version!=null)
                {
                    _request=false;
                    _header.setVersion(version);
                }
                else
                    method=HttpMethods.CACHE.lookup(field).asReadOnlyBuffer();
            }
            if(method!=null)
                _header.setMethod(method);
            _headerName=null;
        }

        public void foundField1(Buffer field)
        {
            if(_request)
                _header.setURI(field.asReadOnlyBuffer());
            else
                _header.setStatus(BufferUtil.toInt(field));
        }

        public void foundField2(Buffer field)
        {
            if(_request)
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
    }
}
