// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Filter;
import com.mortbay.HTTP.HTML.*;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTML.*;
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
 * static method described.  Arguements that can be passed
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
 * @see com.mortbay.HTTP.Handler.FilterHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class HtmlFilter extends HttpFilter
{    
    /* ------------------------------------------------------------- */
    int state=0;
    StringBuffer tagBuf = new StringBuffer();
    
    /* ------------------------------------------------------------- */
    public void write(byte[]  b)
	 throws IOException
    {
	write(b,0,b.length);
    }
     
    /* ------------------------------------------------------------- */
    public void write(byte  b[], int  off, int  len)
	 throws IOException
    {
	int l = off+len;
	for (int i=off; i<l;i++)
	    write(b[i]);
    }
    
    /* ------------------------------------------------------------- */
    public void write(int  b)
	 throws IOException
    {
	String tag=null;
	
	switch((char)b)
	{
	  case '<':
	      state=(state==0)?1:0;
	      break;
	  case '!':
	      state=(state==1)?2:0;
	      break;
	  case '=':
	      state=(state==2)?3:0;
	      break;
	  case '>':
	      state=(state==4)?5:0;
	      break;
	  default:
	      state=(state==3||state==4)?4:0;
	}

	switch(state)
	{
	  case 1:
	  case 2:
	  case 3:
	  case 4:
	      tagBuf.append((char)b);
	      break;
	  case 5:
	      try{
		  tag=tagBuf.toString();
		  tag=tag.substring(3);

		  Code.debug("Found tag "+tag);
		  Hashtable named = new Hashtable();
		  named.put("this",info);
		  named.put("data",info);
		  named.put("info",info);
		  named.put("out",out);
		  named.put("request",request);
		  
		  Object o = null;
		  try{
		      o=new MethodTag(tag,named,request).invoke();
		  }
		  catch(ClassNotFoundException e)
		  {
		      Code.debug("tag problem with "+tag,e);
		      out.write("<P><B><PRE>".getBytes());
		      e.printStackTrace(new PrintWriter(out));
		      out.write("</PRE></B><P>".getBytes());
		  }
		  catch(NoSuchMethodException e)
		  {
		      Code.debug("tag problem with "+tag,e);
		      out.write("<P><B><PRE>".getBytes());
		      e.printStackTrace(new PrintWriter(out));
		      out.write("</PRE></B><P>".getBytes());
		  }
		  catch(InvocationTargetException e)
		  {
		      Code.debug("tag problem with "+tag,
				 e.getTargetException());
		      Code.debug("at",e);
		      out.write("<P><B><PRE>".getBytes());
		      e.getTargetException()
			  .printStackTrace(new PrintWriter(out));
		      out.write("</PRE></B><P>".getBytes());
		  }
		  catch(IllegalAccessException e)
		  {
		      Code.debug("tag problem with "+tag,e);
		      out.write("<P><B><PRE>".getBytes());
		      e.printStackTrace(new PrintWriter(out));
		      out.write("</PRE></B><P>".getBytes());
		  }
		  if (o!=null)
		      out.write(o.toString().getBytes());
	      }
	      finally{
		  tagBuf.setLength(0);
	      }
	      break;
	  case 0:
	      if (tagBuf.length()>0)
	      {
		  out.write(tagBuf.toString().getBytes());
		  tagBuf.setLength(0);
	      }
	      out.write(b);
	}	
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
	EmbedUrl embed=null;

	URL relUrl = new URL("http",
			     request.getServerName(),
			     request.getServerPort(),
			     request.getRequestPath());
	
	URL url = new URL(relUrl,urlString);

	if (proxyUrlString!=null)
	{
	    URL proxyUrl = new URL(proxyUrlString);
	    InetAddrPort proxy = new InetAddrPort();
	    proxy.inetAddress = InetAddress.getByName(proxyUrl.getHost());
	    proxy.port = proxyUrl.getPort();

	    embed = new EmbedUrl(url,proxy);
	}
	else
	    embed = new EmbedUrl(url);

	embed.write(out);
    }    
}



