// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Base.*;
import java.io.*;
import java.util.*;


/* --------------------------------------------------------------------- */
/** Http Filter
 * Derived Http Filters may be added into HttpOutputStreams.
 * A HttpOutputStream
 * will check the content types handled by a filter and if pass all
 * output through the filter if it can handle the type of the HTTP response.
 * The FilterHandler HttpHandler can be used to insert HttpFilters
 * into HttpResponses.
 * <p>
 * ActivateOn is called after any HTTP/1.1 chunked output is added
 * to the HttpOutputStream, so filters always receive pre-chunked data.
 * @version $Id$
 * @author Greg Wilkins
 */
public class HttpFilter extends FilterOutputStream
    implements Observer
{
    /* ----------------------------------------------------------------- */
    protected HttpRequest request=null;
    protected HttpResponse response=null;
    
    /* ----------------------------------------------------------------- */
    public HttpFilter()
    {
	super(System.err);
    }
    
    /* ----------------------------------------------------------------- */
    public HttpFilter(HttpRequest request)
    {
	super(System.err);
	this.request=request;
	if(request!=null)
	    this.response=request.getHttpResponse();
    }
    
    /* ----------------------------------------------------------------- */
    /** This method must be implemented by the derived class to indicate
     * the content types handled by the filter. The default implementation
     * returns true for text content types.
     * Called by activate.
     */
    protected boolean canHandle(String contentType)
    {
	return contentType.startsWith("text/");
    }
    
    /* ----------------------------------------------------------------- */
    /** Activate is called on a filter by a HttpResponse, normally just
     * before the response headers are written. If canHandle() returns
     * true, the filter is inserted into the HttpResponse output stream and
     * a thread started to run the filter.
     */
    public final void activateOn(HttpResponse response) 
	throws IOException
    {
	if (canHandle(response.getHeader(HttpHeader.ContentType)))
	{
	    Code.debug("Activate HttpFilter "+this); 
	    this.request=response.getRequest();
	    this.response=response;
	    HttpOutputStream httpOut = response.getHttpOutputStream();
	    super.out = httpOut.replaceOutputStream(this);
	    activate();
	}
	else
	    Code.debug("Can't activate HttpFilter "+this); 
    }

    /* ------------------------------------------------------------ */
    /** Set output stream for testing purposes. 
     * @param out OutputStream
     */
    public void test(OutputStream out)
    {
	super.out=out;
    }
    
    /* ----------------------------------------------------------------- */
    /** Notify derived class of activation
     */
    protected void activate()
    {}
    
    /* ----------------------------------------------------------------- */
    /** Observer update.
     * This method is called as part of the Observer pattern to notify
     * the filter that headers are about to be written. The call is
     * delegated to activateOn(HttpResponse).
     * @param o ignored
     * @param arg The HttpResponse.
     */
    public void update(Observable o, Object arg)
    {
	try{
	    activateOn((HttpResponse)arg);
	}
	catch(IOException e){
	    Code.debug("Convert to RuntimeException",e);
	    throw new RuntimeException(e.toString());
	}
    }
}





