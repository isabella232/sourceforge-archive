// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.OutputStream;
import org.mortbay.http.ChunkableOutputStream;
import org.mortbay.util.Code;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.util.Log;
import org.mortbay.util.StringUtil;

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
     * Called by addHandler methods of HttpContext.
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
    public void handleTrace(HttpRequest request,
                        HttpResponse response)
        throws IOException
    {
        // Handle TRACE by returning request header
        response.setField(HttpFields.__ContentType,
                          HttpFields.__MessageHttp);
        OutputStream out = response.getOutputStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream(2048);
        Writer writer = new OutputStreamWriter(buf,StringUtil.__ISO_8859_1);
        writer.write(request.toString());
        writer.flush();
        response.setIntField(HttpFields.__ContentLength,buf.size());
        buf.writeTo(out);
        request.setHandled(true);
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




