// ========================================================================
// A very basic CGI Servlet, for use, originally, with Jetty
// (www.mortbay.com). It's heading towards CGI/1.1 compliance, but
// still lacks a few features - the basic stuff is here though...
// Copyright 2000 Julian Gosnell <jules_gosnell@yahoo.com>
// Released under the terms of the Jetty Licence.
//
// For all problems with this servlet please email jules_gosnell@yahoo.com,
// NOT the Jetty support lists
//
// ========================================================================

package com.mortbay.Servlet;

import java.io.*;

import java.util.List;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.mortbay.Util.Code;
import com.mortbay.Util.IO;
import com.mortbay.Util.LineInput;
import com.mortbay.HTTP.HttpFields;

//-----------------------------------------------------------------------------
public class CGI extends HttpServlet
{
  protected Vector _roots = new Vector();

  public void
    init()
  {
    Code.debug("CGI: starting initialisation");

    // check and cache root directories...
    String tmp = getInitParameter("Roots");

    if (tmp==null)
      Code.fail("CGI: Roots init parameter not set");

    StringTokenizer roots = new StringTokenizer(tmp, ",");
    while (roots.hasMoreTokens())
    {
      File dir = new File(roots.nextToken());

      if (!dir.exists())
      {
	Code.warning("CGI: "+dir+" does not exist");
	continue;
      }

      if (!dir.canRead())
      {
	Code.warning("CGI: "+dir+" is not readable");
	continue;
      }

      if (!dir.isDirectory())
      {
	Code.warning("CGI: "+dir+" is not a directory");
	continue;
      }

      try
      {
	File canonical=dir.getCanonicalFile();
	_roots.add(0,canonical);
	Code.debug("CGI: accepting root : "+canonical);
      }
      catch (IOException e)
      {
	Code.warning("CGI: failed to add root : "+dir+" - "+e);
      }
	
    }

    if (_roots.size()==0)
      Code.fail("CGI: no valid roots found from : "+tmp);

    Code.debug("CGI: ending initialisation");
  }

