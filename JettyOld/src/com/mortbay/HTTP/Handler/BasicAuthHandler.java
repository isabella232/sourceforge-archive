// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import java.util.*;
import java.io.*;


/* --------------------------------------------------------------------- */
/** Basic Authentication HttpHandler
 * <p>
 * If the request is for a path with basic authentication, then the
 * request is examined for either existing authentication (in which case
 * the remoteUser is set and the request passed throug ) or a basic
 * authenication challenge is sent.
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class BasicAuthHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    private PathMap realms;
    
    /* ----------------------------------------------------------------- */
    /** Construct basic auth handler.
     * @param realms PathMap of path to BasicAuthRealm instances which
     * map username to password.
     */
    public BasicAuthHandler(PathMap realms)
    {
	this.realms = realms;
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {
	String address = request.getPathInfo();
	
	String path=realms.longestMatch(address);
	
	if (path != null)
	{
	    BasicAuthRealm realm = (BasicAuthRealm) realms.get(path);
	    Code.debug("Authenicate in Realm "+realm.name());

	    String credentials =
		request.getHeader(HttpHeader.Authorization);

	    if (credentials!=null)
	    {
		Code.debug("Credentials: "+credentials);
		credentials =
		    credentials.substring(credentials.indexOf(' ')+1);
		credentials = B64Code.decode(credentials);
		int i = credentials.indexOf(':');
		String user = credentials.substring(0,i);
		String password = credentials.substring(i+1);
		request.setRemoteUser("Basic",user);

		String realPassword=(String)realm.get(user);
		if (realPassword!=null && realPassword.equals(password))
		    return;
	    }
	    
	    Code.debug("Unauthorized in "+realm.name());

	    response.setStatus(HttpResponse.SC_UNAUTHORIZED);
	    response.setHeader(HttpHeader.WwwAuthenticate,
			       "basic realm=\""+realm.name()+'"');
	    response.writeHeaders();	    
	}
    }
}


