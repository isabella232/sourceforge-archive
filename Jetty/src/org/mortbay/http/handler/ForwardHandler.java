// ========================================================================
// Copyright (c) 1999-2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.PathMap;
import org.mortbay.util.URI;
import org.mortbay.util.UrlEncoded;
import org.mortbay.util.LogSupport;


/* ------------------------------------------------------------ */
/** Forward Request Handler.
 * Forwards a request to a new URI. Experimental - use with caution.
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ForwardHandler extends AbstractHttpHandler
{
    private static Log log = LogFactory.getLog(ForwardHandler.class);

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
     * This allows a forward for exactly / which is the default
     * path in a pathSpec.
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
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        if(LogSupport.isTraceEnabled(log))log.trace("Look for "+pathInContext+" in "+_forward);
        
        String newPath=null;
        String query=null;
        if (_root!=null && ("/".equals(pathInContext) || pathInContext.startsWith("/;")))
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
                if(log.isDebugEnabled())log.debug("Forward: match:\""+ match+ "\" info:"+info+"\" query:"+query);
                newPath=info==null?match:(URI.addPaths(match,info));
            }
        }
        
        if (newPath!=null)
        {
            if(log.isDebugEnabled())log.debug("Forward from "+pathInContext+" to "+newPath);
            
            int last=request.setState(HttpMessage.__MSG_EDITABLE);
            String context=getHttpContext().getContextPath();
            if (context.length()==1)
                request.setPath(newPath);
            else
                request.setPath(URI.addPaths(context,newPath));
            if (_handleQueries && query != null){
                // add forwarded to query string to parameters
                UrlEncoded.decodeTo(query, request.getParameters());
            }
            request.setState(last);
            getHttpContext().getHttpServer().service(request,response);
            return;
        }
    }
}
