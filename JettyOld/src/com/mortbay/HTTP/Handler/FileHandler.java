// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTML.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.text.DateFormat;

/* --------------------------------------------------------------------- */
/** FileHandler
 * This handler attempts to match files to a file or directory and
 * return a response set by the MIME type indicated by the file
 * extension.
 * The request path is used as a filepath relative to the configured
 * fileBase.
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class FileHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    PathMap dirMap;
    String indexFile;

    /* ----------------------------------------------------------------- */
    /** Construct a FileHandler at "/" for the given fileBase
     */
    public FileHandler(String fileBase)
    {
	this(fileBase,"index.html");
    }

    /* ----------------------------------------------------------------- */
    /** Construct a FileHandler at the given fileBase and use indexFile
     * as the default directory return file.
     */
    public FileHandler(String fileBase,
		       String indexFile)
    {
	dirMap = new PathMap();
	dirMap.put("/",fileBase);
	this.indexFile = indexFile;
    }
    
    /* ----------------------------------------------------------------- */
    /** Construct a FileHandler
     * @param directoryMap PathMap of pathname to directory name
     */
    public FileHandler(PathMap directoryMap)
    {
	dirMap=directoryMap;
	this.indexFile = indexFile;
	Code.debug(dirMap);
    }

    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {
	// Extract and check filename
	String uri = request.getRequestPath();
	if (uri.indexOf("..")>=0)
	{
	    Code.warning("Path with .. not handled");
	    return;
	}
	
	// Find path
	String path=dirMap.longestMatch(uri);
	if (path==null)
	    return;
	String pathInfo = PathMap.pathInfo(path,uri);
	String filename = dirMap.get(path)+
	    (pathInfo.startsWith("/")?"":"/")+
	    pathInfo;
	
	Code.debug("URI=",uri,
		   " PATH=",path,
		   " PATHINFO=",pathInfo,
		   " FILENAME=",filename);
	
	// check filename
	boolean endsWithSlash= filename.endsWith("/");
	if (endsWithSlash)
	    filename = filename.substring(0,filename.length()-1);
	endsWithSlash=endsWithSlash || path.endsWith("/");
	
	Code.debug("Looking for ",uri," in ",filename);
	
	File file = new File(filename);
	if (file.exists())
	{	    
	    // check any modified headers.
	    long date=0;
	    if ((date=request.getIntHeader(HttpHeader.IfModifiedSince))>0)
	    {
		if (file.lastModified() < date)
		{
		    response.sendError(304,"Not Modified");
		    return;
		}
	    }
	    if ((date=request.getIntHeader(HttpHeader.IfUnmodifiedSince))>0)
	    {
		if (file.lastModified() > date)
		{
		    response.sendError(412,"Precondition Failed");
		    return;
		}
	    }
	    
	    // check if directory
	    if (file.isDirectory())
	    {
		if (!endsWithSlash)
		{
		    int port=request.getServerPort();
		    String q=request.getQueryString();
		    if (q!=null&&q.length()==0)
			q=null;
		    response.setHeader(HttpResponse.Location,
				       "http://"+
				       request.getServerName()+
				       (port==80?"":(":"+port))+
				       request.getRequestPath()+"/"+
				       (q==null?"":("?"+q)));
		    response.sendError(301,"Moved Permanently");
		    return;
		}

		// See if index file exists
		File index = new File(filename+"/"+indexFile);
		if (index.isFile())
		    sendFile(request,response,index);
		else
		    sendDirectory(request,response,file,
				  !("/".equals(pathInfo)||pathInfo.length()==0));
	    }
	    
	    // check if it is a file
	    else if (file.isFile())
	    {
		sendFile(request,response,file);
	    }
	    else
		// dont know what it is
		Code.warning("Unknown file type");
	}
    }


    /* ------------------------------------------------------------------- */
    void sendFile(HttpRequest request,HttpResponse response, File file)
	throws Exception
    {
	Code.debug("sendFile: "+file.getAbsolutePath());
	String encoding=httpServer.getMimeType(file.getName());
	response.setContentType(encoding);
	InputStream in = new FileInputStream(file);
	int len = (int)file.length();
	response.setContentLength(len);
	response.writeInputStream(in,len);
    }


    /* ------------------------------------------------------------------- */
    void sendDirectory(HttpRequest request,
		       HttpResponse response,
		       File file,
		       boolean parent)
	 throws Exception
    {
	Code.debug("sendDirectory: "+file);
	String base = request.getRequestURI();
	if (!base.endsWith("/"))
	    base+="/";
	
	response.setContentType("text/html");

	String title = "Directory: "+base;
	
	Page page = new Page(title);
	page.add(new Heading(1,title));
	Table table = new Table(0);
	page.add(table);

	if (parent)
	{
	    table.newRow();
	    table.addCell(new Link(base+"..","Parent Directory"));
	}
	
	DateFormat dfmt=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
						       DateFormat.MEDIUM);
	String[] ls = file.list();
	for (int i=0 ; i< ls.length ; i++)
	{
	    File item = new File(file.getPath()+File.separator+ls[i]);
	    table.newRow();
	    table.addCell(new Link(base+ls[i],ls[i])+"&nbsp;");
	    table.addCell(item.length()+"bytes&nbsp;").cell().right();
	    table.addCell(dfmt.format(new Date(item.lastModified())));
	}
	
	page.write(response.getOutputStream());
    }
}


