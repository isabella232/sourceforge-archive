/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 08-Apr-2003
 * $Id$
 * ============================================== */
 
package org.mortbay.http;

import org.mortbay.io.BufferCache;

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
		__HTTP_0_9=9,
		__HTTP_1_0=10,
		__HTTP_1_1=11;
	
	public final static HttpVersion CACHE = new HttpVersion();
	
	private HttpVersion()
	{
		add(HTTP_0_9,__HTTP_0_9);
		add(HTTP_1_0,__HTTP_1_0);
		add(HTTP_1_1,__HTTP_1_1);
	}
}
