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
    Vector indexFiles=new Vector(2);
    boolean putAllowed;
    boolean deleteAllowed;
    String allowHeader = null;

    /* ----------------------------------------------------------------- */
    /** Construct from properties.
     * @param properties Passed to setProperties
     */
    public FileHandler(Properties properties)
	throws IOException
    {
	setProperties(properties);
    }
    
    /* ----------------------------------------------------------------- */
    /** Construct a FileHandler at "/" for the given fileBase
     */
    public FileHandler(String fileBase)
    {
	this(fileBase,"index.html");
	indexFiles.addElement("index.htm");
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
	indexFiles.addElement(indexFile);
    }
    
    /* ----------------------------------------------------------------- */
    /** Construct a FileHandler
     * @param directoryMap PathMap of pathname to directory name
     */
    public FileHandler(PathMap directoryMap)
    {
	dirMap=directoryMap;
	indexFiles.addElement("index.htm");
	indexFiles.addElement("index.html");
	Code.debug(dirMap);
    }

    /* ------------------------------------------------------------ */
    /** Configure from Properties.
     * Properties are assumed to be in the format of a PropertyTree
     * like:<PRE>
     * INDEXS               : index.html,index.htm
     * PUT                  : False
     * DELETE               : False
     * FILES.name.PATHS     : /pathSpec;/list%
     * FILES.name.DIRECTORY : /Directory
     *</PRE>
     * @param properties Configuration.
     */
    public void setProperties(Properties properties)
	throws IOException
    {
	PropertyTree tree=null;
	if (properties instanceof PropertyTree)
	    tree = (PropertyTree)properties;
	else
	    tree = new PropertyTree(properties);
	Code.debug(tree);

	putAllowed=tree.getBoolean("AllowPut");
	deleteAllowed=tree.getBoolean("AllowDelete");
	indexFiles=tree.getVector("Indexes",";,");
	if (indexFiles==null || indexFiles.size()==0)
	{
	    indexFiles=new Vector(2);
	    indexFiles.addElement("index.html");
	    indexFiles.addElement("index.htm");
	}    
	
	dirMap=new PathMap();
	Enumeration names = tree.getTree("FILES").getNodes();
	while (names.hasMoreElements())
	{
	    String filesName = names.nextElement().toString();
	    Code.debug("Configuring files "+filesName);
	    PropertyTree filesTree = tree.getTree("FILES."+filesName);
	    String filesDir = filesTree.getProperty("DIRECTORY");
	    
	    Vector paths = filesTree.getVector("PATHS",",;");
	    for (int d=paths.size();d-->0;)
		dirMap.put(paths.elementAt(d),filesDir);
	}    
    }
    
    /* ----------------------------------------------------------------- */
    public boolean isPutAllowed()
    {
	return putAllowed;
    }
    
    /* ----------------------------------------------------------------- */
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
    
    /* ----------------------------------------------------------------- */
    public void setDeleteAllowed(boolean deleteAllowed_)
    {
	deleteAllowed = deleteAllowed_;
	allowHeader = null;
    }

    /* ----------------------------------------------------------------- */
    public String translate(String path)
    {
	try
	{
	    // Find pathSpec
	    String pathSpec=dirMap.longestMatch(path);
	    if (pathSpec==null)
		return path;
	    String pathInfo = PathMap.pathInfo(pathSpec,path);
	    String filename= dirMap.get(pathSpec)+
		(pathInfo.startsWith("/")?"":"/")+pathInfo;
	    return filename.replace('/',File.separatorChar);
	}
	catch(Exception e)
	{
	    Code.ignore(e);
	    return path;
	}
    }
    
    
    /* ------------------------------------------------------------ */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {
	// Extract and check filename
	String uri = request.getResourcePath();
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
        String filename =
	    (dirMap.get(path)+
	     (pathInfo.startsWith("/")?"":"/")+
	     pathInfo)
	    .replace('/',File.separatorChar);;
        
        Code.debug("URI=",uri,
                   " PATHINFO=",pathInfo,
                   " FILENAME=",filename+
                   " METHOD=",request.getMethod());
	
	// check filename
	boolean endsWithSlash= uri.endsWith("/");
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
	    response.sendError(response.SC_NOT_IMPLEMENTED);
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
	    if (!request.getMethod().equals(HttpRequest.HEAD))
	    {
		// check any modified headers.
		long date=0;
		if ((date=request.
		     getDateHeader(HttpHeader.IfModifiedSince))>0)
		{
		    if (file.lastModified() <= date)
		    {
			response.sendError(response.SC_NOT_MODIFIED);
			return;
		    }
		}
		
		if ((date=request.
		     getDateHeader(HttpHeader.IfUnmodifiedSince))>0)
		{
		    if (file.lastModified() > date)
		    {
			response.sendError(response.SC_PRECONDITION_FAILED);
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
				       request.getResourcePath()+"/"+
				       (q==null?"":("?"+q)));
		    response.sendError(301,"Moved Permanently");
		    return;
		}
		    
		// See if index file exists
		boolean indexSent=false;
		for (int i=indexFiles.size();i-->0;)
		{
		    File index = new File(filename+
					  File.separator +
					  indexFiles.elementAt(i));
		    if (index.isFile())
		    {
			sendFile(request,response,index);
			indexSent=true;
			break;
		    }
		}
		
		if (!indexSent)
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
	    response.sendError(response.SC_METHOD_NOT_ALLOWED);
	}
	try
	{
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
	    response.setStatus(response.SC_NO_CONTENT);
	    response.writeHeaders();
	}
	catch (SecurityException sex)
	{
	    Code.warning(sex);
	    response.sendError(response.SC_FORBIDDEN, sex.getMessage());
	}
	catch (Exception ex)
	{
	    Code.warning(ex);
	}
    }
    /* ------------------------------------------------------------ */
    void handleDelete(HttpRequest request, HttpResponse response,
		      String uri, String filename)
	throws Exception
    {
	Code.debug("DELETEting "+uri+" from "+filename);
	if (!deleteAllowed)
	{
	    response.setHeader(HttpResponse.Allow,
			       HttpRequest.GET + HttpRequest.HEAD +
			       (putAllowed ? HttpRequest.PUT : ""));
	    response.sendError(response.SC_METHOD_NOT_ALLOWED);
	    return;
	}
	File file = new File(filename);
	if (!file.exists())
	    response.sendError(response.SC_METHOD_NOT_ALLOWED);
	else
	{
	    try
	    {
		file.delete();
		response.setStatus(response.SC_NO_CONTENT);
		response.writeHeaders();
	    }
	    catch (SecurityException sex)
	    {
		Code.warning(sex);
		response.sendError(response.SC_FORBIDDEN, sex.getMessage());
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
	    response.sendError(response.SC_METHOD_NOT_ALLOWED);
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
		(newPathInfo.startsWith("/")?"":File.separator)+
		newPathInfo;
	    File file = new File(filename);
	    File newFile = new File(newFilename);
	    Code.debug("Moving "+filename+" to "+newFilename);
	    file.renameTo(newFile);
	    response.setStatus(response.SC_NO_CONTENT);
	    response.writeHeaders();
	} catch (Exception ex){
	    Code.warning(ex);
	    response.sendError(response.SC_METHOD_NOT_ALLOWED, "Error:"+ex);
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
	    StringBuffer sb = new StringBuffer(128);
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

	PrintWriter out=response.getWriter();

	out.print("<HTML><HEAD><TITLE>");
	out.print(title);
	out.print("</TITLE></HEAD><BODY>\n<H1>");
	out.print(title);
	out.print("</H1><TABLE BORDER=0>");
	
	if (parent)
	{
	    out.print("<TR><TD><A HREF=");
	    out.print(base);
	    out.print("../>Parent Directory</A></TD><TD></TD><TD></TD></TR>\n");
	    
	}
	
	DateFormat dfmt=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
						       DateFormat.MEDIUM);
	String[] ls = file.list();
	for (int i=0 ; i< ls.length ; i++)
	{
	    File item = new File(file.getPath()+File.separator+ls[i]);
	    out.print("<TR><TD><A HREF=");
	    String uri=base+ls[i];
	    if (item.isDirectory())
		uri+="/";
	    out.print(uri);
	    out.print(">");
	    out.print(ls[i]);
	    out.print("&nbsp;");
	    out.print("</TD><TD ALIGN=right>");
	    out.print(item.length());
	    out.print(" bytes&nbsp;</TD><TD>");
	    out.print(dfmt.format(new Date(item.lastModified())));
	    out.print("</TD></TR>\n");
	}
	out.println("</TABLE>");
	out.flush();
    }
}












