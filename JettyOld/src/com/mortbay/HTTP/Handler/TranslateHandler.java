// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;


/* --------------------------------------------------------------------- */
/** Request path translation handler
 * <p>This Handler can translate a prefix of a request paths to another
 * path.
 * <p>
 * The Translate handler is configured with PathMap mapping from the
 * old path to the new path. Translations are applied, longest match
 * first, until no more translations match.
 *
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class TranslateHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    PathMap translations;
    
    /* ----------------------------------------------------------------- */
    public TranslateHandler(PathMap translations)
    {
	this.translations=translations;
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {
	String address = request.getResourcePath();
	
	String path=translations.longestMatch(address);
	if (path != null)
	{
	    String translation = (String)translations.get(path);
	    Code.debug("Translation from "+path+
		       " to "+translation);
		
	    request.translateAddress(path,translation);

	    // recurse for more translations
	    handle(request,response);
	}
    }
    
    /* ----------------------------------------------------------------- */
    public String translate(String address)
    {
	String path=translations.longestMatch(address);
	if (path != null)
	{
	    String translation = (String)translations.get(path);
	    Code.debug("Translation from "+path+
		       " to "+translation);

	    address = PathMap.translate(address,path,translation);
	    
	    // recurse for more translations
	    address = translate(address);
	}
	return address;
    }
}






