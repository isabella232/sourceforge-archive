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
    public URL url = null;
    public InetAddrPort proxy = null;
    public String replyLine =null;
    public HttpHeader replyHeader =null;
    public HttpInputStream replyStream = null;

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
    public HttpHeader readReplyHeader()
	 throws IOException
    {
	if (replyHeader==null)
	{
	    Code.debug("Forward to "+url);
	    Socket socket=null;	
	    HttpRequest request=null;
	
	    if (proxy==null)
	    {
		int port = url.getPort();
		if (port==-1)
		    port=80;
		socket= new Socket(url.getHost(),port);
		request=new HttpRequest(HttpRequest.GET,
					url.getFile());
	    }
	    else
	    {
		socket= new Socket(proxy.getInetAddress(),
				   proxy.getPort());
		request=new HttpRequest(HttpRequest.GET,
					url.toString());
	    }

	    request.write(socket.getOutputStream());   
	    Code.debug("waiting for forward reply...");

	    replyHeader = new HttpHeader();
	    replyStream = new HttpInputStream(socket.getInputStream());
	    replyLine=replyStream.readLine();
	    Code.debug("got "+replyLine);
	    replyHeader.read(replyStream);
	}
	return replyHeader;
    }

    /* ----------------------------------------------------------------- */
    public void write(OutputStream out)
	 throws IOException
    {
	readReplyHeader();
	IO.copy(replyStream,out);
	out.flush();
    }
    
    /* ----------------------------------------------------------------- */
    public void write(Writer out)
	 throws IOException
    {
	readReplyHeader();
	IO.copy(new InputStreamReader(replyStream),out);
	out.flush();
    }
}




