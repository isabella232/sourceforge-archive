// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.http.bio;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.mortbay.http.temp.HttpHeader;
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
