// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;

import java.io.*;
import java.util.*;


/* ------------------------------------------------------------ */
/** LineInput InputStream.
 * This buffered InputStream provides methods for reading lines
 * of bytes. The lines can be converted to String or character
 * arrays either using the default encoding or a user supplied
 * encoding.
 *
 * Buffering and data copying are highly optimized, making this
 * an ideal class for protocols that mix character encoding lines
 * with arbitrary byte data (eg HTTP).
 *
 * The buffer size is also the maximum line length in bytes and/or
 * characters. If the byte length of a line is less than the max,
 * but the character length is greater, than then trailing characters
 * are lost.
 *
 * Line termination is forgiving and accepts CR, LF, CRLF or EOF.
 * Line input uses the mark/reset mechanism, so any marks set
 * prior to a readLine call are lost.
 *
 * @version 1.0 Thu Oct  7 1999
 * @author Greg Wilkins (gregw)
 */
public class LineInput extends FilterInputStream
{
    /* ------------------------------------------------------------ */
    private byte _buf[];
    private ByteBuffer _byteBuffer;
    private InputStreamReader _reader;
    private int _contents;
    private int _avail;
    private int _pos;
    private int _mark;
    private int _byteLimit=-1;
    private LineBuffer _lineBuffer;
    private String _encoding;
    private boolean _eof=false;

    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * Default buffer and maximum line size is 2048.
     * @param in The underlying input stream.
     */
    public LineInput(InputStream in)
    {
        this(in,0);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param in The underlying input stream.
     * @param bufferSize The buffer size and maximum line length.
     */
    public LineInput(InputStream in, int bufferSize)
    {
        super(in);
        _mark = -1;
        if (bufferSize==0)
            bufferSize=2048;
        _buf = new byte[bufferSize];
        _byteBuffer=new ByteBuffer(_buf);
        _reader=new InputStreamReader(_byteBuffer);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param in The underlying input stream.
     * @param bufferSize The buffer size and maximum line length.
     * @param encoding the character encoding to use for readLine methods.
     * @exception UnsupportedEncodingException 
     */
    public LineInput(InputStream in, int bufferSize, String encoding)
        throws UnsupportedEncodingException
    {
        super(in);
        _mark = -1;
        if (bufferSize==0)
            bufferSize=2048;
        _buf = new byte[bufferSize];
        _byteBuffer=new ByteBuffer(_buf);
        _reader=new InputStreamReader(_byteBuffer,encoding);
        _encoding=encoding;
    }

    /* ------------------------------------------------------------ */
    /** Set the byte limit.
     * If set, only this number of bytes are read before EOF.
     * @param bytes Limit number of bytes, or -1 for no limit.
     */
    public void setByteLimit(int bytes)
    {
        _byteLimit=bytes;
        if (bytes>=0)
        {
            _byteLimit-=_contents-_pos;
            if (_byteLimit<0)
            {
                _avail+=_byteLimit;
                _byteLimit=0;
            }
        }
        else
            _avail=_contents;
        
    }
    
    
    /* ------------------------------------------------------------ */
    /** Get the byte limit
     * @return Number of bytes until EOF is returned or -1 for no limit.
     */
    public int getByteLimit()
    {
        if (_byteLimit<0)
            return _byteLimit;
        
        return _byteLimit+_avail-_pos;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR, LF or CRLF.
     * The default or supplied encoding is used to convert bytes to
     * characters.
     * @return The line as a String or null for EOF.
     * @exception IOException 
     */
    public synchronized String readLine()
        throws IOException
    {
        int len = fillLine(_buf.length);
        
        if (len<0)
            return null;
        
        String s=null;
        if (_encoding==null)
            s = new String(_buf,_mark,len);
        else
        {
            try
            {
                s=new String(_buf,_mark,len,_encoding);
            }
            catch(UnsupportedEncodingException e)
            {
                Code.warning(e);
            }
        }
        _mark=-1;
        return s;
    }
    
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR, LF or CRLF.
     * The default or supplied encoding is used to convert bytes to
     * characters.
     * @param c Character buffer to place the line into.
     * @param off Offset into the buffer.
     * @param len Maximum length of line.
     * @return The length of the line or -1 for EOF.
     * @exception IOException 
     */
    public int readLine(char[] c,int off,int len)
        throws IOException
    {
        int blen = fillLine(len);

        if (blen<0)
            return -1;
        if (blen==0)
            return 0;
        
        _byteBuffer.setStream(_mark,blen);
        len= _reader.read(c,off,len);
        _mark=-1;

        return len;
    }
    
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR, LF or CRLF.
     * @param b Byte array to place the line into.
     * @param off Offset into the buffer.
     * @param len Maximum length of line.
     * @return The length of the line or -1 for EOF.
     * @exception IOException 
     */
    public int readLine(byte[] b,int off,int len)
        throws IOException
    {
        len = fillLine(len);

        if (len<0)
            return -1;
        if (len==0)
            return 0;
        
        System.arraycopy(_buf,_mark, b, off, len);
        _mark=-1;

        return len;
    }

    
    /* ------------------------------------------------------------ */
    /** Read a Line ended by CR, LF or CRLF.
     * Read a line into a shared LineBuffer instance.  The LineBuffer is
     * resused between calls and should not be held by the caller.
     * The default or supplied encoding is used to convert bytes to
     * characters.
     * @return LineBuffer instance or null for EOF.
     * @exception IOException 
     */
    public LineBuffer readLineBuffer()
        throws IOException
    {
        return readLineBuffer(_buf.length);
    }
    
    /* ------------------------------------------------------------ */
    /** Read a Line ended by CR, LF or CRLF.
     * Read a line into a shared LineBuffer instance.  The LineBuffer is
     * resused between calls and should not be held by the caller.
     * The default or supplied encoding is used to convert bytes to
     * characters.
     * @param len Maximum length of a line, or 0 for default
     * @return LineBuffer instance or null for EOF.
     * @exception IOException 
     */
    public LineBuffer readLineBuffer(int len)
        throws IOException
    {
        len = fillLine(len>0?len:_buf.length);

        if (len<0)
            return null;
        
        if (_lineBuffer==null)
            _lineBuffer=new LineBuffer(_buf.length);

        if (len==0)
        {
            _lineBuffer.size=0;
            return _lineBuffer;
        }

        _byteBuffer.setStream(_mark,len);
        _lineBuffer.size=
            _reader.read(_lineBuffer.buffer,0,_lineBuffer.buffer.length);
        _mark=-1;

        return _lineBuffer;
    }
    
    /* ------------------------------------------------------------ */
    public synchronized int read() throws IOException
    {
        int b;
        if (_pos >= _avail)
            fill();
        if (_pos >= _avail)
            b= -1;
        else
            b=_buf[_pos++]&255;
        
        return b;
    }
 
 
    /* ------------------------------------------------------------ */
    public synchronized int read(byte b[], int off, int len) throws IOException
    {
        int avail = _avail-_pos;
        if (avail <= 0)
        {
            fill();
            avail = _avail-_pos;
        }

        if (avail <= 0)
            len= -1;
        else
        {
            len= (avail < len) ? avail : len;
            System.arraycopy(_buf,_pos,b,off,len);
            _pos += len;
        }
        
        return len;
    }
    
    /* ------------------------------------------------------------ */
    public long skip(long n) throws IOException
    {
        int avail = _avail-_pos;
        if (avail <= 0)
        {
            fill();
            avail = _avail-_pos;
        }

        if (avail <= 0)
            n=0;
        else
        {
            n= (avail < n) ? avail : n;
            _pos += n;
        }
        
        return n;
    }


    /* ------------------------------------------------------------ */
    public synchronized int available()
        throws IOException
    {
        int in_stream = in.available();
        if (_byteLimit>=0 && in_stream>_byteLimit)
            in_stream=_byteLimit;
        
        return _avail - _pos + in_stream;
    }

    /* ------------------------------------------------------------ */
    public synchronized void mark(int limit)
        throws IllegalArgumentException
    {
        if (limit>_buf.length)
            throw new IllegalArgumentException("limit larger than buffer");
        _mark = _pos;
    }

    /* ------------------------------------------------------------ */
    public synchronized void reset()
        throws IOException
    {
        if (_mark < 0)
            throw new IOException("Resetting to invalid mark");
        if (_byteLimit>=0)
            _byteLimit+=_pos-_mark;
        _pos = _mark;
    }

    /* ------------------------------------------------------------ */
    public boolean markSupported()
    {
        return true;
    }
    
    /* ------------------------------------------------------------ */
    private void fill()
        throws IOException
    {
        // if the mark is in the middle of the buffer
        if (_mark > 0)
        {
            // moved saved bytes to start of buffer
            int saved = _contents - _mark;
            System.arraycopy(_buf, _mark, _buf, 0, saved);
            _pos-=_mark;
            _avail-=_mark;
            _contents=saved;
            _mark=0;
        }
        else if (_mark<0 && _pos>0)
        {
            // move remaining bytes to start of buffer
            int saved = _contents-_pos;
            System.arraycopy(_buf,_pos, _buf, 0, saved);
            _avail-=_pos;
            _contents=saved;
            _pos = 0;
        }
        else if (_mark==0 && _pos>0 && _contents==_buf.length)
        {
            _mark=-1;
            fill();
            return;
        }

        _eof=false;
        if (_byteLimit==0)
            _eof=true;
        else if (_buf.length>_contents)
        {
            int space=_buf.length-_contents;
            if (_byteLimit>=0 && space>_byteLimit)
                space=_byteLimit;
            
            int n = in.read(_buf,_contents,space);
            
            _eof=(n<0);
            if (!_eof)
                _contents+=n;
            _avail=_contents;
            
            if (_byteLimit>0)
            {
                if (_contents-_pos >= _byteLimit)
                    _avail=_byteLimit+_pos;
                
                if (n>0)
                    _byteLimit-=n;
                else if (n==-1)
                    throw new IOException("Premature EOF");
            }
        }
    }

    
    /* ------------------------------------------------------------ */
    private int fillLine(int maxLen)
        throws IOException
    {
        _mark=_pos;
        
        if (_pos>=_avail)
            fill();
        if (_pos>=_avail)
            return -1;
        
        byte b;  
        boolean cr = false;
        boolean lf = false;

        int len=0;
        
    LineLoop:
        while (_pos<=_avail)
        {
            // if we have gone past the end of the buffer
            if (_pos==_avail)
            {
                if (_eof || (_mark==0 && _avail==_buf.length))
                {
                    cr=true;
                    lf=true;
                    break;
                }

                _pos=_mark;
                fill();
                _pos=len;
                continue;
            }

            // Get the byte
            b=_buf[_pos++];
            
            switch(b)
            {
              case 10:
                  lf = true;
                  break LineLoop;
                
              case 13:
                  cr = true;
                  break;
                
              default:
                  if(cr)
                  {
                      _pos--;
                      break LineLoop;
                  }
                  else
                  {
                      len++;
                      if (len==maxLen)
                      {
                          // look for EOL
                          if (_mark!=0 && _pos+2>=_avail && _avail<_buf.length)
                              fill();
                          
                          if (_pos<_avail && _buf[_pos]==13)
                          {
                              cr=true;
                              _pos++;
                          }
                          if (_pos<_avail && _buf[_pos]==10)
                          {
                              lf=true;
                              _pos++;
                          }
                          
                          if (!cr && !lf)
                          {
                              // fake EOL
                              lf=true;
                              cr=true;
                          }
                          break LineLoop;
                      }
                  }
                  
                  break;
            }
        }
        
        if (!cr && !lf && len==0)
        {
            len=-1;
        }

        if (Code.verbose(1000))
        {
            if (len>=0)
                Code.debug("LINE: "+new String(_buf,_mark,len));
            else
                Code.debug("EOF");
        }

        return len;
    }

    /* ------------------------------------------------------------ */
    private static class ByteBuffer extends ByteArrayInputStream
    {
        ByteBuffer(byte[] buffer)
        {
            super(buffer);
        }
        
        void setStream(int offset,int length)
        {
            pos=offset;
            count=offset+length;
            mark=-1;
        }        
    };
    
    /* ------------------------------------------------------------ */
    public static class LineBuffer
    {
        public char[] buffer;
        public int size;
        public LineBuffer(int maxLineLength)
        {buffer=new char[maxLineLength];}
    };

};
