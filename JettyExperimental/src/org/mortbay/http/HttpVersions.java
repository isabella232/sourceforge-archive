/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 08-Apr-2003
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
public class HttpVersions extends BufferCache
{
	public final static String
		HTTP_0_9 = "",
		HTTP_1_0 = "HTTP/1.0",
		HTTP_1_1 = "HTTP/1.1";
		
	public final static int
		HTTP_0_9_ORDINAL=9,
		HTTP_1_0_ORDINAL=10,
		HTTP_1_1_ORDINAL=11;
	
	public final static HttpVersions CACHE = new HttpVersions();
	
    public final static Buffer 
        HTTP_0_9_BUFFER=CACHE.add(HTTP_0_9,HTTP_0_9_ORDINAL),
        HTTP_1_0_BUFFER=CACHE.add(HTTP_1_0,HTTP_1_0_ORDINAL),
        HTTP_1_1_BUFFER=CACHE.add(HTTP_1_1,HTTP_1_1_ORDINAL);
}
