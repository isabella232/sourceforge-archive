// ===========================================================================
// Copyright (c) 1996-2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.io.Serializable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.SecurityConstraint.Authenticator;
import org.mortbay.http.UserPrincipal;
import org.mortbay.http.UserRealm;
import org.mortbay.util.Code;
import org.mortbay.util.URI;

/* ------------------------------------------------------------ */
/** 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 * @author dan@greening.name
 */
public class FormAuthenticator implements Authenticator
{
    /* ------------------------------------------------------------ */
    public final static String __J_URI="org.mortbay.jetty.URI";
    public final static String __J_AUTHENTICATED="org.mortbay.jetty.Auth";
    public final static String __J_SECURITY_CHECK="j_security_check";
    public final static String __J_USERNAME="j_username";
    public final static String __J_PASSWORD="j_password";

    private String _formErrorPage;
    private String _formErrorPath;
    private String _formLoginPage;
    private String _formLoginPath;
    
    /* ------------------------------------------------------------ */
    public String getAuthMethod()
    {
        return HttpServletRequest.FORM_AUTH;
    }

    /* ------------------------------------------------------------ */
    public void setLoginPage(String path)
    {
        if (!path.startsWith("/"))
        {
            Code.warning("form-login-page must start with /");
            path="/"+path;
        }
        _formLoginPage=path;
        _formLoginPath=path;
        if (_formLoginPath.indexOf('?')>0)
            _formLoginPath=_formLoginPath.substring(0,_formLoginPath.indexOf('?'));
    }

    /* ------------------------------------------------------------ */
    public String getLoginPage()
    {
        return _formLoginPage;
    }
    
    /* ------------------------------------------------------------ */
    public void setErrorPage(String path)
    {
        if (!path.startsWith("/"))
        {
            Code.warning("form-error-page must start with /");
            path="/"+path;
        }
        _formErrorPage=path;
        _formErrorPath=path;
        if (_formErrorPath.indexOf('?')>0)
            _formErrorPath=_formErrorPath.substring(0,_formErrorPath.indexOf('?'));
    }

    /* ------------------------------------------------------------ */
    public String getErrorPage()
    {
        return _formErrorPage;
    }
    
    /* ------------------------------------------------------------ */
    /** Perform form authentication.
     * Called from SecurityHandler.
     * @return UserPrincipal if authenticated else null.
     */
    public UserPrincipal authenticated(UserRealm realm,
                                       String pathInContext,
                                       HttpRequest httpRequest,
                                       HttpResponse httpResponse)
        throws IOException
    {
        HttpServletRequest request =(ServletHttpRequest)httpRequest.getWrapper();
        HttpServletResponse response =(HttpServletResponse) httpResponse.getWrapper();

        // Handle paths
        String uri = pathInContext;

        // Setup session 
        HttpSession session=request.getSession(true);
        
        // Handle a request for authentication.
        if ( uri.substring(uri.lastIndexOf("/")+1).startsWith(__J_SECURITY_CHECK) )
        {
            // Check the session object for login info.
            FormCredential credential=new FormCredential();
            credential._jUserName = request.getParameter(__J_USERNAME);
            credential._jPassword = request.getParameter(__J_PASSWORD);
            
            credential._userPrincipal = realm.authenticate(credential._jUserName,
                                                           credential._jPassword,
                                                           httpRequest);
            
            String nuri=(String)session.getAttribute(__J_URI);
            if (nuri==null || nuri.length()==0)
                nuri="/";
            
            if (credential._userPrincipal!=null)
            {
                Code.debug("Form authentication OK for ",credential._jUserName);
                session.removeAttribute(__J_URI); // Remove popped return URI.
                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                httpRequest.setAuthUser(credential._jUserName);
                httpRequest.setUserPrincipal(credential._userPrincipal);
                session.setAttribute(__J_AUTHENTICATED,credential);
                response.sendRedirect(response.encodeRedirectURL(nuri));
            }
            else
            {
                Code.debug("Form authentication FAILED for ",credential._jUserName);
                if (_formErrorPage!=null)
                {
                    response.sendRedirect(response.encodeRedirectURL
                                          (URI.addPaths(request.getContextPath(),
                                                        _formErrorPage)));
                }
                else
                {
                    session.removeAttribute(__J_URI); // Remove popped return URI.
                    response.sendError(HttpResponse.__403_Forbidden);
                }
            }
            
            // Security check is always false, only true after final redirection.
            return null;
        }

        // Check if the session is already authenticated.
        FormCredential credential = (FormCredential) session.getAttribute(__J_AUTHENTICATED);
        
        if (credential != null)
        {
            if (credential._userPrincipal==null)
            {
                // This credential appears to have been distributed.  Need to reauth
                credential._userPrincipal = realm.authenticate(credential._jUserName,
                                                               credential._jPassword,
                                                               httpRequest);
            }
            else if (!credential._userPrincipal.isAuthenticated())
                credential._userPrincipal=null;
                
            if (credential._userPrincipal!=null)
            {
                Code.debug("FORM Authenticated for ",credential._userPrincipal.getName());
                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                httpRequest.setAuthUser(credential._userPrincipal.getName());
                httpRequest.setUserPrincipal(credential._userPrincipal);
                return credential._userPrincipal;
            }
            else
                session.setAttribute(__J_AUTHENTICATED,null);
        }
        
        // Don't authenticate authform or errorpage
        if (pathInContext!=null &&
            pathInContext.equals(_formErrorPath) || pathInContext.equals(_formLoginPath))
            return SecurityConstraint.__NOBODY;
        
        // redirect to login page
        if (httpRequest.getQuery()!=null)
            uri+="?"+httpRequest.getQuery();
        session.setAttribute(__J_URI, 
        	request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() 
        	+ URI.addPaths(request.getContextPath(),uri));
        response.sendRedirect(response.encodeRedirectURL(URI.addPaths(request.getContextPath(),
                                           _formLoginPage)));
        return null;
    }


    /* ------------------------------------------------------------ */
    /** FORM Authentication credential holder.
     */
    private static class FormCredential implements Serializable
    {
        private String _jUserName;
        private String _jPassword;
        private transient UserPrincipal _userPrincipal;
        
        public int hashCode()
        {
            return _jUserName.hashCode()+_jPassword.hashCode();
        }

        public boolean equals(Object o)
        {
            if (!(o instanceof FormCredential))
                return false;
            FormCredential fc = (FormCredential)o;
            return
                _jUserName.equals(fc._jUserName) &&
                _jPassword.equals(fc._jPassword);
        }

        public String toString()
        {
            return "Cred["+_jUserName+"]";
        }
        
    }
    
}
