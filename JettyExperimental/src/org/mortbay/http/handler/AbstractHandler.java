//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.http.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ugli.LoggerFactory;
import org.apache.ugli.ULogger;
import org.mortbay.http.HttpConnection;
import org.mortbay.http.HttpHandler;
import org.mortbay.thread.AbstractLifeCycle;


/* ------------------------------------------------------------ */
/** AbstractHandler.
 * @author gregw
 *
 */
public class AbstractHandler extends AbstractLifeCycle implements HttpHandler
{
    private static ULogger log = LoggerFactory.getLogger(HttpConnection.class);
    
    private HttpHandler _next;

    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public AbstractHandler()
    {
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.http.HttpHandler#setNextHttpHandler(org.mortbay.http.HttpHandler)
     */
    public void setNextHttpHandler(HttpHandler next)
    {
        _next=next;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.http.HttpHandler#getNextHttpHandler()
     */
    public HttpHandler getNextHttpHandler()
    {
        return _next;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.http.HttpHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
        log.debug("{} handling {}",this,request);
        if (_next!=null)
            _next.handle(request,response);
        log.debug("{} handled  {}",this,response);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.LifeCycle#start()
     */
    public void doStart() throws Exception
    {
        log.info("start {}",this);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.LifeCycle#stop()
     */
    public void doStop() throws Exception
    {
        log.info("stop {}",this);
    }

}
