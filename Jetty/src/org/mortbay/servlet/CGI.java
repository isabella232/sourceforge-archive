// ========================================================================
// A very basic CGI Servlet, for use, with Jetty
// (jetty.mortbay.org). It's heading towards CGI/1.1 compliance, but
// still lacks a few features - the basic stuff is here though...
// Copyright 2000 Julian Gosnell <jules_gosnell@yahoo.com> Released
// under the terms of the Jetty Licence.
//
// ========================================================================

// TODO
// - logging
// - child's stderr
// - exceptions should report to client via sendError()
// - tidy up

package org.mortbay.servlet;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.http.HttpFields;
import org.mortbay.util.Code;
import org.mortbay.util.IO;
import org.mortbay.util.LineInput;
import org.mortbay.util.StringUtil;

//-----------------------------------------------------------------------------
/** CGI Servlet.
 *
 * The cgi bin directory can be set with the cgibinResourceBase init
 * parameter or it will default to the resource base of the context.
 *
 * @version $Revision$
 * @author Julian Gosnell
 */
public class CGI extends HttpServlet
{
    protected File _docRoot;
    protected String _path;

    /* ------------------------------------------------------------ */
    public void
        init()
        throws ServletException
    {


        String tmp = getInitParameter("cgibinResourceBase");
        if (tmp==null)
            tmp = getServletContext().getRealPath("/");

        Code.debug("CGI: CGI bin "+tmp);

        if (tmp==null)
        {
            Code.warning("CGI: no CGI bin !");
            throw new ServletException();
        }

        File dir = new File(tmp);
        if (!dir.exists())
        {
            Code.warning("CGI: CGI bin does not exist - "+dir);
            throw new ServletException();
        }

        if (!dir.canRead())
        {
            Code.warning("CGI: CGI bin is not readable - "+dir);
            throw new ServletException();
        }

        if (!dir.isDirectory())
        {
            Code.warning("CGI: CGI bin is not a directory - "+dir);
            throw new ServletException();
        }
    
        try
        {
            _docRoot=dir.getCanonicalFile();
            Code.debug("CGI: CGI bin accepted - "+_docRoot);
        }
        catch (IOException e)
        {
            Code.warning("CGI: CGI bin failed - "+dir);
            e.printStackTrace();
            throw new ServletException();
        }

        _path=getInitParameter("Path");
        Code.debug("CGI: PATH accepted - "+_path);
    }

