// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.HttpException;
import com.mortbay.HTTP.HttpHandler;
import com.mortbay.HTTP.HttpMessage;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.HttpResponse;
import com.mortbay.HTTP.PathMap;
import com.mortbay.Util.UrlEncoded;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import java.io.IOException;
import java.util.Map;


/* ------------------------------------------------------------ */
/** Forward Request Handler.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ForwardHandler extends NullHandler
{
    PathMap _forward = new PathMap();
    String _root;
    boolean _handleQueries = false;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ForwardHandler()
    {}
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param rootForward 
     */
    public ForwardHandler(String rootForward)
    {
        _root=rootForward;
    }
    
    /* ------------------------------------------------------------ */
    /** Add a forward mapping.
     * @param pathSpecInContext The path to forward from 
     * @param newPath The path to forward to.
     */
    public void addForward(String pathSpecInContext,
                           String newPath)
    {
        _forward.put(pathSpecInContext,newPath);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a forward mapping for root path.
     * @param newPath The path to forward to.
     */
    public void setRootForward(String newPath)
    {
        _root=newPath;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the Handler up to cope with forwards to paths that contain query
     * elements (e.g. "/blah"->"/foo?a=b").
     * @param b 
     */
    public void setHandleQueries(boolean b)
    {
        _handleQueries = b;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param pathInContext 
     * @param request 
     * @param response 
     * @exception HttpException 
     * @exception IOException 
     */
    public void handle(String pathInContext,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        String newPath=null;
        String query=null;
        if (_root!=null && "/".equals(pathInContext))
            newPath=_root;
        else
        {
            Map.Entry entry = _forward.getMatch(pathInContext);
            if (entry!=null)
            {
                String match = (String)entry.getValue();
                if (_handleQueries)
                {
                    int hook = match.indexOf('?');
                    if (hook != -1){
                        query = match.substring(hook+1);
                        match = match.substring(0, hook);
                    }
                }
                String info=PathMap.pathInfo((String)entry.getKey(),pathInContext);
                Code.debug("Forward: match:\"", match, "\" info:",
                           info, "\" query:", query);
                newPath=match+(info==null?"":info);
            }
        }
        
        if (newPath!=null)
        {
            Code.debug("Forward from ",pathInContext," to ",newPath);
            
            int last=request.setState(HttpMessage.__MSG_EDITABLE);
            String context=getHandlerContext().getContextPath();
            if (context.length()==1)
                request.setPath(newPath);
            else
                request.setPath(getHandlerContext().getContextPath()+newPath);
            if (_handleQueries && query != null){
                // add forwarded to query string to parameters
                UrlEncoded.decodeTo(query, request.getParameters());
            }
            request.setState(last);
            getHandlerContext().getHttpServer().service(request,response);
            return;
        }
    }
}
