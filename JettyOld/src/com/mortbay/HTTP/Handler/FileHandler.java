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
    boolean putAllowed;
    boolean deleteAllowed;
    String allowHeader = null;
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
	this.indexFile = "index.html";
	Code.debug(dirMap);
    }

    
    /* ----------------------------------------------------------------- */
    public boolean isPutAllowed()
    {
	return putAllowed;
    }
    public void setPutAllowed(boolean putAllowed_)
    {
	putAllowed = putAllowed_;
	allowHeader = null;
    }
    /* ------------------------------------------------------------ */
    public boolean isDeleteAllowed()
    {
	return deleteAllowed;
    }
    public void setDeleteAllowed(boolean deleteAllowed_)
    {
	deleteAllowed = deleteAllowed_;
	allowHeader = null;
    }
    /* ------------------------------------------------------------ */
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
		   " FILENAME=",filename+
		   " METHOD="+request.getMethod());
	
	
	// check filename
	boolean endsWithSlash= filename.endsWith("/");
	if (endsWithSlash)
	    filename = filename.substring(0,filename.length()-1);
	
	if (request.getMethod().equals(HttpRequest.GET) ||
	    request.getMethod().equals(HttpRequest.HEAD))
	    handleGet(request, response, uri, filename,
		      pathInfo, endsWithSlash);
	else if (request.getMethod().equals(HttpRequest.PUT))
	    handlePut(request, response, uri, filename);
	else if (request.getMethod().equals(HttpRequest.DELETE))
	    handleDelete(request, response, uri, filename);
	else if (request.getMethod().equals(HttpRequest.OPTIONS))
	    handleOptions(response);
	else if (request.getMethod().equals("MOVE"))
	    handleMove(request, response, uri, filename, path);
	else {
	// anything else...
	    Code.debug("Unknown action:"+request.getMethod());
	    response.sendError(501, "Not Implemented");
	}
    }


    /* ------------------------------------------------------------------- */
    void handleGet(HttpRequest request, HttpResponse response,
		   String uri, String filename,
		   String pathInfo, boolean endsWithSlash)
	throws Exception
    {
	Code.debug("Looking for ",uri," in ",filename);
	    
	File file = new File(filename);
	if (file.exists())
	{	    
	    if (!request.getMethod().equals(HttpRequest.HEAD)){
		// check any modified headers.
		long date=0;
		if ((date=request.
		     getIntHeader(HttpHeader.IfModifiedSince))>0)
		{
		    if (file.lastModified() < date)
		    {
			response.sendError(304,"Not Modified");
			return;
		    }
		}
		if ((date=request.
		     getIntHeader(HttpHeader.IfUnmodifiedSince))>0)
		{
		    if (file.lastModified() > date)
		    {
			response.sendError(412,"Precondition Failed");
			return;
		    }
		}
	    }
		
	    // check if directory
	    if (file.isDirectory())
	    {
		if (!endsWithSlash)
		{
		    Code.debug("Redirect to directory/");
			
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
				  !("/".equals(pathInfo) ||
				    pathInfo.length()==0));
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
    /* ------------------------------------------------------------ */
    void handlePut(HttpRequest request, HttpResponse response,
		   String uri, String filename)
	throws Exception
    {
	Code.debug("PUTting "+uri+" in "+filename);
	if (!putAllowed){
	    response.setHeader(HttpResponse.Allow,
			       HttpRequest.GET + HttpRequest.HEAD +
			       (deleteAllowed ? HttpRequest.DELETE : ""));
	    response.sendError(405, "Method Not Allowed");
	}
	try {
	    int toRead = request.getIntHeader(HttpHeader.ContentLength);
	    InputStream in = request.getInputStream();
	    FileOutputStream fos = new FileOutputStream(filename);
	    final int bufSize = 1024;
	    byte bytes[] = new byte[bufSize];
	    int read;
	    Code.debug(HttpHeader.ContentLength+"="+toRead);
	    while (toRead > 0 &&
		   (read = in.read(bytes, 0,
				   (toRead>bufSize?bufSize:toRead))) > 0)
	    {
		toRead -= read;
		fos.write(bytes, 0, read);
		Code.debug("Read " + read + "bytes: " + bytes);
	    }
	    fos.close();
	    response.setStatus(204, uri + " put OK...");
	    response.writeHeaders();
	} catch (SecurityException sex){
	    Code.warning(sex);
	    response.sendError(403, sex.getMessage());
	} catch (Exception ex){
	    Code.warning(ex);
	}
    }
    /* ------------------------------------------------------------ */
    void handleDelete(HttpRequest request, HttpResponse response,
		      String uri, String filename)
	throws Exception
    {
	Code.debug("DELETEting "+uri+" from "+filename);
	if (!deleteAllowed){
	    response.setHeader(HttpResponse.Allow,
			       HttpRequest.GET + HttpRequest.HEAD +
			       (putAllowed ? HttpRequest.PUT : ""));
	    response.sendError(405, "Method Not Allowed");
	    return;
	}
	File file = new File(filename);
	if (!file.exists())
	    response.sendError(405, "Method Not Allowed");
	else {
	    try {
		file.delete();
		response.setStatus(204, uri + " deleted...");
		response.writeHeaders();
	    } catch (SecurityException sex){
		Code.warning(sex);
		response.sendError(403, sex.getMessage());
	    }
	}
    }
    /* ------------------------------------------------------------ */
    void handleMove(HttpRequest request, HttpResponse response,
		    String uri, String filename, String path)
	throws Exception
    {
	if (!deleteAllowed || !putAllowed){
	    response.setHeader(HttpResponse.Allow,
			       HttpRequest.GET + HttpRequest.HEAD +
			       (putAllowed ? HttpRequest.PUT : "") +
			       (deleteAllowed ? HttpRequest.DELETE : ""));
	    response.sendError(405, "Method Not Allowed");
	    return;
	}
	String newUri = request.getHeader("New-uri");
	if (newUri.indexOf("..")>=0)
	{
	    response.sendError(405, "File contains ..");
	    return;
	}
	// Find path
	try {
	    String newPathInfo = PathMap.pathInfo(path,newUri);
	    String newFilename = dirMap.get(path) +
		(newPathInfo.startsWith("/")?"":"/")+
		newPathInfo;
	    File file = new File(filename);
	    File newFile = new File(newFilename);
	    Code.debug("Moving "+filename+" to "+newFilename);
	    file.renameTo(newFile);
	    response.setStatus(204, uri + " renamed to "+newFilename);
	    response.writeHeaders();
	} catch (Exception ex){
	    Code.warning(ex);
	    response.sendError(405, "Error:"+ex);
	    return;
	}
    }
    /* ------------------------------------------------------------ */
    void handleOptions(HttpResponse response)
	throws Exception
    {
	setAllowHeader(response);
	response.writeHeaders();
    }
    /* ------------------------------------------------------------ */
    void setAllowHeader(HttpResponse response){
	if (allowHeader == null){
	    StringBuffer sb = new StringBuffer();
	    sb.append(HttpRequest.GET);
	    sb.append(" ");
	    sb.append(HttpRequest.HEAD);
	    sb.append(" ");
	    if (putAllowed){
		sb.append(HttpRequest.PUT);
		sb.append(" ");
	    }
	    if (deleteAllowed){
		sb.append(HttpRequest.DELETE);
		sb.append(" ");
	    }
	    if (putAllowed && deleteAllowed){
		sb.append("MOVE");
		sb.append(" ");
	    }
	    sb.append(HttpRequest.OPTIONS);
	    allowHeader = sb.toString();
	}
	response.setHeader(HttpResponse.Allow, allowHeader);
    }
    /* ------------------------------------------------------------ */
    void sendFile(HttpRequest request,HttpResponse response, File file)
	throws Exception
    {
	Code.debug("sendFile: "+file.getAbsolutePath());
	String encoding=httpServer.getMimeType(file.getName());
	response.setContentType(encoding);
	int len = (int)file.length();
	response.setContentLength(len);
	response.setDateHeader("Last-Modified", file.lastModified());
	InputStream in = new FileInputStream(file);
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
	if (request.getMethod().equals(HttpRequest.HEAD)){
	    // Bail out here otherwise we build the page fruitlessly and get
	    // hit with a HeadException when we try to write the page...
	    response.writeHeaders();
	    return;
	}
	
	String title = "Directory: "+base;
	
	Page page = new Page(title);
	page.add(new Heading(1,title));
	Table table = new Table(0);
	page.add(table);

	if (parent)
	{
	    table.newRow();
	    table.addCell(new Link(base+"../","Parent Directory"));
	}
	
	DateFormat dfmt=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
						       DateFormat.MEDIUM);
	String[] ls = file.list();
	for (int i=0 ; i< ls.length ; i++)
	{
	    File item = new File(file.getPath()+File.separator+ls[i]);
	    table.newRow();
	    String uri=base+ls[i];
	    if (item.isDirectory())
		uri+="/";
	    table.addCell(new Link(uri,ls[i])+"&nbsp;");
	    table.addCell(item.length()+" bytes&nbsp;").cell().right();
	    table.addCell(dfmt.format(new Date(item.lastModified())));
	}
	
	page.write(response.getOutputStream());
    }
}
