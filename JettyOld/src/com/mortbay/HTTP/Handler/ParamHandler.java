// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.PropertyTree;
import java.io.*;
import java.util.*;


/* --------------------------------------------------------------------- */
/** Parameter Handler
 * Moves form content to request parameters
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class ParamHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    public boolean includeCookiesAsParameters=true;

    /* ------------------------------------------------------------ */
    /** Constructor from properties.
     * Calls setProperties.
     * @param properties Configuration properties
     */
    public ParamHandler(Properties properties)
    {
        setProperties(properties);
    }
    
    /* ----------------------------------------------------------------- */
    public ParamHandler()
    {}
    
    /* ------------------------------------------------------------ */
    /** Configure from properties.
     * The configuration keys are:<PRE>
     * CookiesAsParameters - boolean, if true include cookies as request params.
     * </PRE>
     * @param properties configuration.
     */
    public void setProperties(Properties properties)
    {
        PropertyTree tree=null;
        if (properties instanceof PropertyTree)
            tree = (PropertyTree)properties;
        else
            tree = new PropertyTree(properties);

        includeCookiesAsParameters=tree.getBoolean("CookiesAsParameters");
    }
    
    /* ----------------------------------------------------------------- */
    public ParamHandler(boolean cookies)
    {
        this.includeCookiesAsParameters=cookies;
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
                       HttpResponse response)
         throws IOException
    {
        Code.debug("ParamHandler");
        request.decodeFormParameters();
        if (includeCookiesAsParameters)
            request.cookiesAsParameters();
    }
}





