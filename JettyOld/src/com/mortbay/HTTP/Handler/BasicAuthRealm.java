// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import java.util.*;
import java.io.*;

/* --------------------------------------------------------------------- */
/** Basic Authentication realm.<p>
 * Instances of this class represent a named basic authentication realm.
 * BasicAuthRealm extends a Hashtable which maps usernames to passwords.
 * @see com.mortbay.HTTP.Handler.BasicAuthHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class BasicAuthRealm extends Properties
{
    /* ----------------------------------------------------------------- */
    private String name =null;
    
    /* ----------------------------------------------------------------- */
    /** Construct realm from properties
     * @param name The name of the realm
     * @param properties Map from username to password
     * @exception IOException 
     */
    public BasicAuthRealm(String name,Properties properties)
        throws IOException
    {
        this.name=name;
        Enumeration e=properties.keys();
        while(e.hasMoreElements())
        {
            Object k=e.nextElement();
            put(k,properties.get(k));
        }
        Code.debug(this);
    }
    
    /* ----------------------------------------------------------------- */
    /** Construct realm from property file
     * @param name The name of the realm
     */
    public BasicAuthRealm(String name,String filename)
        throws IOException
    {
        this.name=name;
        load(new FileInputStream(filename));
    }
    
    /* ----------------------------------------------------------------- */
    /** Construct empty realm
     * @param name The name of the realm
     */
    public BasicAuthRealm(String name)
    {
        this.name=name;
    }    
    
    /* ----------------------------------------------------------------- */
    public String name()
    {
        return name;
    }
}


