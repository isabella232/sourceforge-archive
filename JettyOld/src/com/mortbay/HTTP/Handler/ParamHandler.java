// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import java.io.*;
import java.util.*;


/* --------------------------------------------------------------------- */
/** Parameter Handler
 * Moves form content to request parameters
 */
public class ParamHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    public boolean includeCookiesAsParameters=true;

    /* ----------------------------------------------------------------- */
    public ParamHandler()
    {}
    
    /* ----------------------------------------------------------------- */
    public ParamHandler(boolean includeCookiesAsParameters)
    {
	this.includeCookiesAsParameters=includeCookiesAsParameters;
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws IOException
    {
	Code.debug("ParamHandler");
	request.decodeFormParameters();
	if (includeCookiesAsParameters)
	    request.decodeCookieParameters();
    }
}





