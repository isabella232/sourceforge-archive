// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Filter;
import com.mortbay.HTTP.HTML.*;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;


/* --------------------------------------------------------------------- */
/** Filter Html for java method tags
 * This HttpFilter is used to expand dynamic content within a
 * HTML template file.  The HTML template file should contain
 * java method tags where dynamic content is to be included.
 * A java method tag is a HTML comment of the form:<br>
 * &lt!=:packageName.className.methodName(arg1,arg2,...)&gt;<br>
 * The MethodTag class is used to create a method call to the
 * static method described.  Arguments that can be passed
 * include:
 * <li>Strings
 * <li>Double instances
 * <li>out - An OutputStream to which output can be directed.
 * <li>data - which is a Hashtable associated with the current request.
 * <li>request - which is the HttpRequest.
 * <li>null - null value.
 * <li>Any HTTPRequest parameter
 * <ul>
 * Any object returned by the tag is sent to the response output.
 * 
 * The special tag &lt;!=SESSION&gt; is expanded to the session
 * URL encoding if it is required.
 *
 * The assumption is made that <!= is fairly infrequent, so that
 * write(byte[]) can be efficiently implemented.
 *
 * @see com.mortbay.HTTP.Handler.FilterHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class HtmlFilter extends HttpFilter
{    
    /* ------------------------------------------------------------- */
    int state=0;
    StringBuffer tagBuf = new StringBuffer(128);
    byte[] ba={0};
    protected Hashtable info = null;


    /* ------------------------------------------------------------ */
    /** Constructor.
     * The If-Modified-Since and If-Unmodified-since headers are
     * blanked as it is possible for this filter to modify the
     * content, even if it has not changed at source.
     * @param request The Request.
     */
    public HtmlFilter(HttpRequest request)
    {
	super(request);
	if (request!=null)
	{
	    request.setHeader(HttpHeader.IfUnmodifiedSince, null);
	    request.setHeader(HttpHeader.IfModifiedSince, null);
	}
    }
    
    
    /* ------------------------------------------------------------- */
    /** Modify response on activation.
     * Remove content length and reset the last modified headers, as they
     * are possibly incorrect after filtering.
     */
    protected void activate()
    {
	info = new Hashtable();
	if(response!=null)
	{
	    response.setDateHeader(HttpHeader.LastModified,
				   System.currentTimeMillis());
	    response.setHeader(HttpHeader.ContentLength, null);
	}
    }

    /* ------------------------------------------------------------- */
    public void write(byte[]  b)
	 throws IOException
    {
	write(b,0,b.length);
    }
     
    /* ------------------------------------------------------------- */
    public void write(byte  buf[], int  off, int  len)
	 throws IOException
    {
	int end = off-1;
	int l = off+len;
	String tag=null;
	
	for (int i=off; i<l;i++)
	{
	    byte b = buf[i];
	    char c = (char)b;
	    int last=state;
	    
	    switch(c)
	    {
	      case '<':
		  switch(state)
		  {
		    case 0:case 5:state=1;break;
		    case 3: case 4: state=4;break;
		    default:state=0;
		  }
		  break;
	      case '!':
		  switch(state)
		  {
		    case 1: state=2; break;
		    case 3: case 4: state=4;break;
		    default: state=0;
		  }
		  break;
	      case '=':
		  switch(state)
		  {
		    case 2: state=3; break;
		    case 3: case 4: state=4;break;
		    default: state=0;
		  }
		  break;
	      case '>':
		  switch(state)
		  {
		    case 3: case 4: state=5; break;
		    default: state=0;
		  }
		  break;
	      default:
		  switch(state)
		  {
		    case 3: case 4: state=4;break;
		    default: state=0;
		  }
	    }

	    switch(state)
	    {
	      case 0: // Normal text
		  end=i;
		  // If we are sitting on some tag chars which are not
		  // in the buffer, write them out.
		  if(last==1 && end==off)
		      out.write('<');
		  if(last==2 && end<=off+1)
		      out.write("<!".getBytes());
		  break;
		  
	      case 1: // Got a  <
	      case 2: // Got a !
		  break;
		  
	      case 3: // Got a =
		  // write what we have got before the tag
		  if (end>=off)
		      out.write(buf,off,end-off+1);
		  break;
		  
	      case 4: // Tag text
		  tagBuf.append(c);
		  break;
		  
	      case 5: // End of tag
		  off=i+1;
		  tag=tagBuf.toString();
		  tagBuf.setLength(0);
		  if (tag==null ||tag.length()==0)
		      break;
		  
		  Code.debug("Found tag "+tag);
		  Hashtable named = new Hashtable(10);
		  named.put("this",info);
		  named.put("data",info);
		  named.put("info",info);
		  named.put("out",out);
		  if(request!=null)
		      named.put("request",request);

		  // Special case handling
		  if ("SESSION".equals(tag))
		  {
		      out.write(response.encodeUrl("").getBytes());
		      break;
		  }
		  
		  Object o = null;
		  try{
		      o=new MethodTag(tag,named,request).invoke();
		  }
		  catch(ClassNotFoundException e)
		  {
		      Code.warning("tag problem with "+tag,e);
		      out.write("<P><B><PRE>".getBytes());
		      e.printStackTrace(new PrintWriter(out));
		      out.write("</PRE></B><P>".getBytes());
		  }
		  catch(NoSuchMethodException e)
		  {
		      Code.warning("tag problem with "+tag,e);
		      out.write("<P><B><PRE>".getBytes());
		      e.printStackTrace(new PrintWriter(out));
		      out.write("</PRE></B><P>".getBytes());
		  }
		  catch(InvocationTargetException e)
		  {
		      if (e.getTargetException() instanceof java.io.IOException)
			  throw (java.io.IOException)e.getTargetException();
			  
		      Code.warning("tag problem with "+tag,e);
		      out.write("<P><B><PRE>".getBytes());
		      e.getTargetException()
			  .printStackTrace(new PrintWriter(out));
		      out.write("</PRE></B><P>".getBytes());
		  }
		  catch(IllegalAccessException e)
		  {
		      Code.warning("tag problem with "+tag,e);
		      out.write("<P><B><PRE>".getBytes());
		      e.printStackTrace(new PrintWriter(out));
		      out.write("</PRE></B><P>".getBytes());
		  }
		  if (o!=null)
		      out.write(o.toString().getBytes());
		 
		  break;
	    }	
	}

	// write what we have got that is OK
	if (end>=off)
	    out.write(buf,off,end-off+1);
    }
    
    /* ------------------------------------------------------------- */
    public void write(int  b)
	 throws IOException
    {
	ba[0]=(byte)b;
	write(ba,0,1);
    }


    /* ------------------------------------------------------------- */
    /* Include a file
     */
    public static void includeFile(OutputStream out,
				   String directory,
				   String fileName)
	 throws IOException
    {
	if (directory==null)
	    directory=".";

	Code.debug("writeFile("+directory+","+fileName+")");

	File file = new File(directory,fileName);
	BufferedInputStream in =
	    new BufferedInputStream(new FileInputStream(file));
	IO.copy(in,out);
    }

    /* ---------------------------------------------------------------- */
    /* Include a file and expand all instances of &lt; to &&lt;
     */
    public static void includePreFile(OutputStream out,
				      String directory,
				      String fileName)
	 throws IOException
    {
	if (directory==null)
	    directory=".";

	Code.debug("writePreFile("+directory+","+fileName+")");

	File file = new File(directory,fileName);
	BufferedInputStream in =
	    new BufferedInputStream(new FileInputStream(file));

	int c;
	while ((c=in.read())!=-1)
	{
	    switch((char)c)
	    {
	      case '<':
		  out.write("&lt;".getBytes());
		  break;
	      default:
		  out.write(c);
		  break;
	    }
	}
    }
    
    /* ---------------------------------------------------------------- */
    /** Embed a URL
     * GET the page at the given URL.
     * @param out Where to write the output
     * @param request The HttpRequest to make the url relative to.
     * @param url the url as a string
     * @param proxyUrl the proxy as a Url like "http://proxyhost:proxyport/"
     *        or null if no proxy
     */
    public static void embedUrl(OutputStream out,
				HttpRequest request,
				String urlString,
				String proxyUrlString)
	 throws IOException
    {
	if (request.isLocalRequest())
	{
	    HttpRequest eRequest=
		new HttpRequest(request.getHttpServer(),
				"GET",
				urlString);
	    IO.copy(eRequest.handleRequestLocally(),out);
	    eRequest.destroy();
	}
	else
	{
	    EmbedUrl embed=null;
	    URL relUrl = new URL("http",
				 request.getServerName(),
				 request.getServerPort(),
				 request.getRequestPath());
	    
	    URL url = new URL(relUrl,urlString);
	    
	    if (proxyUrlString!=null && proxyUrlString.length()>0)
	    {
		Code.debug("Proxy=",proxyUrlString);
		
		URL proxyUrl = new URL(proxyUrlString);
		InetAddrPort proxy = new InetAddrPort();
		proxy.setInetAddress(InetAddress.getByName(proxyUrl.getHost()));
		proxy.setPort(proxyUrl.getPort());
		
		embed = new EmbedUrl(url,proxy);
	    }
	    else
		embed = new EmbedUrl(url);
	    
	    embed.write(out);
	}
    }
}



