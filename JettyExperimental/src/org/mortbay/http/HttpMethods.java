/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 07-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferCache;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class HttpMethods extends BufferCache
{
    public final static String GET= "GET",
        POST= "POST",
        HEAD= "HEAD",
        PUT= "PUT",
        OPTIONS= "OPTIONS",
        DELETE= "DELETE",
        TRACE= "TRACE",
        CONNECT= "CONNECT",
        MOVE= "MOVE";

    public final static int GET_ORDINAL= 1,
        POST_ORDINAL= 2,
        HEAD_ORDINAL= 3,
        PUT_ORDINAL= 4,
        OPTIONS_ORDINAL= 5,
        DELETE_ORDINAL= 6,
        TRACE_ORDINAL= 7,
        CONNECT_ORDINAL= 8,
        MOVE_ORDINAL= 9;

    public final static HttpMethods CACHE= new HttpMethods();

    public final static Buffer 
        GET_BUFFER= CACHE.add(GET, GET_ORDINAL),
        POST_BUFFER= CACHE.add(POST, POST_ORDINAL),
        HEAD_BUFFER= CACHE.add(HEAD, HEAD_ORDINAL),
        PUT_BUFFER= CACHE.add(PUT, PUT_ORDINAL),
        OPTIONS_BUFFER= CACHE.add(OPTIONS, OPTIONS_ORDINAL),
        DELETE_BUFFER= CACHE.add(DELETE, DELETE_ORDINAL),
        TRACE_BUFFER= CACHE.add(TRACE, TRACE_ORDINAL),
        CONNECT_BUFFER= CACHE.add(CONNECT, CONNECT_ORDINAL),
        MOVE_BUFFER= CACHE.add(MOVE, MOVE_ORDINAL);

}