    /* ------------------------------------------------------------ */
    public void service(HttpServletRequest req, HttpServletResponse res) 
        throws ServletException, IOException
    {
        Code.debug("CGI: req.getContextPath() : ",req.getContextPath());
        Code.debug("CGI: req.getServletPath() : ",req.getServletPath());
        Code.debug("CGI: req.getPathInfo()    : ",req.getPathInfo());

        Code.debug("CGI: System.Properties : " + System.getProperties().toString());

        // pathInfo() actually comprises scriptName/pathInfo...We will
        // walk backwards up it until we find the script - the rest must
        // be the pathInfo;

        // what about dos - '\'...should we use pathSeparator ?

        String both=req.getPathInfo();
        String first=both;
        String last="";
        File exe=new File(_docRoot, first);

        while ((first.endsWith("/") || !exe.exists()) && first.length()>=0)
        {
            int index=first.lastIndexOf('/'); 
      
            first=first.substring(0, index);
            last =both.substring(index, both.length());
            exe=new File(_docRoot, first);
        }
    
        if (first.length()==0 || !exe.exists())
            res.sendError(404);

        exe = exe.getCanonicalFile();

        Code.debug("CGI: script is "+exe);
        Code.debug("CGI: pathInfo is "+last);

        exec(exe.toString(), last, req, res);
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @param root 
     * @param path 
     * @param req 
     * @param res 
     * @exception IOException 
     */
    private void exec(String path,
                      String pathInfo,
                      HttpServletRequest req,
                      HttpServletResponse res)
        throws IOException
    {
        Code.debug("CGI: execing : "+path);
        String env[]=
        {
            // these ones are from "The WWW Common Gateway Interface Version 1.1"
            // look at : http://Web.Golux.Com/coar/cgi/draft-coar-cgi-v11-03-clean.html#6.1.1
            "AUTH_TYPE="                + StringUtil.nonNull(req.getAuthType()),
            "CONTENT_LENGTH="           + req.getContentLength(),
            "CONTENT_TYPE="             + StringUtil.nonNull(req.getContentType()),
            "GATEWAY_INTERFACE="        + "CGI/1.1",
            "PATH_INFO="                + StringUtil.nonNull(pathInfo),
            "PATH_TRANSLATED="          + StringUtil.nonNull(req.getPathTranslated()),
            "QUERY_STRING="             + StringUtil.nonNull(req.getQueryString()),
            "REMOTE_ADDR="              + req.getRemoteAddr(),
            "REMOTE_HOST="              + req.getRemoteHost(),

            // The identity information reported about the connection by a
            // RFC 1413 [11] request to the remote agent, if
            // available. Servers MAY choose not to support this feature, or
            // not to request the data for efficiency reasons.
            // "REMOTE_IDENT="             + "NYI",

            "REMOTE_USER="              + StringUtil.nonNull(req.getRemoteUser()),
            "REQUEST_METHOD="           + req.getMethod(),
            "SCRIPT_NAME="              + req.getRequestURI().substring(0, req.getRequestURI().length() - pathInfo.length()),
            "SERVER_NAME="              + req.getServerName(),
            "SERVER_PORT="              + req.getServerPort(),
            "SERVER_PROTOCOL="          + req.getProtocol(),
            "SERVER_SOFTWARE="          + getServletContext().getServerInfo(),
            "HTTP_ACCEPT="              + StringUtil.nonNull(req.getHeader(HttpFields.__Accept)),
            "HTTP_ACCEPT_CHARSET="      + StringUtil.nonNull(req.getHeader(HttpFields.__AcceptCharset)),
            "HTTP_ACCEPT_ENCODING="     + StringUtil.nonNull(req.getHeader(HttpFields.__AcceptEncoding)),
            "HTTP_ACCEPT_LANGUAGE="     + StringUtil.nonNull(req.getHeader(HttpFields.__AcceptLanguage)),
            "HTTP_FORWARDED="           + StringUtil.nonNull(req.getHeader(HttpFields.__Forwarded)),
            "HTTP_HOST="                + StringUtil.nonNull(req.getHeader(HttpFields.__Host)),
            "HTTP_PROXY_AUTHORIZATION=" + StringUtil.nonNull(req.getHeader(HttpFields.__ProxyAuthorization)),
            "HTTP_REFERRER="            + StringUtil.nonNull(req.getHeader(HttpFields.__Referer)),
            "HTTP_USER_AGENT="          + StringUtil.nonNull(req.getHeader(HttpFields.__UserAgent)),
        
            // found these 2 extra headers in request from Jetty - should
            // they be included ?
            "HTTP_PRAGMA="              + StringUtil.nonNull(req.getHeader(HttpFields.__Pragma)),
            "HTTP_COOKIE="              + StringUtil.nonNull(req.getHeader(HttpFields.__Cookie)),

            // these extra ones were from printenv on www.dev.nomura.co.uk
            "HTTPS="                    + (req.isSecure()?"ON":"OFF"),
            "PATH="                     + _path,
            //       "DOCUMENT_ROOT="            + root + "/docs",
            //       "SERVER_URL="               + "NYI - http://us0245",
            //       "TZ="                       + System.getProperty("user.timezone"),
        };
      
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

            if (status!=null)
            {
                Code.debug("Found a Status header - setting status on response");
                fields.remove(ContentStatus);

                // NOTE: we ignore any reason phrase, otherwise we
                // would need to use res.sendError() selectively.
                int i = status.indexOf(' ');
                if (i>0)
                    status = status.substring(0,i);
	  
                res.setStatus(Integer.parseInt(status));
            }
      
            // copy remaining headers into response...
            Enumeration headers = fields.getFieldNames();
            while (headers.hasMoreElements())
            {
                String key = headers.nextElement().toString();
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
    }
};

//-----------------------------------------------------------------------------
