// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;
import java.io.IOException;
import org.mortbay.util.LifeCycle;


/* ------------------------------------------------------------ */
/** HTTP handler.
 * The HTTP Handler interface is implemented by classes that wish to
 * receive and handle requests from the HttpServer.  The handle method
 * is called for each request and the handler may ignore, modify or
 * handle the request.
 * Examples of HttpHandler instances include:<UL>
 * <LI>org.mortbay.http.handler.ResourceHandler</LI>
 * <LI>org.mortbay.jetty.servlet.ServletHandler</LI>
 * </UL>
 * @see org.mortbay.http.HttpServer
 * @see org.mortbay.http.HttpContext
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public interface HttpHandler extends LifeCycle
{
    /* ------------------------------------------------------------ */
    public String getName();
    
    /* ------------------------------------------------------------ */
    public HttpContext getHttpContext();

    /* ------------------------------------------------------------ */
    public void initialize(HttpContext context);
    
    /* ------------------------------------------------------------ */
    /** Start the handler.
     * All requests are ignored until start is called.
     */
    public void start() throws Exception;
    
    
    /* ------------------------------------------------------------ */
    /** Stop the handler.
     * New requests are refused and the handler may attempt to wait
     * for existing requests to complete. The caller may interrupt
     * the stop call is waiting is taking too long.
     */
    public void stop()
        throws InterruptedException;
    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if the handler has been started. 
     */
    public boolean isStarted();
    
    /* ------------------------------------------------------------ */
    /** Handle a request.
     *
     * @param pathInContext The context path
     * @param pathParams Path parameters such as encoded Session ID
     * @param request The request
     * @param response The response.
     */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException;
}







