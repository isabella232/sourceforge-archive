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
        if (_root!=null && "/".equals(pathInContext))
            newPath=_root;
        else
        {
            Map.Entry entry = _forward.getMatch(pathInContext);
            if (entry!=null)
            {
                String info=PathMap.pathInfo((String)entry.getKey(),pathInContext);
                newPath=(String)entry.getValue()+(info==null?"":info);
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
            request.setState(last);
            getHandlerContext().getHttpServer().service(request,response);
            return;
        }
    }
}
