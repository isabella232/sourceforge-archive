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
public class Method extends BufferCache
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
	    GET_INDEX= 1,
	    POST_INDEX= 2,
	    HEAD_INDEX= 3,
	    PUT_INDEX= 4,
	    OPTIONS_INDEX= 5,
	    DELETE_INDEX= 6,
	    TRACE_INDEX= 7,
	    CONNECT_INDEX= 8,
	    MOVE_INDEX= 9;
	    
	public final static Method CACHE = new Method();
	    
	private Method()
	{
		add(GET,GET_INDEX);
		add(POST,POST_INDEX);
		add(HEAD,HEAD_INDEX);
		add(PUT,PUT_INDEX);
		add(OPTIONS,OPTIONS_INDEX);
		add(DELETE,DELETE_INDEX);
		add(TRACE,TRACE_INDEX);
		add(CONNECT,CONNECT_INDEX);
		add(MOVE,MOVE_INDEX);
	}
	
}
