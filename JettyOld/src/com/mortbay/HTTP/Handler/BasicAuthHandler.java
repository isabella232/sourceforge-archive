// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.PropertyTree;
import java.util.*;
import java.io.*;


/* --------------------------------------------------------------------- */
/** Basic Authentication HttpHandler
 * <p>
 * If the request is for a path with basic authentication, then the
 * request is examined for either existing authentication (in which case
 * the remoteUser is set and the request passed through ) or a basic
 * authentication challenge is sent.
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
     * @param properties Passed to setProperties
     */
    public BasicAuthHandler(Properties properties)
	throws IOException
    {
	setProperties(properties);
    }
    
    /* ----------------------------------------------------------------- */
    /** Construct basic auth handler.
     * @param realms PathMap of path to BasicAuthRealm instances which
     * map username to password.
     */
    public BasicAuthHandler(PathMap realms)
    {
	this.realms = realms;
    }

    /* ------------------------------------------------------------ */
    /** Configure from Properties.
     * Properties are assumed to be in the format of a PropertyTree
     * like:<PRE>
     * name.LABEL : The realm label
     * name.PATHS : /pathMap/entry;/list
     * name.PROPERTIES : fileNameOfLoginPasswordMapping
     * name.PROPERTY.login : password
     *</PRE>
     * @param properties Configuration.
     */
    public void setProperties(Properties properties)
	throws IOException
    {
	PropertyTree tree=null;
	if (properties instanceof PropertyTree)
	    tree = (PropertyTree)properties;
	else
	    tree = new PropertyTree(properties);
	Code.debug(tree);

	realms = new PathMap();

	Enumeration names = tree.getRealNodes();
	while (names.hasMoreElements())
	{
	    String realmName = names.nextElement().toString();
	    if ("*".equals(realmName))
		continue;

	    Code.debug("Configuring realm "+realmName);
	    PropertyTree realmTree = tree.getTree(realmName);
	    Properties realmMap = getProperties(realmTree);
	    BasicAuthRealm realm =
		new BasicAuthRealm(realmTree.getProperty("LABEL"),
				   realmMap);
	    Vector paths = realmTree.getVector("PATHS",",;");
	    for (int r=paths.size();r-->0;)
		realms.put(paths.elementAt(r),realm);
	}

	Code.debug(realms);
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
	    Code.debug("Authenticate in Realm "+realm.name());

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

		if (Code.debug())
		    Code.warning("'"+realPassword+"'!='"+password+"'");
	    }
	    
	    Code.debug("Unauthorized in "+realm.name());

	    response.setStatus(HttpResponse.SC_UNAUTHORIZED);
	    response.setHeader(HttpHeader.WwwAuthenticate,
			       "basic realm=\""+realm.name()+'"');
	    response.writeHeaders();	    
	}
    }
}


