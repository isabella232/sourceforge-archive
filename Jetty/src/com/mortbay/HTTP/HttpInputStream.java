// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Util.*;

import java.io.*;

/** HTTP input stream
 * @version $Id$
 * @author Greg Wilkins
*/
public class HttpInputStream extends FilterInputStream
{
    /* ------------------------------------------------------------ */
    /** Limit max line length */
    public static int __maxLineLength=8192;
    
    /* ------------------------------------------------------------ */
    final static byte[]
        __CRLF_B      ={(byte)'\015',(byte)'\012'};
    
    /* ------------------------------------------------------------ */
    private int _chunksize=0;
    private HttpFields _footers=null;
    private boolean _chunking=false;
    private int _contentLength=-1;

    
    /* ------------------------------------------------------------ */
    /** Buffer for readLine.
     * The buffer gets reused across calls.
     * Note that the readCharBufferLine method breaks encapsulation
     * by returning this buffer, so BE CAREFUL!!!
     */
    static class CharBuffer
    {
        private int _size=0;
        char[] _chars = new char[128];

        void add(char c)         { _chars[_size++]=c; }
        void reset()             { _size=0; }
        int size()               { return _size; }
        int capacity()           { return _chars.length-_size; }
        public String toString() { return new String(_chars,0,_size); }
        void expand()
        {
            // Double the size of the buffer but don't
            // wastefully overshoot any contentLength limit.
            int length = Math.min(__maxLineLength,_chars.length<<1);
            char[] old = _chars;
            _chars =new char[length];
            System.arraycopy(old,0,_chars,0,_size);
        }
    };
    
