// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Filter;
import com.mortbay.HTTP.HTML.*;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.reflect.*;

/* --------------------------------------------------------------------- */
/** Filter Html and expire content.
 * When activated, this extension of the HtmlFilter modifies the
 * response header to prevent any caching of the the response.
 *
 * This filter should be used instead of HtmlFilter if the tags
 * expansions may vary over time, and thus invalidate the
 * expiry time of the raw content.
 *
 *
 * @version 1.0 Thu Jun 22 2000
 * @author Greg Wilkins (gregw)
 */
public class HtmlExpireFilter extends HtmlFilter
{
    protected void activate()
    {
        if(response!=null)
        { 
            long now = System.currentTimeMillis();
            response.setDateHeader(HttpHeader.LastModified, now);
            response.setDateHeader(HttpHeader.Expires, 0);
            response.setHeader(HttpHeader.ContentLength, null);
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache,no-store");
	}
	super.activate();
    }
}

