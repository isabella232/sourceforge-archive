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
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.SecurityConstraint.Authenticator;
import org.mortbay.http.UserPrincipal;
import org.mortbay.http.UserRealm;
import org.mortbay.http.SSORealm;
import org.mortbay.util.Code;
import org.mortbay.util.URI;
import org.mortbay.util.Credential;
import org.mortbay.util.Password;

/* ------------------------------------------------------------ */
/** FORM Authentication Authenticator.
 * The HTTP Session is used to store the authentication status of the
 * user, which can be distributed.
 * If the realm implements SSORealm, SSO is supported.
 *
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
    private transient SSORealm _ssoRealm;
    
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
        if (path==null || path.trim().length()==0)
        {
            _formErrorPath=null;
            _formErrorPage=null;
        }
        else
        {
            if (!path.startsWith("/"))
            {
                Code.warning("form-error-page must start with /");
                path="/"+path;
            }
            _formErrorPage=path;
            _formErrorPath=path;

            if (_formErrorPath!=null && _formErrorPath.indexOf('?')>0)
                _formErrorPath=_formErrorPath.substring(0,_formErrorPath.indexOf('?'));
        }
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
            FormCredential form_cred=
                new FormCredential(request.getParameter(__J_USERNAME),
                                   request.getParameter(__J_PASSWORD));
            form_cred.authenticate(realm,httpRequest);
            
            String nuri=(String)session.getAttribute(__J_URI);
            if (nuri==null || nuri.length()==0)
            {
                nuri=request.getContextPath();
                if (nuri.length()==0) nuri="/";
            }
            
            if (form_cred._userPrincipal!=null)
            {
                // Authenticated OK
                Code.debug("Form authentication OK for ",form_cred._jUserName);
                session.removeAttribute(__J_URI); // Remove popped return URI.
                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                httpRequest.setAuthUser(form_cred._jUserName);
                httpRequest.setUserPrincipal(form_cred._userPrincipal);
                session.setAttribute(__J_AUTHENTICATED,form_cred);

                // Sign-on to SSO mechanism
                if (realm instanceof SSORealm)
                {
                    ((SSORealm)realm).setSingleSignOn(httpRequest,
                                                      httpResponse,
                                                      form_cred._userPrincipal,
                                                      new Password(form_cred._jPassword));
                }

                // Redirect to original request
                response.setContentLength(0);
                response.sendRedirect(response.encodeRedirectURL(nuri));
            }
            else
            {
                Code.debug("Form authentication FAILED for ",form_cred._jUserName);
                if (_formErrorPage!=null)
                {
                    response.setContentLength(0);
                    response.sendRedirect(response.encodeRedirectURL
                                          (URI.addPaths(request.getContextPath(),
                                                        _formErrorPage)));
                }
                else
                {
                    response.sendError(HttpResponse.__403_Forbidden);
                }
            }
            
            // Security check is always false, only true after final redirection.
            return null;
        }
        
        // Check if the session is already authenticated.
        FormCredential form_cred = (FormCredential) session.getAttribute(__J_AUTHENTICATED);
        
        if (form_cred != null)
        {
            // We have a form credential. Has it been distributed?
            if (form_cred._userPrincipal==null)
            {
                // This form_cred appears to have been distributed.  Need to reauth
                form_cred.authenticate(realm,httpRequest);
                
                // Sign-on to SSO mechanism
                if (realm instanceof SSORealm)
                {
                    ((SSORealm)realm).setSingleSignOn(httpRequest,
                                                      httpResponse,
                                                      form_cred._userPrincipal,
                                                      new Password(form_cred._jPassword));
                }
            }
            
            // Check that it is still authenticated.
            else if (!form_cred._userPrincipal.isAuthenticated())
                form_cred._userPrincipal=null;

            // If this credential is still authenticated
            if (form_cred._userPrincipal!=null)
            {
                Code.debug("FORM Authenticated for ",form_cred._userPrincipal.getName());
                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                httpRequest.setAuthUser(form_cred._userPrincipal.getName());
                httpRequest.setUserPrincipal(form_cred._userPrincipal);
                return form_cred._userPrincipal;
            }
            else
                session.setAttribute(__J_AUTHENTICATED,null);
        }
        else if (realm instanceof SSORealm)
        {
            // Try a single sign on.
            Credential cred = ((SSORealm)realm).getSingleSignOn(httpRequest,httpResponse);
            
            if (request.getUserPrincipal()!=null)
            {
                form_cred=new FormCredential(request.getUserPrincipal().getName(),
                                             cred!=null?cred.toString():null);
                form_cred._userPrincipal=(UserPrincipal)request.getUserPrincipal();
                form_cred._realm=realm;
                Code.debug("SSO for ",form_cred._userPrincipal);
                           
                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                session.setAttribute(__J_AUTHENTICATED,form_cred);
                return form_cred._userPrincipal;
            }
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
        response.setContentLength(0);
        response.sendRedirect(response.encodeRedirectURL(URI.addPaths(request.getContextPath(),
                                                                      _formLoginPage)));
        return null;
    }


    /* ------------------------------------------------------------ */
    /** FORM Authentication credential holder.
     */
    private static class FormCredential implements Serializable, HttpSessionBindingListener
    {
        String _jUserName;
        String _jPassword;
        transient UserRealm _realm;
        transient UserPrincipal _userPrincipal;

        FormCredential()
        {}
        
        FormCredential(String username, String password)
        {
            _jUserName=username;
            _jPassword=password;
        }
        
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

        public UserPrincipal authenticate(UserRealm realm,HttpRequest httpRequest)
        {
            _userPrincipal=realm.authenticate(_jUserName,_jPassword,httpRequest);
            if (_userPrincipal!=null)
                _realm=realm;
            return _userPrincipal;
        }
        
        public void valueBound(HttpSessionBindingEvent event) {}
        
        public void valueUnbound(HttpSessionBindingEvent event)
        {
            Code.debug("unbind "+this);
            
            if (_realm!=null && _realm instanceof SSORealm)
                ((SSORealm)_realm).clearSingleSignOn(_jUserName);
                
            if (_userPrincipal!=null)
                _realm.logout(_userPrincipal);
            _userPrincipal=null;
            _jPassword=null;
        }
        
        public String toString()
        {
            return "Cred["+_jUserName+"]";
        }

    }
}
