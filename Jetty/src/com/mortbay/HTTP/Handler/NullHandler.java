// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;
//import com.sun.java.util.collections.*; XXX-JDK1.1

import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.util.*;
import java.text.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Null HTTP Handler
 * This No-op handler is a good base for other handlers
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class NullHandler implements HttpHandler
{
    /* ----------------------------------------------------------------- */
    private boolean _started=false;
    private boolean _destroyed=true;
    private HandlerContext _context;
    
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
    
    /* ----------------------------------------------------------------- */
    public String realPath(String pathSpec, String path)
    {
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String contextPathSpec,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
	Code.warning("NullHandler called for "+contextPathSpec);
    }
    
}




