/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 08-Apr-2003
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
public class HttpVersion extends BufferCache
{
	public final static String
		HTTP_0_9 = "",
		HTTP_1_0 = "HTTP/1.0",
		HTTP_1_1 = "HTTP/1.1";
		
	public final static int
		HTTP_0_9_INDEX=9,
		HTTP_1_0_INDEX=10,
		HTTP_1_1_INDEX=11;
	
	public final static HttpVersion CACHE = new HttpVersion();
	
	private HttpVersion()
	{
		add(HTTP_0_9,HTTP_0_9_INDEX);
		add(HTTP_1_0,HTTP_1_0_INDEX);
		add(HTTP_1_1,HTTP_1_1_INDEX);
	}
}
