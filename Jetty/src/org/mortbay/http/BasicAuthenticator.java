// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.io.IOException;
import java.security.Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.SecurityConstraint.Authenticator;
import org.mortbay.util.B64Code;
import org.mortbay.util.LogSupport;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** BASIC authentication.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class BasicAuthenticator implements Authenticator
{
    private static Log log = LogFactory.getLog(BasicAuthenticator.class);

    /* ------------------------------------------------------------ */
    /** 
     * @return UserPrinciple if authenticated or null if not. If
     * Authentication fails, then the authenticator may have committed
     * the response as an auth challenge or redirect.
     * @exception IOException 
     */
    public Principal authenticate(UserRealm realm,
                                  String pathInContext,
                                  HttpRequest request,
                                  HttpResponse response,
                                  boolean check)
        throws IOException
    {
        // Get the user if we can
        Principal user=null;
        String credentials = request.getField(HttpFields.__Authorization);
        
        if (credentials!=null )
        {
            try
            {
                if(log.isDebugEnabled())log.debug("Credentials: "+credentials);
                credentials = credentials.substring(credentials.indexOf(' ')+1);
                credentials = B64Code.decode(credentials,StringUtil.__ISO_8859_1);
                int i = credentials.indexOf(':');
                String username = credentials.substring(0,i);

                if (check)
                {
                    String password = credentials.substring(i+1);
                    user = realm.authenticate(username,password,request);
                }
                else
                    user = realm.getUserPrincipal(username);
                
                if (user!=null)
                {
                    request.setAuthType(SecurityConstraint.__BASIC_AUTH);
                    request.setAuthUser(username);
                    request.setUserPrincipal(user);                
                }
                else if (check)
                    log.warn("AUTH FAILURE: user "+username);
            }
            catch (Exception e)
            {
                log.warn("AUTH FAILURE: "+e.toString());
                log.trace(LogSupport.IGNORED,e);
            }
        }

        // Challenge if we have no user
        if (user==null)
            sendChallenge(realm,response);
        
        return user;
    }
    
    /* ------------------------------------------------------------ */
    public String getAuthMethod()
    {
        return SecurityConstraint.__BASIC_AUTH;
    }

    /* ------------------------------------------------------------ */
    public void sendChallenge(UserRealm realm,
                              HttpResponse response)
        throws IOException
    {
        response.setField(HttpFields.__WwwAuthenticate,
                          "basic realm=\""+realm.getName()+'"');
        response.sendError(HttpResponse.__401_Unauthorized);
    }
    
}
    
