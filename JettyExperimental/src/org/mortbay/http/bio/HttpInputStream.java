package org.mortbay.http.bio;

import java.io.IOException;
import java.io.InputStream;

import org.mortbay.http.HttpHeader;
import org.mortbay.http.HttpInput;
import org.mortbay.io.Buffer;
import org.mortbay.io.Portable;

/**
 * An input stream that can process a HTTP stream, extracting
 * headers, dechunking content and handling persistent connections.
 */
public class HttpInputStream extends InputStream
{

    public HttpInputStream(Buffer buffer, HttpHeader header)
    {
        _in = new HttpInput(buffer,header);
    }

    public HttpInput getHttpInput()
    {
        return _in;
    }

    public Buffer getBuffer()
    {
        return _in.getBuffer();
    }
    
    public HttpHeader getHttpHeader()
    {
        return _in.getHttpHeader();
    }
    
    /* 
     * @see java.io.InputStream#available()
     */
    public int available() throws IOException
    {
        if (_content!=null)
            return _content.length();
        return -1;
    }

    /* 
     * @see java.io.InputStream#close()
     */
    public void close() throws IOException
    {
        _content=null;
        _in.close();
    }

    /*
     * @see java.io.InputStream#read()
     */
    public int read() throws IOException
    {
        if (!getContent())
            return -1;
           
        return _content.get();
    }

    /* 
     * @see java.io.InputStream#read(byte[])
     */
    public int read(byte[] b) throws IOException
    {
        if (!getContent())
            return -1;
           
        return _content.get(b,0,b.length);
    }

    /* 
     * @see java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte[] b, int off, int len) throws IOException
    {
        if (!getContent())
            return -1;
           
        return _content.get(b,off,len);
        
    }

    /* 
     * @see java.io.InputStream#skip(long)
     */
    public long skip(long n) throws IOException
    {
        if (!getContent())
            return -1;
           
        return _content.skip((int)n);
    }
    
    public HttpHeader readHeader()
        throws IOException
    {   
        while (_in.getParsedHeader()==null)
        {
            switch(_in.parseNext())
            {
                case HttpInput.NOP: 
                    continue;
                case HttpInput.HEADER:
                    break;
                case HttpInput.EOF:
                    return null;
                case HttpInput.CONTENT:
                    Portable.throwIllegalState("In Content");
            }
            break;
        }
            
        return _in.getParsedHeader();
    }
    
    public void resetStream()
    {
        _in.reset();
        _content=null;
    }
    
    private boolean getContent()
        throws IOException
    {
        if (_content!=null && _content.length()>0)
            return true;
            
        while(true)
        {
            switch(_in.parseNext())
            {
                case HttpInput.NOP: 
                    continue;
                case HttpInput.HEADER:
                case HttpInput.EOF:
                    return false;
                case HttpInput.CONTENT:
                    _content=_in.getParsedContent();
                    if (_content!=null && _content.length()==0)
                        continue;
            }
            break;
        }
        return _content!=null;
    }
    
    
    Buffer _content;
    private HttpInput _in;
    
    /* ------------------------------------------------------------------------------- */
    /** destroy.
     * 
     */
    public void destroy()
    {
        _content=null;
        if (_in!=null)
            _in.destroy();
        _in=null;
    }
    

}
