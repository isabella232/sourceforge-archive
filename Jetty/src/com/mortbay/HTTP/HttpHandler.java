// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import com.mortbay.Util.LifeCycle;
import java.io.IOException;


/* ------------------------------------------------------------ */
/** HTTP request handler.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public interface HttpHandler extends LifeCycle
{
    /* ------------------------------------------------------------ */
    public String getName();
    
    /* ------------------------------------------------------------ */
    public HandlerContext getHandlerContext();

    /* ------------------------------------------------------------ */
    public void initialize(HandlerContext context);
    
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
    /** Destroy the handler.
     * New requests are refused and all current requests are immediately
     * terminated.
     */
    public void destroy();


    /* ------------------------------------------------------------ */
    /** 
     * @return True if the handler has been started. 
     */
    public boolean isStarted();
    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if the handler has been destroyed. 
     */
    public boolean isDestroyed();
    
    /* ------------------------------------------------------------ */
    /** Handle a request.
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







