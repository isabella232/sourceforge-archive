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
    String fileBase;
    String indexFile;

    /* ----------------------------------------------------------------- */
    /** Construct a FileHandler at the given fileBase
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
	this.fileBase = fileBase;
	this.indexFile = indexFile;
    }

    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {	
	String filename = request.getRequestPath();
	
	if (filename.endsWith("/"))
	   filename = filename.substring(0,filename.length()-1);

	if (filename.indexOf("..")>=0)
	{
	    Code.warning("Path with .. not handled");
	    return;
	}
	
	filename = fileBase + filename;
	
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
		File index = new File(filename+"/"+indexFile);
		if (index.isFile())
		    sendFile(request,response,index);
		else
		    sendDirectory(request,response,file);
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
		       File file)
	 throws Exception
    {
	Code.debug("sendDirectory: "+file);
	String base = request.getRequestURI();
	if (!base.endsWith("/"))
	    base+="/";
	
	response.setContentType("text/html");

	Page page = new Page("Directory");
	List list = new List(List.Unordered);

	if (base.length()>1)
	    list.add(new Link(base+"..",
			      new Text("..")));
	
	String[] ls = file.list();
	for (int i=0 ; i< ls.length ; i++)
	    list.add(new Link(base+ls[i],ls[i]));
	page.add(list);
	
	page.write(response.getOutputStream());
    }
}


