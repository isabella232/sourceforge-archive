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
/** Client Certificate Authenticator.
 * This Authenticator uses a client certificate to authenticate the user.
 * Each client certificate supplied is tried against the realm using the
 * Principal name as the username and a string representation of the
 * certificate as the credential.
 * @version $id:$
 * @author Greg Wilkins (gregw)
 */
public class ClientCertAuthenticator implements Authenticator
{
    /* ------------------------------------------------------------ */
    public ClientCertAuthenticator()
    {
        Code.warning("Client Cert Authentication is EXPERIMENTAL");
    }
    
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
        java.security.cert.X509Certificate[] certs =
            (java.security.cert.X509Certificate[])
            request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs==null || certs.length==0 || certs[0]==null)
            return null;

        for (int i=0;i<certs.length;i++)
        {
            Principal principal = certs[i].getSubjectDN();
            String cred = certs[i].toString();
            UserPrincipal user = realm.authenticate(principal.getName(),
                                                    cred,
                                                    request);
            if (user!=null)
                return user;
        }

        return null;
    }
    
    public String getAuthMethod()
    {
        return SecurityBase.__CERT_AUTH;
    }


}
