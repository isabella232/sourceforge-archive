// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;

import java.io.*;
import java.util.*;


/* ------------------------------------------------------------ */
/** XXX
 *
 * @version 1.0 Thu Oct  7 1999
 * @author Greg Wilkins (gregw)
 */
public class LineInput extends BufferedInputStream
{
    /* ------------------------------------------------------------ */
    public static class LineBuffer
    {
        public char[] buffer;
        public int size;
        public LineBuffer(int maxLineLength)
        {buffer=new char[maxLineLength];}
    };

    /* ------------------------------------------------------------ */
    private int _byteLimit=-1;
    private LineBuffer _lineBuffer;
    
    /* ------------------------------------------------------------ */
    /** Constructor
     */
    public LineInput(InputStream in)
    {
        super(in);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor
     */
    public LineInput(InputStream in, int bufferSize)
    {
        super(in,bufferSize);
    }

    /* ------------------------------------------------------------ */
    /** Set the byte limit.
     * If set, only this number of bytes are returned before EOF.
     * @param bytes Limit number of bytes, or -1 for no limit.
     */
    public void setByteLimit(int bytes)
    {
        _byteLimit=bytes;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the byte limit
     * @return Number of bytes until EOF is returned or -1 for no limit.
     */
    public int getByteLimit()
    {
        return _byteLimit;
    }
    
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR, LF or CRLF.
     * A new Stringbuffer and String is allocated for each call, so
     * the other readLine methods are the more efficient.
     */
    public String readLine() throws IOException
    {
        if (_byteLimit==0)
            return null;
        
        StringBuffer buf = new StringBuffer(256);
        int c;  
        boolean cr = false;
        boolean lf = false;

    LineLoop:
        while ((c=in.read())!=-1)
        {
            if (_byteLimit>0)
                _byteLimit--;
                
            if (Code.verbose(100))
                Code.debug("Read: "+(char)c);
            
            switch(c)
            {
              case 10:
                  lf = true;
                  _byteLimit--;
                  break LineLoop;
                
              case 13:
                  cr = true;
                  in.mark(2);
                  break;
                
              default:
                  if(cr)
                  {
                      in.reset();
                      _byteLimit--;
                      break LineLoop;
                  }
                  else
                      buf.append((char)c);
                  break;
            }

            // Check the byte limit
            if (_byteLimit==0)
            {
                cr = true;
                lf = true;
                break LineLoop;
            }
        }
        
        if (!cr && !lf)
           return null;

        return buf.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR, LF or CRLF.
     * @param buf Buffer to place the line into.
     * @param off Offset into the buffer.
     * @param len Maximum length of line.
     * @return The length of the line
     * @exception IOException 
     */
    public int readLine(char[] buf,int off,int len)
        throws IOException
    {
        // Check byte limit
        if (_byteLimit==0)
            return -1;
        
        int c;  
        boolean cr = false;
        boolean lf = false;
        int l=0;

    LineLoop:
        while (l<len && (c=in.read())!=-1)
        {
            if (_byteLimit>0)
                _byteLimit--;
                  
            switch(c)
            {
              case 10:
                  lf = true;
                  break LineLoop;
                
              case 13:
                  cr = true;
                  in.mark(2);
                  break;
                
              default:
                  if(cr)
                  {
                      in.reset();
                      break LineLoop;
                  }
                  else
                  {
                      buf[off++]=(char)c;
                      l++;
                  }
                  break;
            }
            
            // Check the byte limit
            if (_byteLimit==0)
            {
                cr = true;
                lf = true;
                break LineLoop;
            }
        }
        
        if (!cr && !lf  && l<len)
           return -1;
        return l;
    }

    
    /* ------------------------------------------------------------ */
    /** Read a Line ended by CR, LF or CRLF.
     * Read a line into a shared LineBuffer instance.  The Buffer is
     * resused between calls and should not be held by the caller.
     * @param maxLineLength Maximum length of a line.
     * @return LineBuffer instance.
     * @exception IOException 
     */
    public LineBuffer readLine(int maxLineLength)
        throws IOException
    {
        // Check byte limit
        if (_byteLimit==0)
            return null;

        // Check LineBuffer
        if (_lineBuffer==null  || maxLineLength>_lineBuffer.buffer.length)
            _lineBuffer=new LineBuffer(maxLineLength);        
        
        int c;  
        boolean cr = false;
        boolean lf = false;
        _lineBuffer.size=0;

    LineLoop:
        while (_lineBuffer.size<maxLineLength && (c=in.read())!=-1)
        {
            if (_byteLimit>0)
                _byteLimit--;
            
            switch(c)
            {
              case 10:
                  lf = true;
                  break LineLoop;
                
              case 13:
                  cr = true;
                  in.mark(2);
                  break;
                
              default:
                  if(cr)
                  {
                      in.reset();
                      break LineLoop;
                  }
                  else
                      _lineBuffer.buffer[_lineBuffer.size++]=(char)c;
                      
                  break;
            }
            
            // Check the byte limit
            if (_byteLimit==0)
            {
                cr = true;
                lf = true;
                break LineLoop;
            }
        }
        
        if (!cr && !lf && _lineBuffer.size<maxLineLength)
            return null;
        
        return _lineBuffer;
    }
    
    /* ------------------------------------------------------------ */
    public int read() throws IOException
    {
        if (_byteLimit==0)
            return -1;
        int b=in.read();
        
        if (_byteLimit>0)
        {
            if (b!=-1)
                _byteLimit--;
            else if (Code.debug())
                Code.warning("Premature EOF");
        }
        return b;
    }
 
    /* ------------------------------------------------------------ */
    public int read(byte b[]) throws IOException
    {
        int len = b.length;
        if (_byteLimit==0)
            return -1;
        if (len>_byteLimit && _byteLimit>=0)
            len=_byteLimit;
        len=in.read(b,0,len);
        
        if (_byteLimit>0) 
        {
            if (len>=0)
                _byteLimit-=len;
            else if (Code.debug())
                Code.warning("Premature EOF");
        }
        return len;
    }
 
    /* ------------------------------------------------------------ */
    public int read(byte b[], int off, int len) throws IOException
    {
        if (_byteLimit==0)
            return -1;
        if (len>_byteLimit && _byteLimit>=0)
            len=_byteLimit;
        len=in.read(b,off,len);
        
        if (_byteLimit>0) 
        {
            if (len>=0)
                _byteLimit-=len;
            else if (Code.debug())
                Code.warning("Premature EOF");
        }
        return len;
    }
    
    /* ------------------------------------------------------------ */
    public long skip(long n) throws IOException
    {
        if (n>_byteLimit)
            n=_byteLimit;
        n=in.skip(n);
        if (n>0)
            _byteLimit-=n;
        return n;
    }
};





