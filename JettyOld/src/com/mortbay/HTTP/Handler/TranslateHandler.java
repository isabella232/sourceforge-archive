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
 * <p>This Handler can translate a prefix of a request paths to another
 * path.
 * <p>
 * The Translate handler is configured with PathMap mapping from the
 * old path to the new path. Translations are applied, longest match
 * first, until no more translations match.
 *
 * If the PathMap key "TranslateURI" is set to a value of true, then
 * translations are reflected in the requests URI.
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class TranslateHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    PathMap translations;
    boolean _translateURI=false;
    
    /* ----------------------------------------------------------------- */
    /** Construct basic auth handler.
     * @param properties Passed to setProperties
     */
    public TranslateHandler(Properties properties)
        throws IOException
    {
        setProperties(properties);
    }
    
    /* ----------------------------------------------------------------- */
    public TranslateHandler(PathMap translations)
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
        String address = request.getResourcePath();
        
        String path=translations.matchSpec(address);
        while (path != null)
        {
            String translation = (String)translations.get(path);
            Code.debug("Translation from "+path+
                       " to "+translation);
                
            request.translateAddress(path,translation,_translateURI);

            address = request.getResourcePath();
            path=translations.matchSpec(address);
        }
    }
    
    /* ----------------------------------------------------------------- */
    public String translate(String address)
    {
        String path=translations.matchSpec(address);
        while (path != null)
        {
            String translation = (String)translations.get(path);
            Code.debug("Translation from "+path+
                       " to "+translation);

            address = PathMap.translate(address,path,translation);
            path=translations.matchSpec(address);
        }
        return address;
    }
}
