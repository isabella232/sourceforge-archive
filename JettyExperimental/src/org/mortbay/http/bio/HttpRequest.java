/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 18/02/2004
 * $Id$
 * ============================================== */
 
package org.mortbay.http.bio;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.mortbay.http.HttpHeader;
import org.mortbay.io.Portable;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class HttpRequest extends HttpHeader
{
    HttpInputStream _in;
    HashMap _attributes;
    HttpConnection _connection;
    HttpResponse _response;
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     * 
     */
    public HttpRequest(HttpConnection connection)
    {
        _connection=connection;
    }

    /* ------------------------------------------------------------------------------- */
    /** getInputStream.
     * @return
     */
    public HttpInputStream getHttpInputStream()
    {
        return _in;
    }

    /* ------------------------------------------------------------------------------- */
    /** setInputStream.
     */
    public void setHttpInputStream(HttpInputStream in)
    {
        if (in.getHttpHeader()!=this)
            Portable.throwIllegalState("not stream for request: "+in.getHttpHeader());
        _in=in;;
    }
    

    /* ------------------------------------------------------------ */
    /** Get a request attribute.
     * @param name Attribute name
     * @return Attribute value
     */
    public Object getAttribute(String name)
    {
        if (_attributes==null)
            return null;
        return _attributes.get(name);
    }

    /* ------------------------------------------------------------ */
    /** Set a request attribute.
     * @param name Attribute name
     * @param attribute Attribute value
     * @return Previous Attribute value
     */
    public Object setAttribute(String name, Object attribute)
    {
        if (_attributes==null)
            _attributes=new HashMap(11);
        return _attributes.put(name,attribute);
    }

    /* ------------------------------------------------------------ */
    /** Get Attribute names.
     * @return Enumeration of Strings
     */
    public Set getAttributeNames()
    {
        if (_attributes==null)
            return Collections.EMPTY_SET;
        return _attributes.keySet();
    }

    /* ------------------------------------------------------------ */
    /** Remove a request attribute.
     * @param name Attribute name
     */
    public void removeAttribute(String name)
    {
        if (_attributes!=null)
            _attributes.remove(name);
    }

    /* ------------------------------------------------------------------------------- */
    /** getHttpConnection.
     * @return
     */
    public HttpConnection getHttpConnection()
    {
        return _connection;
    }

    /* ------------------------------------------------------------------------------- */
    public HttpResponse getHttpResponse()
    {
        return _response;
    }

    /* ------------------------------------------------------------------------------- */
    public void destroy()
    {
        super.destroy();
        if(_attributes!=null)
            _attributes.clear();
        _attributes=null;
        if (_in!=null)
            _in.destroy();
        _in=null;
    }
    
}
