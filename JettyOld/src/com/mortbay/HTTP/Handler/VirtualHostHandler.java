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
import javax.servlet.http.*;
import javax.servlet.*;


/* --------------------------------------------------------------------- */
/** Request path translation handler
 * Modified translate handler for virtual host translations.
 * 
 * Usage:
 * main.root.Vhost.PROPERTY.//ahma.dhs.org%  : /ahma
 *
 *  Most accesses go to the server itself, but if server is accessed with
 *  http://ahma.dhs.org/ the accesses are actually mapped to /ahma/.
 *
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Marko P. O. Nippula <mnippula@cc.hut.fi>
 */
public class VirtualHostHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    PathMap translations;
    boolean _translateURI=false;
    
    /* ----------------------------------------------------------------- */
    /** Construct basic auth handler.
     * @param properties Passed to setProperties
     */
    public VirtualHostHandler(Properties properties)
        throws IOException
    {
        setProperties(properties);
    }
    
    /* ----------------------------------------------------------------- */
    public VirtualHostHandler(PathMap translations)
    {
        this.translations=translations;
    }
    
    /* ------------------------------------------------------------ */
    /** Configure from Properties.
     * @param properties 
     * @exception IOException 
     */
    public void setProperties(Properties properties)
        throws IOException
    {
        PropertyTree tree=null;
        if (properties instanceof PropertyTree)
            tree = (PropertyTree)properties;
        else
            tree = new PropertyTree(properties);
        
        _translateURI=tree.getBoolean("TranslateURI");
        translations=new PathMap(tree);
        translations.remove("TranslateURI");
        Code.debug(translations);
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
                       HttpResponse response)
         throws Exception
    {   
	String address;
        address = "//" + request.getServerName() + request.getResourcePath();

        String path=translations.matchSpec(address);
        if (path != null)
        {
            String translation = (String)translations.get(path);
            Code.debug("Translation from "+path+
                       " to "+translation);

	    path = path.substring(request.getServerName().length()+2);
	    if (path.equals("%")) path = "";

            request.translateAddress(path,translation,_translateURI);

            address = request.getResourcePath();
        }
    }
    
    /* ----------------------------------------------------------------- */
    public String translate(String address)
    {
        String path=translations.matchSpec(address);
        if (path != null)
        {
            String translation = (String)translations.get(path);
            Code.debug("Translation from "+path+
                       " to "+translation);

            address = PathMap.translate(address,path,translation);
        }
        return address;
    }
}
