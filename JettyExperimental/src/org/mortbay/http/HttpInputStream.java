package org.mortbay.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.InBuffer;
import org.mortbay.io.Portable;
import org.mortbay.io.stream.InputBuffer;

/**
 * An input stream that can process a HTTP stream, extracting
 * headers, dechunking content and handling persistent connections.
 */
public class HttpInputStream extends InputStream
{

    public HttpInputStream(Buffer buffer)
    {
        _buffer= buffer;
        _parser= new Parser(_buffer);
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
                _parser.parseNext();
            }
            
            if (_parser._content!=null && _parser._content.length()>=1)
                return _parser._content.get();
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
                _parser.parseNext();
            }
            
            if (_parser._content!=null)
                return _parser._content.get(b, 0, b.length);
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
                _parser.parseNext();
            }
            
            if (_parser._content!=null)
                return _parser._content.get(b, off, len);
        }
        return -1;
    }
    
    public HttpHeader readHeader()
        throws IOException
    {
        if (_parser.inContentState())
            Portable.throwIllegalState("in context");
            
        _parser._header.clear();
        if (_parser.getState()==HttpParser.STATE_END)
            _parser.parseNext();
            
        if (_parser.inHeaderState())
        {
            while(_parser.inHeaderState())
                _parser.parseNext();
            return _parser._header;
        }
        return null;
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
                _parser.parseNext();
            }
            
            if (_parser._content!=null)
                return _parser._content.skip((int)n);
        }
        return -1;
    }

    public static void main(String[] args)
        throws Exception
    {   
        ServerSocket ss = new ServerSocket(8080);
        while(true)
        {
            Socket socket=ss.accept();
            InputBuffer in = new InputBuffer(socket.getInputStream(),2048);
            HttpInputStream input = new HttpInputStream(in);
            
            while (true) 
            {
                HttpHeader header = input.readHeader();
                if (header==null)
                    break;
                System.err.println(header);
                byte data[] = new byte[4096];
                
                int len;
                while((len=input.read(data,0,4096))>0)
                {
                    System.err.println(len+" of content");
                }
                socket.getOutputStream().write("HTTP/1.1 200 OK\015\012Transfer-Encoding: chunked\015\012Content-Type: text/html\015\012\015\0120b\015\012<h1>Hi</h1>\015\0120\015\012\015\012".getBytes());        
            } 
        }
    }   
    
    private Buffer _buffer;
    private Parser _parser;

    private static class Parser extends HttpParser
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

        public void foundField1(Buffer field)
        {
            if (_request)
                _header.setURI(field);
            else
                _header.setStatus(BufferUtil.toInt(field));
        }

        public void foundField2(Buffer field)
        {
            if (_request)
                _header.setVersion(HttpVersions.CACHE.lookup(field));
            else
                _header.setReason(field);
        }

        public void foundHttpHeader(Buffer header)
        {
            _headerName=header;
        }

        public void foundHttpValue(Buffer value)
        {
            _header.add(_headerName,value);
        }

        public void headerComplete()
        {
        }

        public void messageComplete(int contextLength)
        {
        }
        
        Buffer _content;
        HttpHeader _header;
        Buffer _headerName;
        boolean _request;
    }
}
