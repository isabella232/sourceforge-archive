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
    boolean countContentLength;
    boolean longForm;
    Writer out;
    Hashtable outMap=new Hashtable();
    DateCache dateCache=null;
    static final String lineSeparator = System.getProperty("line.separator");

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
     * @param countContentLength if true the output is filtered for the
     *        content size.
     * @param longForm if true, output is in the long form aka Netscape
     */
    public LogHandler(boolean countContentLength,
		      boolean longForm)
    {
	out=new OutputStreamWriter(System.out);
	this.countContentLength=countContentLength;
	this.longForm=longForm;
    }

    /* ------------------------------------------------------------ */
    /** Configure from properties.
     * Format of properties is expected to be that of a PropertyTree with
     * the following root nodes:
     * <BR>File - The filename of the log. "err" and "out" are special
     * file names that log to System.err and System.out. The filename
     * "com.mortbay.Base.Log" outputs to com.mortbay.Base.Log.
     * <BR>Append - Boolean, if true append to the log file.
     * <BR>LongForm - Boolean, if true the log is the long format
     * <BR>DateFormat - Simple date format. If not present, use
     *                  the format in the request.
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

	String logFilename = tree.getProperty("File");
	boolean append = tree.getBoolean("Append");
	countContentLength=tree.getBoolean("CountContentLength");
	longForm=tree.getBoolean("LongForm");
	String dateFormat=tree.getProperty("DateFormat");
	if (dateFormat!=null && dateFormat.length()>0)
	    dateCache=new DateCache(dateFormat);

	if ("out".equals(logFilename))
	    out=new OutputStreamWriter(System.out);
	else if ("err".equals(logFilename))
	    out=new OutputStreamWriter(System.err);
	else if ("com.mortbay.Base.Log".equals(logFilename))
	    out=new LogWriter();
	else
	{
	    out = (Writer)outMap.get(logFilename);
	    if (out==null)
	    {
		try
		{
		    out=new OutputStreamWriter
			(new FileOutputStream(logFilename,append));
		    outMap.put(logFilename,out);
		}
		catch(IOException ex)
		{
		    Code.warning(ex);
		    out=new OutputStreamWriter(System.err);
		}
	    }
	}
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

	try{
	    if (countContentLength)
		new LogFilter(this,out).activateOn(response);
	    else
	    {
		int cl =
		    response.getIntHeader(HttpHeader.ContentLength);
		log(out,response,cl);
	    }
	}
	catch(IOException e)
	{
	    Code.debug("Convert to RuntimeException",e);
	    throw new RuntimeException(e.toString());
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
		(dateCache==null
		 ?response.getHeader(response.Date)
		 :dateCache.format(System.currentTimeMillis()))+
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
		lineSeparator;
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
		lineSeparator;
	}

	synchronized(out){
	    out.write(log);
	    out.flush();
	}
    }


    /* --------------------------------------------------------------------- */
    /** A writer that outputs to com.mortbay.Base.Log.
     *	The implementation is limited to methods LogHandler uses.
      */
    private static class LogWriter extends Writer {

	/* --------------------------------------------------------------------- */
	/** Write a string. */
	public void write(String str)
	{
	    // Avoid an extra lineSeparator
	    String trimmed = str.substring(0, str.length() - 
					   lineSeparator.length());
	    Log.message("HTTP",trimmed,null);
	}

	/* --------------------------------------------------------------------- */
	/**
	 * Write a portion of an array of characters.
	 * We have to implement this, even though we don't use it.
	 */
	public void write(char cbuf[], int off, int len)
	{
	    Code.notImplemented();
	}

	/* --------------------------------------------------------------------- */
	public void flush() {}
	 
	public void close() {}
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
