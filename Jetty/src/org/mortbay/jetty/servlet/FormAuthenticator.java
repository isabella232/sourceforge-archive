// ===========================================================================
// Copyright (c) 1996-2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.io.IOException;
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
            String username = request.getParameter(__J_USERNAME);
            String password = request.getParameter(__J_PASSWORD);
            
            UserPrincipal user = realm.authenticate(username,password,httpRequest);
            String nuri=(String)session.getAttribute(__J_URI);
            session.removeAttribute(__J_URI); // Remove popped return URI.
            if (nuri==null || nuri.length()==0)
                nuri="/";
            
            if (user!=null)
            {
                Code.debug("Form authentication OK for ",username);
                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                httpRequest.setAuthUser(username);
                httpRequest.setUserPrincipal(user);
                session.setAttribute(__J_AUTHENTICATED,user);
                response.sendRedirect(response.encodeRedirectURL(nuri));
            }
            else
            {
                Code.debug("Form authentication FAILED for ",username);
                if (_formErrorPage!=null)
                    response.sendRedirect(response.encodeRedirectURL
                                          (URI.addPaths(request.getContextPath(),
                                                        _formErrorPage)));
                else
                    response.sendError(HttpResponse.__403_Forbidden);
            }
            
            // Security check is always false, only true after final redirection.
            return null;
        }

        // Check if the session is already authenticated.
        UserPrincipal user = (UserPrincipal) session.getAttribute(__J_AUTHENTICATED);
        if (user != null)
        {
            if (user.isAuthenticated())
            {
                Code.debug("FORM Authenticated for ",user.getName());
                httpRequest.setAuthType(SecurityConstraint.__FORM_AUTH);
                httpRequest.setAuthUser(user.getName());
                httpRequest.setUserPrincipal(user);
                return user;
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
        response.sendRedirect(response.encodeRedirectURL(URI.addPaths(request.getContextPath(),
                                           _formLoginPage)));
        return null;
    }

}