  public void service(HttpServletRequest req, HttpServletResponse res) 
    throws ServletException, IOException
  {
    Code.debug("CGI: System.Properties : " + System.getProperties().toString());

    Enumeration p=req.getParameterNames();
    while (p.hasMoreElements())
    {
      String name  = p.nextElement().toString();
      Code.debug("CGI: Parameter - "+name+" : "+req.getParameter(name));
    }

    Enumeration h = req.getHeaderNames();
    while (h.hasMoreElements())
    {
      String name  = h.nextElement().toString();
      Code.debug("CGI: Header - "+name+" : "+req.getHeader(name));
    }

    String exe = req.getPathInfo();
    if (exe==null)
    {
      Code.fail("CGI: no executable specified");
    }

    boolean done=false;
    for (int i=0,length=_roots.size(); !done && i<length;i++)
    {
      File root = (File)_roots.elementAt(i);
      File file = new File(root, exe).getCanonicalFile();
      
      // ensure that this file is below our 'root'...
      String parent = root.toString();
      String child  = file.toString();
      
      if (!file.exists())
	continue;
      
      if (!parent.equals(child.substring(0, parent.length())))
      {
	Code.debug("CGI: "+child+" is not below "+parent);
	continue;
      }
      
      done=true;
      exec(root.toString(), file.toString(), req, res);
    }

    if (!done)
      Code.fail("CGI: could not find cgi "+exe+" in roots "+_roots);
  }

    
  /* ------------------------------------------------------------ */
  /* 
   * @param root 
   * @param path 
   * @param req 
   * @param res 
   * @exception IOException 
   */
  private void exec(String root,
		    String path,
		    HttpServletRequest req,
		    HttpServletResponse res)
    throws IOException
  {
    Code.debug("CGI: execing : "+path);
    String env[]=
      {
	// these ones are from "The WWW Common Gateway Interface Version 1.1"
	// look at : http://Web.Golux.Com/coar/cgi/draft-coar-cgi-v11-03-clean.html#6.1.1
	"AUTH_TYPE="                + req.getAuthType(),
	"CONTENT_LENGTH="           + req.getContentLength(),
	"CONTENT_TYPE="             + req.getContentType(),

	// GATEWAY_INTERFACE
	// This metavariable is set to the dialect of CGI being used by
	// the server to communicate with the script.
	"GATEWAY_INTERFACE="        + "CGI/1.1",

	"PATH_INFO="                + req.getPathInfo(),
	"PATH_TRANSLATED="          + req.getPathTranslated(),
	"QUERY_STRING="             + req.getQueryString(),
	"REMOTE_ADDR="              + req.getRemoteAddr(),
	"REMOTE_HOST="              + req.getRemoteHost(),

	// REMOTE_IDENT
	// The identity information reported about the connection by a
	// RFC 1413 [11] request to the remote agent, if
	// available. Servers MAY choose not to support this feature, or
	// not to request the data for efficiency reasons.
	// "REMOTE_IDENT="             + "NYI",

	"REMOTE_USER="              + req.getRemoteUser(),
	"REQUEST_METHOD="           + req.getMethod(),
	"SCRIPT_NAME="              + req.getRequestURI(),
	"SERVER_NAME="              + req.getServerName(),
	"SERVER_PORT="              + req.getServerPort(),
	"SERVER_PROTOCOL="          + req.getProtocol(),
	"SERVER_SOFTWARE="          + getServletContext().getServerInfo(),

	// these extra ones were from printenv on www.dev.nomura.co.uk
	//       "DOCUMENT_ROOT="            + root + "/docs",
	//       "HTTPS="                    + "NYI - OFF",
	//       "PATH="                     + "NYI - /apps/java/jdk/sun4/SunOS5/1.1.7_05/java1.1/bin:/usr/sbin:/usr/bin",
	//       "SERVER_URL="               + "NYI - http://us0245",
	//       "TZ="                       + System.getProperty("user.timezone"),

	// for the moment I am just going to assume the 'scheme' will
	// always be http... - am I right ?

	//     };

	//     if (req.getScheme().compareTo("http")==0)
	//     {
	//       String httpEnv[]=
	//       {
	"HTTP_ACCEPT="              + req.getHeader("Accept"),
	"HTTP_ACCEPT_CHARSET="      + req.getHeader("Accept-Charset"),
	"HTTP_ACCEPT_ENCODING="     + req.getHeader("Accept-Encoding"),
	"HTTP_ACCEPT_LANGUAGE="     + req.getHeader("Accept-Language"),
	"HTTP_FORWARDED="           + req.getHeader("Forwarded"),
	"HTTP_HOST="                + req.getHeader("Host"),
	"HTTP_PROXY_AUTHORIZATION=" + req.getHeader("Proxy-authorization"),
	"HTTP_USER_AGENT="          + req.getHeader("User-Agent"),
	
	// found these 2 extra headers in request from Jetty - should
	// they be included ?
	"HTTP_PRAGMA="              + req.getHeader("Pragma"),
	"HTTP_COOKIE="              + req.getHeader("Cookie"),
      };
      
    // find a better way...
    //      env = env + httpEnv;
    //    }

    // are we meant to decode args here ? or does the script get them
    // via PATH_INFO ?  if we are, they should be decoded and passed
    // into exec here...
    Process p=Runtime.getRuntime().exec(path,env);

    // hook processes input to browser's output (async)
    final InputStream inFromReq=req.getInputStream();
    final OutputStream outToCgi=p.getOutputStream();
    final int inputLength = req.getContentLength();
	
    new Thread(new Runnable()
      {
	public void run()
	{
	  try{
	    if (inputLength>0)
	      IO.copy(inFromReq,outToCgi,inputLength);
	    outToCgi.close();
	  }
	  catch(IOException e){Code.ignore(e);}
	}
      }).start();	
    

    // hook processes output to browser's input (sync)
    // if browser closes stream, we should detect it and kill process...
    try
    {
      // read any headers off the top of our input stream
      LineInput li = new LineInput(p.getInputStream());
      HttpFields fields=new HttpFields();
      fields.read(li);

      String ContentStatus = "Status";
      String location = fields.get(HttpFields.__ContentLocation);
      String status   = fields.get(ContentStatus);

      if (location!=null)
      {
	Code.debug("CGI: Found Location header - what to do?");
	// TODO
	// for location do redirect HttpResponse - sendRedirect ?
	fields.remove(HttpFields.__ContentLocation);
      }
      
      if (status!=null)
      {
	Code.debug("CGI: Found a Status header - setting status on response");
	res.setStatus(new Integer(status).intValue());
	fields.remove(ContentStatus);
      }
      
      // copy remaining fields into response...
      List headers = fields.getFieldNames();
      for (int i = 0, size=headers.size(); i<size; i++)
      {
	String key = headers.get(i).toString();
	String val = fields.get(key).toString();
	res.setHeader(key,val);
      }

      // copy remains of input onto output...
      IO.copy(li, res.getOutputStream());
    }
    catch (IOException e)
    {
      // browser has closed its input stream - we should
      // terminate script and clean up...
      Code.debug("CGI: Client closed connection!");
      p.destroy();
    }
    // think about this a bit more :
    // what should we do with our processes error stream ?
    // what should we do with it's exit value ?
    // so should we wait for it ?
    // will Greg's stuff flush and close all streams correctly ?
  }
};

//-----------------------------------------------------------------------------
