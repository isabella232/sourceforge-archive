// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.util.*;
import java.text.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Null HTTP Handler
 * This No-op handler is a good base for other handlers
 *
 * @version 1.0 Mon Oct 11 1999
 * @author Greg Wilkins (gregw)
 */
abstract public class NullHandler implements HttpHandler
{
    /* ----------------------------------------------------------------- */
    protected boolean _started=false;
    protected boolean _destroyed=true;
    
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
    public void handle(String pathSpec,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
    }
    
}




