//========================================================================
//Copyright (c) 2003 Mort Bay Consulting (Australia) Pty. Ltd.
//$Id$
//========================================================================

package org.mortbay.http;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;

/** Authenticator Interface.
 * This is the interface that must be implemented to provide authentication implementations to the HttpContext.
 */
public interface Authenticator extends Serializable
{
    /** Authenticate.
     * @param realm an <code>UserRealm</code> value
     * @param pathInContext a <code>String</code> value
     * @param request a <code>HttpRequest</code> value
     * @param response a <code>HttpResponse</code> value. If non-null response is passed, 
     *              then a failed authentication will result in a challenge response being 
     *              set in the response.
     * @return User <code>Principal</code> if authenticated. Null if Authentication
     * failed. If the SecurityConstraint.__NOBODY instance is returned,
     * the request is considered as part of the authentication process.
     * @exception IOException if an error occurs
     */
    public Principal authenticate(
        UserRealm realm,
        String pathInContext,
        HttpRequest request,
        HttpResponse response)
        throws IOException;

    /* ------------------------------------------------------------ */
    public String getAuthMethod();
}