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

package org.mortbay.jetty.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.handler.WrappedHandler;
import org.slf4j.LoggerFactory;
import org.slf4j.ULogger;

/* ------------------------------------------------------------ */
/** SessionHandler.
 * @author gregw
 *
 */
public class SessionHandler extends WrappedHandler
{
    private static ULogger log = LoggerFactory.getLogger(SessionHandler.class);

    /* -------------------------------------------------------------- */
    SessionManager _sessionManager;
    

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the sessionManager.
     */
    public SessionManager getSessionManager()
    {
        return _sessionManager;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param sessionManager The sessionManager to set.
     */
    public void setSessionManager(SessionManager sessionManager)
    {
        _sessionManager = sessionManager;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.AbstractLifeCycle#doStart()
     */
    protected void doStart() throws Exception
    {
        if (_sessionManager==null)
            _sessionManager=new HashSessionManager();
        _sessionManager.start();
        super.doStart();
    }
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.AbstractLifeCycle#doStop()
     */
    protected void doStop() throws Exception
    {
        super.doStop();
        _sessionManager.stop();
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public boolean handle(HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException, ServletException
    {
        boolean result=false;
        Request jetty_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
        SessionManager old_session_manager=null;
        HttpSession old_session=null;
        
        try
        {
            String requested_session_id=request.getRequestedSessionId();
            
            if (requested_session_id==null)
            {
                boolean requested_session_id_from_cookie=false;
                
                // Look for session id cookie     
                Cookie[] cookies=request.getCookies();
                if (cookies!=null && cookies.length>0)
                {
                    for (int i=0;i<cookies.length;i++)
                    {
                        if (SessionManager.__SessionCookie.equalsIgnoreCase(cookies[i].getName()))
                        {
                            if (requested_session_id!=null)
                            {
                                // Multiple jsessionid cookies. Probably due to
                                // multiple paths and/or domains. Pick the first
                                // known session or the last defined cookie.
                                if (_sessionManager.getHttpSession(requested_session_id)!=null)
                                    break;
                            }
                            
                            requested_session_id=cookies[i].getValue();
                            requested_session_id_from_cookie = true;
                            if(log.isDebugEnabled())log.debug("Got Session ID "+requested_session_id+" from cookie");
                        }
                    }
                }
                
                if (requested_session_id==null)
                {
                    String uri = request.getRequestURI();
                    int semi = uri.lastIndexOf(';');
                    if (semi>=0)
                    {	
                        String path_params=uri.substring(semi+1);
                        
                        // check if there is a url encoded session param.
                        if (path_params!=null && path_params.startsWith(SessionManager.__SessionURL))
                        {
                            requested_session_id = path_params.substring(SessionManager.__SessionURL.length()+1);
                            if(log.isDebugEnabled())log.debug("Got Session ID "+requested_session_id+" from URL");
                        }
                    }
                }
                
                jetty_request.setRequestedSessionId(requested_session_id);
                jetty_request.setRequestedSessionIdFromCookie(requested_session_id!=null && requested_session_id_from_cookie);
            }
            
            old_session_manager = jetty_request.getSessionManager();
            old_session = jetty_request.getSession(false);
            
            jetty_request.setSessionManager(_sessionManager);
            jetty_request.setSession(null);
            
            HttpSession session=request.getSession(false);
            if (session!=null)
                ((SessionManager.Session)session).access();
            if(log.isDebugEnabled())log.debug("session="+session);
            
            result=super.handle(request,response,dispatch);
        }
        finally
        {
            jetty_request.setSessionManager(old_session_manager);
            jetty_request.setSession(old_session);
        }
        return result;
    }
}
