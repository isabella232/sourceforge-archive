/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
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
public class HttpHeader extends BufferCache
{
	public final static String
		CONTENT_LENGTH="Content-Length";
		
	public final static int
		CONTENT_LENGTH_INDEX=1;

	public final static HttpHeader CACHE = new HttpHeader();
	    
	private HttpHeader()
	{
		add(CONTENT_LENGTH,CONTENT_LENGTH_INDEX);
	}	
		
}
