// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------
package com.mortbay.Util;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.JarURLConnection;
import java.util.jar.Manifest;


/* ------------------------------------------------------------ */
class JarResource extends Resource
{
    JarURLConnection _jarConnection;
    Manifest _manifest;
    
    /* -------------------------------------------------------- */
    JarResource(URL url)
    {
        super(url,null);
        Code.warning("Not completely implemented nor tested");
    }

    /* ------------------------------------------------------------ */
    protected boolean checkConnection()
    {
        boolean check=super.checkConnection();
        try{
            if (_jarConnection!=_connection)
            {
                _manifest=null;
                _jarConnection=(JarURLConnection)_connection;
                _manifest=_jarConnection.getManifest();
                
                System.err.println("JarConnection="+_connection);
                System.err.println("MANIFEST="+_manifest.getEntries().keySet());
            }
        }
        catch(IOException e)
        {
            Code.ignore(e);
        }
        
        return _manifest!=null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns true if the respresenetd resource exists.
     */
    public boolean exists()
    {
        if (_urlString.endsWith("!/"))
            return checkConnection();
        else
            return super.exists();
    }
    
    
}
