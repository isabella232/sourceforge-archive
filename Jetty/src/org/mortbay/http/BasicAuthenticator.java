// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.io.IOException;
import org.mortbay.http.SecurityConstraint.Authenticator;
import java.security.Principal;
import org.mortbay.util.Code;

/* ------------------------------------------------------------ */
/** BASIC authentication.
 *
 * @version $id:$
 * @author Greg Wilkins (gregw)
 */
public class BasicAuthenticator implements Authenticator
{
    /* ------------------------------------------------------------ */
    /** 
     * @return UserPrinciple if authenticated or null if not. If
     * Authentication fails, then the authenticator may have committed
     * the response as an auth challenge or redirect.
     * @exception IOException 
     */
    public UserPrincipal authenticated(UserRealm realm,
                                       String pathInContext,
                                       HttpRequest request,
                                       HttpResponse response)
        throws IOException
    {
        UserPrincipal user=request.basicAuthenticated(realm);
        if (user==null)
            response.sendBasicAuthenticationChallenge(realm);
        return user;
    }
    
    public String getAuthMethod()
    {
        return SecurityBase.__BASIC_AUTH;
    }
}
    
