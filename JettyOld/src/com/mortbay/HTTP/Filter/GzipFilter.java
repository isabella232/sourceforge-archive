// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Filter;
import com.mortbay.HTTP.HTML.*;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;


/* ------------------------------------------------------------ */
/** GZIP content encoding.
 * Experimental filter to apply gzip encoding to content.
 * @version 1.0 Thu Sep  9 1999
 * @author Greg Wilkins (gregw)
 */
public class GzipFilter extends HttpFilter
{
    byte[] ba= {(byte)0};
    GZIPOutputStream _gzOut;
    
    /* ------------------------------------------------------------- */
    public GzipFilter(HttpRequest request)
    {
	super(request);
    }
    
    /* ----------------------------------------------------------------- */
    /** Can handle text/html
     */
    protected boolean canHandle(String contentType)
    {
	if (!contentType.startsWith("text/html"))
	    return false;
	
	String accept=request.getHeader("Accept-Encoding");	
	return accept!=null && accept.indexOf("gzip")>=0;
    }
    
    /* ------------------------------------------------------------- */
    protected void activate()
    {
	try{
	    int content_length=response.getIntHeader(response.ContentLength);
	    String connection=response.getHeader(response.Connection);
	    String transfer_encoding=response.getHeader(response.TransferEncoding);
	    if (content_length==0)
		return;
	    
	    response.setHeader(response.ContentLength,null);
	    if (transfer_encoding==null ||
		transfer_encoding.indexOf("chunked")==-1)
		response.setHeader(response.Connection,"Close");
	    
	    _gzOut=new GZIPOutputStream(out);
	    response.setHeader("Content-Encoding","gzip");
	}
	catch(Exception e)
	{
	    Code.warning(e);
	    _gzOut=null;
	}
    }
    
    /* ------------------------------------------------------------- */
    public void write(byte  buf[], int  off, int  len)
	 throws IOException
    {
	if (_gzOut!=null)
	    _gzOut.write(buf,off,len);
	else
	    out.write(buf,off,len);
    }
    
    /* ------------------------------------------------------------- */
    public void write(byte[]  b)
	 throws IOException
    {
	write(b,0,b.length);
    }
    
    /* ------------------------------------------------------------- */
    public void write(int  b)
	 throws IOException
    {
	ba[0]=(byte)b;
	write(ba,0,1);
    }
    
    /* ------------------------------------------------------------- */
    public void close()
	throws IOException
    {
	if (_gzOut!=null)
	    _gzOut.close();
	else
	    out.close();
    }
};
