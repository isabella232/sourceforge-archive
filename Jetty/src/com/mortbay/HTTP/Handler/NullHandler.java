// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.HttpException;
import com.mortbay.HTTP.HttpHandler;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.HttpResponse;
import com.mortbay.Util.Code;
import java.io.IOException;

/* ------------------------------------------------------------ */
/** Null HTTP Handler
 * This No-op handler is a good base for other handlers
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
abstract public class NullHandler implements HttpHandler
{
    /* ----------------------------------------------------------------- */
    private boolean _started=false;
    private boolean _destroyed=true;
    private HandlerContext _context;
    private String _name;
    
    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public HandlerContext getHandlerContext()
    {
        return _context;
    }
    
    /* ------------------------------------------------------------ */
    /** Initialize with a HandlerContext.
     * The first time it is called, initialize() is called after setting the context.
     * @param configuration Must be the HandlerContext of the handler
     */
    public final void initialize(Object configuration)
    {
        if (_context==null)
        {
            _context=(HandlerContext) configuration;
            initialize();
        }
        else if (_context!=configuration)
            throw new IllegalStateException("Can't initialize handler for different context");
    }
    
    
    /* ------------------------------------------------------------ */
    public void initialize()
    {
    }
    
    /* ----------------------------------------------------------------- */
    public void start()
    {
        _started=true;
        _destroyed=false;
    }
    
    /* ----------------------------------------------------------------- */
    public void stop()
    {
        _started=false;
    }
    
    /* ----------------------------------------------------------------- */
    public void destroy()
    {
        _started=false;
        _destroyed=true;
        _context=null;
    }

    /* ----------------------------------------------------------------- */
    public boolean isStarted()
    {
        return _started;
    }
    
    /* ----------------------------------------------------------------- */
    public boolean isDestroyed()
    {
        return _destroyed;
    }
    
    
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        if(_name==null)
        {
            _name=this.getClass().getName();
            if (!Code.debug())
            {
                int i=_name.lastIndexOf(".");
                _name=_name.substring(i+1);
            }
            _name+=" in "+_context;
        }
        return _name;
    }    

}




