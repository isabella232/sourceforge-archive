// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.PropertyTree;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;



/* --------------------------------------------------------------------- */
/** log handler
 * <p>This Handler logs all requests to Writers in a "standard" format
 * <p>
 * The log handler is configured with PathMap mapping from
 * a path to a writer.
 * <h3>Notes</h3>
 * As dynamic pages often do not have Content-Length set, the log handler
 * has the option of installing a output filter to count the
 * character sent on each request.  This does involve some
 * additional overhead, so it should only be used if the statistics are
 * required.
 *
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class LogHandler extends NullHandler implements Observer
{
    /* ----------------------------------------------------------------- */
    PathMap loggers;
    boolean countContentLength;
    boolean longForm;	
    
    /* ----------------------------------------------------------------- */
    /** Constructor from properties.
     * Calls setProperties.
     */
    public LogHandler(Properties properties)
    {
	setProperties(properties);
    }
    
    /* ----------------------------------------------------------------- */
    /** Constructor
     * @param loggers PathMap of path to Writer to write log to.
     * @param countContentLength if true the output is filtered for the
     *        content size.
     * @param longForm if true, output is in the long form aka Netscape
     */
    public LogHandler(PathMap loggers,
		      boolean countContentLength,
		      boolean longForm)
    {
	this.loggers=loggers;
	this.countContentLength=countContentLength;
	this.longForm=longForm;
    }

    /* ------------------------------------------------------------ */
    /** Configure from properties.
     * Format of properties is expected to be that of a PropertyTree with
     * the following root nodes:
     * <BR>Log - A property tree mapping keys to log sinks.
     * <BR>LongForm - Boolean, if true the log is the long format
     * <BR>CountContentLength - Boolean, if true count the bytes of 
     * replies without a content length header (expensive).
     * @param properties configuration.
     */
    public void setProperties(Properties properties)
    {
	PropertyTree tree=null;
	if (properties instanceof PropertyTree)
	    tree = (PropertyTree)properties;
	else
	    tree = new PropertyTree(properties);

	loggers=new PathMap();
	
	Properties logs = tree.getTree("Log");
	Enumeration e = logs.keys();
	while (e.hasMoreElements())
	{
	    String logPath = e.nextElement().toString();
	    String log = logs.getProperty(logPath);
	    
	    OutputStreamWriter out = null;
	    if ("out".equals(log))
		out=new OutputStreamWriter(System.out);
	    else if ("err".equals(log))
		out=new OutputStreamWriter(System.err);
	    else
	    {
		try
		{
		    out=new OutputStreamWriter(new FileOutputStream(log));
		}
		catch(IOException ex)
		{
		    Code.warning(ex);
		    out=new OutputStreamWriter(System.err);
		}
	    }
	    loggers.put(logPath,out);
	}
	
	countContentLength=tree.getBoolean("CountContentLength");
	longForm=tree.getBoolean("LongForm");
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {
	response.addObserver(this);
    }
  
    /* ----------------------------------------------------------------- */
    /** Observer update called by HttpResponse
     */
    public void update(Observable o, Object arg)
    {
	HttpResponse response = (HttpResponse)arg;
	HttpRequest request = response.getRequest();
	
	String path = request.getResourcePath();
	Writer writer = (Writer)loggers.getLongestMatch(path);
	
	if (writer != null)
	{
		
	    try{
		if (countContentLength)
		    new LogFilter(this,writer).activateOn(response);
		else
		{
		    int cl = 
			response.getIntHeader(HttpHeader.ContentLength);
		    log(writer,response,cl);
		}
	    }
	    catch(IOException e){
		Code.debug("Convert to RuntimeException",e);
		throw new RuntimeException(e.toString());
	    }
	}
    }

    /* ------------------------------------------------------------- */
    void log(Writer out, HttpResponse response, long length)
	 throws IOException
    {
	HttpRequest request = response.getRequest();

	String log=null;
	
	String bytes = ((length>=0)?Long.toString(length):"-");
	String user = request.getRemoteUser();
	if (user==null)
	    user = "-";
	
	if (longForm)
	{
	    String referer = request.getHeader(HttpHeader.Referer);
	    if (referer==null)
		referer="-";
	    else
		referer="\""+referer+"\"";

	    String agent = request.getHeader(HttpHeader.UserAgent);
	    if (agent==null)
		agent="-";
	    else
		agent="\""+agent+"\"";
	    
	    log= request.getRemoteAddr()+
		" - "+
		user +
		" [" +
		response.getHeader(response.Date)+
		"] \""+
		request.getRequestLine()+
		"\" "+
		referer +
		" " +
		response.getStatus()+
		" " +
		bytes +
		" - " +
		agent +
		"\n";
	}
	else
	{
	    log= request.getRemoteAddr()+
		" - "+
		user +
		" [" +
		response.getHeader(response.Date)+
		"] \""+
		request.getRequestLine()+
		"\" "+
		response.getStatus()+
		" " +
		bytes +
		"\n";
	}
	
	synchronized(out){
	    out.write(log);
	    out.flush();
	}
    }
}




/* --------------------------------------------------------------------- */
class LogFilter extends HttpFilter
{
    /* ------------------------------------------------------------- */
    int count=0;
    
    /* ------------------------------------------------------------- */
    private LogHandler log;
    private Writer writer;
    
    /* ------------------------------------------------------------- */
    LogFilter(LogHandler log,Writer writer)
    {
	this.log=log;
	this.writer=writer;
    }

    /* ------------------------------------------------------------- */
    protected boolean canHandle(String contentType)
    {
	return true;
    }

    /* ------------------------------------------------------------- */
    public void write(byte[]  b)
	 throws IOException
    {
	count+=b.length;
	out.write(b,0,b.length);
    }
    
    /* ------------------------------------------------------------- */
    public void write(byte  b[], int  off, int  len)
	 throws IOException
    {
	count+=len;
	out.write(b,off,len);
    }
    
    /* ------------------------------------------------------------- */
    public void write(int  b)
	 throws IOException
    {
	count++;
	out.write(b);
    }

    /* ------------------------------------------------------------- */
    public void close()
	 throws IOException
    {
	log.log(writer,response,count);
	out.close();
    }
}
