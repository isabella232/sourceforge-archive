// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;

import com.mortbay.Base.*;
import java.io.*;
import java.util.*;


// ====================================================================
public class LineInput extends InputStream
{
    // ----------------------------------------------------------------
    // ----------------------------------------------------------------
    public static void main(String args[])
    {
        LineInput input = new LineInput(System.in);
        
        try
        {
            String line;
            while ((line=input.readLine())!=null)
               System.out.println(line);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * The actual input stream.
     */
    private BufferedInputStream in;

    /* ------------------------------------------------------------ */
    /** Constructor
     */
    public LineInput(InputStream in)
    {
        this.in = new BufferedInputStream(in);
    }
    
    
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR or CRLF or LF.
     */
    public String readLine() throws IOException
    {
        StringBuffer buf = new StringBuffer(1024);

        int c;  
        boolean cr = false;
        boolean lf = false;

    LineLoop:
        while ((c=in.read())!=-1)
        {
            if (Code.verbose(100))
                Code.debug("Read: "+(char)c);
            
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
                      buf.append((char)c);
                  break;
            }    
        }
        
        if (!(cr||lf))
           return null;

        return buf.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR or CRLF or LF.
     */
    public int readLine(byte b[],
                        int off,
                        int len) throws IOException
    {
        int c;  
        boolean cr = false;
        boolean lf = false;
        int r=0;

    LineLoop:
        while (r<len && (c=in.read())!=-1)
        {
            b[off++]=(byte)c;
            r++;
            
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
                  break;
            }
        }
        return r;
    }
    
    /* ------------------------------------------------------------ */
    public int read() throws IOException
    {
        return in.read();
    }
 
    /* ------------------------------------------------------------ */
    public int read(byte b[]) throws IOException
    {
        return read(b, 0, b.length);
    }
 
    /* ------------------------------------------------------------ */
    public int read(byte b[], int off, int len) throws IOException
    {
        return in.read(b, off, len);
    }
    
    /* ------------------------------------------------------------ */
    public long skip(long n) throws IOException
    {
        return in.skip(n);
    }

    /* ------------------------------------------------------------ */
    public int available() throws IOException
    {
        return in.available();
    }
 
    /* ------------------------------------------------------------ */
    public void close() throws IOException
    {
        in.close();
    }
 
    /* ------------------------------------------------------------ */
    public synchronized void mark(int readlimit)
    {
        in.mark(readlimit);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void reset() throws IOException
    {
        in.reset();
    }
    
    /* ------------------------------------------------------------ */
    public boolean markSupported()
    {
        return in.markSupported();
    }
};


