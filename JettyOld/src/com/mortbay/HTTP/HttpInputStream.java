// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Base.*;

import javax.servlet.*;
import java.io.*;

/** HTTP input stream
 * <p> Implements JavaSofts wierd input stream
 *
 * <p><h4>Notes</h4>
 * <p> Not really required as it only adds a few uneeded methods and
 * delegates everything else to the stream it was constructed with.
 *
 * @version $Id$
 * @author Greg Wilkins
*/
public class HttpInputStream extends ServletInputStream
{    
    /* ------------------------------------------------------------ */
    /** The actual input stream.
     */
    private BufferedInputStream in;
    private int chunksize=0;
    com.mortbay.HTTP.HttpHeader footers=null;
    private boolean chunking=false;

    /* ------------------------------------------------------------ */
    /** Buffer for readLine, gets reused across calls */
    /* Note that the readCharBufferLine method break encapsulation
     * by returning this buffer, so BE CAREFUL!!!
     */
    static class CharBuffer
    {
	char[] chars = new char[128];
	int size=0;
    };
    CharBuffer charBuffer = new CharBuffer();
    
    
    /* ------------------------------------------------------------ */
    /** Constructor
     */
    public HttpInputStream( InputStream in)
    {
	this.in = new BufferedInputStream(in);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param chunking 
     */
    public void chunking(boolean chunking)
    {
	this.chunking=chunking;
    }

	
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR or CRLF or LF.
     * More forgiving of line termination than ServletInputStream.readLine().
     * This method only read raw data, that may be chunked.  Calling
     * ServletInputStream.readLine() will always return unchunked data.
     */
    public String readLine() throws IOException
    {
	CharBuffer buf = readCharBufferLine();
	if (buf==null)
	    return null;
	return new String(buf.chars,0,buf.size);
    }
    
    /* ------------------------------------------------------------ */
    /** Read a line ended by CR or CRLF or LF.
     * More forgiving of line termination than ServletInputStream.readLine().
     * This method only read raw data, that may be chunked.  Calling
     * ServletInputStream.readLine() will always return unchunked data.
     */
    CharBuffer readCharBufferLine() throws IOException
    {
	BufferedInputStream in = this.in;
	
	int room = charBuffer.chars.length;
	charBuffer.size=0;
	int c;  
	boolean cr = false;
	boolean lf = false;

    LineLoop:
	while ((c=chunking?read():in.read())!=-1)
	{
	    switch(c)
	    {
	      case 10:
		  lf = true;
		  break LineLoop;
        
	      case 13:
		  cr = true;
		  if (!chunking)
		      in.mark(2);
		  break;
        
	      default:
		  if(cr)
		  {
		      if (chunking)
			  Code.fail("Cannot handle CR in chunking mode");
		      in.reset();
		      break LineLoop;
		  }
		  else
		  {
		      if (--room < 0)
		      {
			  char[] old = charBuffer.chars;
			  charBuffer.chars =new char[charBuffer.chars.length+128];
			  room = charBuffer.chars.length-charBuffer.size-1;
			  System.arraycopy(old,0,charBuffer.chars,0,charBuffer.size);
		      }
		      charBuffer.chars[charBuffer.size++] = (char) c;
		  }
		  break;
	    }    
	}

	if (c==-1 && charBuffer.size==0)
	    return null;

	return charBuffer;
    }
    
    /* ------------------------------------------------------------ */
    public int read() throws IOException
    {
	if (chunking)
	{   
	    int b=-1;
	    if (chunksize<=0 && getChunkSize()<=0)
		return -1;
	    b=in.read();
	    chunksize=(b<0)?-1:(chunksize-1);
	    return b;
	}
    
	return in.read();
    }
 
    /* ------------------------------------------------------------ */
    public int read(byte b[]) throws IOException
    {
	int len = b.length;
    
	if (chunking)
	{   
	    if (chunksize<=0 && getChunkSize()<=0)
		return -1;
	    if (len > chunksize)
		len=chunksize;
	    len=in.read(b,0,len);
	    chunksize=(len<0)?-1:(chunksize-len);
	}
	else
	    len=in.read(b,0,len);

	return len;
    }
 
    /* ------------------------------------------------------------ */
    public int read(byte b[], int off, int len) throws IOException
    {
	if (chunking)
	{   
	    if (chunksize<=0 && getChunkSize()<=0)
		return -1;
	    if (len > chunksize)
		len=chunksize;
	    len=in.read(b,off,len);
	    chunksize=(len<0)?-1:(chunksize-len);
	}
	else
	    len=in.read(b,off,len);
	return len;
    }
    
    /* ------------------------------------------------------------ */
    public long skip(long len) throws IOException
    {
	if (chunking)
	{   
	    if (chunksize<=0 && getChunkSize()<=0)
		return -1;
	    if (len > chunksize)
		len=chunksize;
	    len=in.skip(len);
	    chunksize=(len<0)?-1:(chunksize-(int)len);
	}
	else
	    len=in.skip(len);
	return len;
    }

    /* ------------------------------------------------------------ */
    /** Available bytes to read without blocking.
     * If you are unlucky may return 0 when there are more
     */
    public int available() throws IOException
    {
	if (chunking)
	{
	    int len = in.available();
	    if (len<=chunksize)
		return len;
	    return chunksize;
	}
	
        return in.available();
    }
 
    /* ------------------------------------------------------------ */
    public void close() throws IOException
    {
	Code.debug("Close");
        in.close();
	chunksize=-1;
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
	if (chunksize<0)
	    return -1;
	
	footers=null;
	chunksize=-1;

	// Get next non blank line
	chunking=false;
	String line=readLine();
	while(line!=null && line.length()==0)
	    line=readLine();
	chunking=true;
	
	// Handle early EOF or error in format
	if (line==null)
	    return -1;
	
	// Get chunksize
	int i=line.indexOf(';');
	if (i>0)
	    line=line.substring(0,i).trim();
	chunksize = Integer.parseInt(line,16);
	
	// check for EOF
	if (chunksize==0)
	{
	    chunksize=-1;
	    // Look for footers
	    footers = new com.mortbay.HTTP.HttpHeader();
	    chunking=false;
	    footers.read(this);
	}

	return chunksize;
    }
    
    /* ------------------------------------------------------------ */
    /** Get footers
     * Only valid after EOF has been returned
     * @return HttpHeader containing footer fields
     */
    public com.mortbay.HTTP.HttpHeader getFooters()
    {
	return footers;
    }

};


