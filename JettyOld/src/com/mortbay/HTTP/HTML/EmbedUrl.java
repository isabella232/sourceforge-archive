// ===========================================================================
// Copyright (c) 1997 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.HTML;
import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;
import java.util.*;
import java.net.*;
import java.io.*;

/* -------------------------------------------------------------------- */
/** Embed URL HTML Element
 * @see class Element
 * @version $Id$
 * @author Greg Wilkins
*/
public class EmbedUrl extends  Element
{
    /* ----------------------------------------------------------------- */
    private URL url = null;
    private InetAddrPort proxy = null;
    private Socket socket=null;
    HttpRequest request=null;
    HttpHeader replyHeader=null;
    HttpInputStream replyStream=null;
    
    /* ----------------------------------------------------------------- */
    public EmbedUrl(URL url)
    {
	this.url=url;
    }

    /* ----------------------------------------------------------------- */
    public EmbedUrl(URL url,
		    InetAddrPort proxy)
    {
	this.url=url;
	this.proxy=proxy;
    }

    /* ----------------------------------------------------------------- */
    private void skipHeader()
	 throws IOException
    {
	Code.debug("Embed "+url);
	Socket socket=null;	
	HttpRequest request=null;
	
	if (proxy==null)
	{
	    int port = url.getPort();
	    if (port==-1)
		port=80;
	    socket= new Socket(url.getHost(),port);
	}
	else
	{
	    socket= new Socket(proxy.getInetAddress(),
			       proxy.getPort());
	}
	
	request=new HttpRequest(null,HttpRequest.GET,url.getFile());
	
	request.write(socket.getOutputStream());   
	Code.debug("waiting for forward reply...");
	
	replyHeader = new HttpHeader();
	replyStream = new HttpInputStream(socket.getInputStream());
	String replyLine=replyStream.readLine();
	Code.debug("got "+replyLine);
	replyHeader.read(replyStream);
    }

    /* ----------------------------------------------------------------- */
    public void write(OutputStream out)
	 throws IOException
    {
	try
	{
	    skipHeader();
	    IO.copy(replyStream,out);
	    out.flush();
	}
	finally
	{
	    if (socket!=null)
		socket.close();
	    if (replyStream!=null)
		replyStream.close();
	    if (replyHeader!=null)
		replyHeader.destroy();
	    if (request!=null)
		request.destroy();
	    
	    socket=null;
	    replyStream=null;
	    replyHeader=null;
	    request=null;
	}
    }
    
    /* ----------------------------------------------------------------- */
    public void write(Writer out)
	 throws IOException
    {
	try
	{
	    skipHeader();
	    IO.copy(new InputStreamReader(replyStream),out);
	    out.flush();
	}
	finally
	{
	    if (socket!=null)
		socket.close();
	    if (replyStream!=null)
		replyStream.close();
	    if (replyHeader!=null)
		replyHeader.destroy();
	    if (request!=null)
		request.destroy();
	    
	    socket=null;
	    replyStream=null;
	    replyHeader=null;
	    request=null;
	}
    }
}





