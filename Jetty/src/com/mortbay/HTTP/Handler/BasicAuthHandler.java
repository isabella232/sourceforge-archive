// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.HTTP.HttpException;
import com.mortbay.HTTP.HttpFields;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.HttpResponse;
import com.mortbay.Util.B64Code;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.StringUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/* ------------------------------------------------------------ */
/** Basic Authentication Handler.
 * @deprecated Use SecurityHandler.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class BasicAuthHandler extends NullHandler
{
    /* ------------------------------------------------------------ */
    private String _name;
    private Map _user2password;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @deprecated Use SecurityHandler.
     * @param name 
     * @param user2password 
     */
    public BasicAuthHandler()
    {
        _name="Test Basic Authentication";
        _user2password=new HashMap(11);
        _user2password.put("jetty","jetty");
        Log.event("TEST BasicAuthHandler");
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param name 
     * @param user2password 
     */
    public BasicAuthHandler(String name, Map user2password)
    {
        _name=name;
        _user2password=user2password;
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        if (!isStarted())
            return;        

        String credentials =
            request.getField(HttpFields.__Authorization);
        
        if (credentials!=null)
        {
            Code.debug("Credentials: "+credentials);
            credentials =
                credentials.substring(credentials.indexOf(' ')+1);
            credentials = B64Code.decode(credentials,StringUtil.__ISO_8859_1);
            int i = credentials.indexOf(':');
            String user = credentials.substring(0,i);
            String password = credentials.substring(i+1);
            
            request.setAttribute(HttpRequest.__AuthUser,user);
            request.setAttribute(HttpRequest.__AuthType,"BASIC");
            
            String realPassword=(String)_user2password.get(user);
            if (realPassword!=null && realPassword.equals(password))
                return;
            
            if (Code.debug())
                Code.warning("'"+realPassword+"'!='"+password+"'");
        }
        
        Code.debug("Unauthorized in "+_name);
        
        response.setStatus(HttpResponse.__401_Unauthorized);
        response.setField(HttpFields.__WwwAuthenticate,
                          "basic realm=\""+_name+'"');
        response.commit();
        request.setHandled(true);
    }
}
