// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.util.Code;
import org.mortbay.util.Log;

/* ------------------------------------------------------------ */
/** Base HTTP Handler.
 * This No-op handler is a good base for other handlers
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
abstract public class NullHandler implements HttpHandler
{
    /* ----------------------------------------------------------------- */
    private boolean _started=false;
    private HttpContext _context;
    private String _name;

    /* ------------------------------------------------------------ */
    public void setName(String name)
    {
        _name=name;
    }
    
    /* ------------------------------------------------------------ */
    public String getName()
    {
        if (_name==null)
        {
            _name=this.getClass().getName();
            if (!Code.debug())
                _name=_name.substring(_name.lastIndexOf('.')+1);
        }
        return _name;
    }
    
    
    /* ------------------------------------------------------------ */
    public HttpContext getHttpContext()
    {
        return _context;
    }
    
    /* ------------------------------------------------------------ */
    /** Initialize with a HttpContext.
     * @param configuration Must be the HttpContext of the handler
     */
    public void initialize(HttpContext context)
    {
        if (_context==null)
            _context=context;
        else if (_context!=context)
            throw new IllegalStateException("Can't initialize handler for different context");
    }
    
    /* ----------------------------------------------------------------- */
    public void start()
        throws Exception
    {
        if (_context==null)
            throw new IllegalStateException("No context for "+this);        
        if (!_context.isStarted())
            Code.warning("Handler Context not started for "+this);
        
        _started=true;
        Log.event("Started "+this);
    }
    
    /* ----------------------------------------------------------------- */
    public void stop()
        throws InterruptedException
    {
        _started=false;
        Log.event("Stopped "+this);
    }

    /* ----------------------------------------------------------------- */
    public boolean isStarted()
    {
        return _started;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return getName()+" in "+_context;
    }    

}




