// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import com.mortbay.FTP.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;


/* --------------------------------------------------------------------- */
/** Proxy HttpHandler
 * This handler can service requests of the form:<PRE>
 * METHOD [http|ftp|file]:URL VERSION
 * </PRE>
 * Which are generated by browsers that are talking to a proxy.  This
 * handler can be used as a simple proxy or the basis of an advanced
 * proxy.
 * <h3>Notes</h3>
 * The handler must be installed in a handler stack starting with "http:"
 * or "ftp:".  For a HTTP proxy, the ParamHandler should also be in the
 * stack before the ProxyHandler, so that form content is read.
 */
public class ProxyHandler extends NullHandler
{
    /* ------------------------------------------------------------ */
    /** Constructor from properties.
     * Calls setProperties.
     * @param properties Configuration properties
     */
    public ProxyHandler(Properties properties)
    {
	setProperties(properties);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ProxyHandler()
    {}
    
    /* ------------------------------------------------------------ */
    /** Configure from properties.
     * No configuration parameters for this handler
     * @param properties configuration.
     */
    public void setProperties(Properties properties)
    {}
    
    /* ----------------------------------------------------------------- */
    /** Handle proxy requests.
     */
    public void handle(HttpRequest request,
                       HttpResponse response)
         throws IOException
    {
	String url = request.getRequestLine();
	try{
	    int s1=url.indexOf(' ',0);
	    int s2=url.indexOf(' ',s1+1);
	    url=url.substring(s1+1,s2);
	}
	catch(Exception e)
	{
	    Code.warning(e);
	    url="";
	}
	
	Code.debug("Proxy request "+url);
	if (url.startsWith("file:"))
	    getFile(response,url);
	else if (url.startsWith("http:"))
	    getHTTP(response,url);
	else if (url.startsWith("ftp:"))
	    getFTP(response,url);
    }

    /* ---------------------------------------------------------------- */
    void getFile(HttpResponse response,String url)
	 throws IOException
    {
	String mimeType = httpServer.getMimeType(url);
	String filename = url.substring(url.indexOf(":")+1);
	if (filename.indexOf("?")>=0)
	    filename=filename.substring(0,filename.indexOf("?"));
	Code.debug("get File="+filename+" of type "+mimeType);
	response.setContentType(mimeType);
	File file = new File(filename);
	FileInputStream in =new FileInputStream(file);
	IO.copy(in,response.getOutputStream());
    }
    
    /* ---------------------------------------------------------------- */
    void getHTTP(HttpResponse response,String urlStr)
	 throws IOException
    {
	Code.debug("get URL="+urlStr);
	HttpRequest request = response.getRequest();
	URL url=new URL(urlStr);

	int port = url.getPort() ;
	Socket socket= new Socket(url.getHost(),port<0?80:port);

	try{
	    String newPath = new URI(url.getFile()).getPath();
	    request.translateAddress(request.getResourcePath(),
				     newPath,true);
	    request.setHeader(HttpHeader.Connection,null);
	    request.setHeader("Host",null);
	    request.setVersion(request.HTTP_1_0);
	    request.write(socket.getOutputStream());
	    Code.debug("waiting for forward reply...");
	    response.writeInputStream(socket.getInputStream(),-1,true);
	}
	finally
	{
	    socket.close();
	};
    }
    
    /* ---------------------------------------------------------------- */
    void getFTP(HttpResponse response,String url)
	 throws IOException
    {
	String mimeType = httpServer.getMimeType(url);
	Code.debug("FTP="+url+" of type "+mimeType);
	Ftp ftp = new Ftp();
	ftp.getUrl(url,response.getOutputStream());
    }
}
