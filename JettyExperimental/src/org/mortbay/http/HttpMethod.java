/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 07-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import org.mortbay.util.io.BufferCache;



/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class HttpMethod extends BufferCache
{
    public final static String 
    	GET= "GET",
        POST= "POST",
        HEAD= "HEAD",
        PUT= "PUT",
        OPTIONS= "OPTIONS",
        DELETE= "DELETE",
        TRACE= "TRACE",
        CONNECT= "CONNECT",
        MOVE= "MOVE";
        
    public final static int
        __GET= 1,
        __POST= 2,
        __HEAD= 3,
        __PUT= 4,
        __OPTIONS= 5,
        __DELETE= 6,
        __TRACE= 7,
        __CONNECT= 8,
        __MOVE= 9;
    
    public final static HttpMethod CACHE = new HttpMethod();
    
    private HttpMethod()
    {
        add(GET,__GET);
        add(POST,__POST);
        add(HEAD,__HEAD);
        add(PUT,__PUT);
        add(OPTIONS,__OPTIONS);
        add(DELETE,__DELETE);
        add(TRACE,__TRACE);
        add(CONNECT,__CONNECT);
        add(MOVE,__MOVE);
    }
}