    CharBuffer _charBuffer = new CharBuffer();

    
    /* ------------------------------------------------------------ */
    /** Constructor
     */
    public HttpInputStream( InputStream in)
    {
        super(new BufferedInputStream(in));
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param on 
     */
    public void setChunking(boolean on)
    {
        if (on && _contentLength>=0)
            throw new IllegalStateException("Can't chunk and have content length");
        
        _chunking=on;
    }

    /* ------------------------------------------------------------ */
    /** Set the content length.
     * Only this number of bytes can be read before EOF is returned.
     * @param len length.
     */
    public void setContentLength(int len)
    {
        if (_chunking && len>=0)
            throw new IllegalStateException("Can't chunk and have content length");
        _contentLength=len;
    }
        
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR or CRLF or LF.
     * This method only read raw data, that may be chunked.  Calling
     * readLine() will always return unchunked data.
     */
    public String readLine() throws IOException
    {
        CharBuffer buf = readCharBufferLine();
        if (buf==null)
            return null;
        return buf.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR or CRLF or LF.
     * This method only read raw data, that may be chunked.  Calling
     * readLine() will always return unchunked data.
     */
    CharBuffer readCharBufferLine() throws IOException
    {
        BufferedInputStream in = (BufferedInputStream)this.in;
        _charBuffer.reset();
        int room = _charBuffer.capacity();
        int c=0;
        boolean cr = false;
        boolean lf = false;

    LineLoop:
        while (_charBuffer._size<__maxLineLength &&
               (c=_chunking?read():in.read())!=-1)
        {
            switch(c)
            {
              case 10:
                  lf = true;
                  break LineLoop;
        
              case 13:
                  cr = true;
                  if (!_chunking)
                      in.mark(2);
                  break;
        
              default:
                  if(cr)
                  {
                      if (_chunking)
                          Code.fail("Cannot handle CR in chunking mode");
                      in.reset();
                      break LineLoop;
                  }
                  else
                  {
                      if (--room < 0)
                      {
                          _charBuffer.expand();
                          room = _charBuffer.capacity();
                      }
                      _charBuffer.add((char)c);
                  }
                  break;
            }    
        }

        if (c==-1 && _charBuffer._size==0)
            return null;

        return _charBuffer;
    }
    
    /* ------------------------------------------------------------ */
    public int read()
        throws IOException
    {
        if (_chunking)
        {   
            int b=-1;
            if (_chunksize<=0 && getChunkSize()<=0)
                return -1;
            b=in.read();
            _chunksize=(b<0)?-1:(_chunksize-1);
            return b;
        }

        if (_contentLength==0)
            return -1;
        int b=in.read();
        if (_contentLength>0)
            _contentLength--;
        return b;
    }
 
    /* ------------------------------------------------------------ */
    public int read(byte b[]) throws IOException
    {
        int len = b.length;
    
        if (_chunking)
        {   
            if (_chunksize<=0 && getChunkSize()<=0)
                return -1;
            if (len > _chunksize)
                len=_chunksize;
            len=in.read(b,0,len);
            _chunksize=(len<0)?-1:(_chunksize-len);
        }
        else
        {
            if (_contentLength==0)
                return -1;
            if (len>_contentLength && _contentLength>=0)
                len=_contentLength;
            len=in.read(b,0,len);
            if (_contentLength>0 && len>0)
                _contentLength-=len;
        }

        return len;
    }
 
    /* ------------------------------------------------------------ */
    public int read(byte b[], int off, int len) throws IOException
    {
        if (_chunking)
        {   
            if (_chunksize<=0 && getChunkSize()<=0)
                return -1;
            if (len > _chunksize)
                len=_chunksize;
            len=in.read(b,off,len);
            _chunksize=(len<0)?-1:(_chunksize-len);
        }
        else
        {
            if (_contentLength==0)
                return -1;
            if (len>_contentLength && _contentLength>=0)
                len=_contentLength;
            len=in.read(b,off,len);
            if (_contentLength>0 && len>0)
                _contentLength-=len;
        }

        return len;
    }
    
    /* ------------------------------------------------------------ */
    public long skip(long len) throws IOException
    {
        if (_chunking)
        {   
            if (_chunksize<=0 && getChunkSize()<=0)
                return -1;
            if (len > _chunksize)
                len=_chunksize;
            len=in.skip(len);
            _chunksize=(len<0)?-1:(_chunksize-(int)len);
        }
        else
        {
            len=in.skip(len);
            if (_contentLength>0 && len>0)
                _contentLength-=len;
        }
        return len;
    }

    /* ------------------------------------------------------------ */
    /** Available bytes to read without blocking.
     * If you are unlucky may return 0 when there are more
     */
    public int available() throws IOException
    {
        if (_chunking)
        {
            int len = in.available();
            if (len<=_chunksize)
                return len;
            return _chunksize;
        }
        
        return in.available();
    }
 
    /* ------------------------------------------------------------ */
    /** Close stream.
     * The close is not passed to the wrapped stream, as this is
     * needed for the response
     */
    public void close() throws IOException
    {
        Code.debug("Close");
        _chunksize=-1;
    }
 
    /* ------------------------------------------------------------ */
    /** Mark is not supported
     * @return false
     */
    public boolean markSupported()
    {
        return false;
    }
    
    /* ------------------------------------------------------------ */
    /** Not Implemented
     */
    public void reset()
    {
        Code.notImplemented();
    }

    /* ------------------------------------------------------------ */
    /** Not Implemented
     * @param readlimit 
     */
    public void mark(int readlimit)
    {
        Code.notImplemented();
    }    
    
    /* ------------------------------------------------------------ */
    private int getChunkSize()
        throws IOException
    {
        if (_chunksize<0)
            return -1;
        
        _footers=null;
        _chunksize=-1;

        // Get next non blank line
        _chunking=false;
        String line=readLine();
        while(line!=null && line.length()==0)
            line=readLine();
        _chunking=true;
        
        // Handle early EOF or error in format
        if (line==null)
            return -1;
        
        // Get chunksize
        int i=line.indexOf(';');
        if (i>0)
            line=line.substring(0,i).trim();
        _chunksize = Integer.parseInt(line,16);
        
        // check for EOF
        if (_chunksize==0)
        {
            _chunksize=-1;
            // Look for footers
            _footers = new HttpFields();
            _chunking=false;
            _footers.read(this);
        }

        return _chunksize;
    }
    
    /* ------------------------------------------------------------ */
    /** Get footers
     * Only valid after EOF has been returned
     * @return HttpHeader containing footer fields
     */
    public HttpFields getFooters()
    {
        return _footers;
    }

};


