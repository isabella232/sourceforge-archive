// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;
import java.security.Principal;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.PathMap;
import org.mortbay.http.SecurityConstraint.Authenticator;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.UserPrincipal;
import org.mortbay.http.UserRealm;
import org.mortbay.util.B64Code;
import org.mortbay.util.Code;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** Handler to enforce SecurityConstraints.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class SecurityHandler extends NullHandler
{
    
    public final static String __BASIC_AUTH="BASIC";
    public final static String __FORM_AUTH="FORM";
    public final static String __ATTR="org.mortbay.J.H.SecurityHandler";

    
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public interface FormAuthenticator extends Authenticator
    {
        public final static String __J_SECURITY_CHECK="j_security_check";
        public final static String __J_USERNAME="j_username";
        public final static String __J_PASSWORD="j_password";
        public void formAuthInit(String formLoginPage, String formErrorPage);
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static class BasicAuthenticator implements Authenticator
    {
        public UserPrincipal authenticated(UserRealm realm,
                                           String pathInContext,
                                           String pathParams,
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
            return "BASIC";
        }
    }

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private PathMap _constraintMap=new PathMap();
    private String _authMethod=__BASIC_AUTH;
    private Map _authRealmMap;
    private String _realmName ;
    private UserRealm _realm ;
    private boolean _realmForced=false;
    private String _formLoginPage;
    private String _formErrorPage;
    private Authenticator _authenticator;
    
    /* ------------------------------------------------------------ */
    public UserRealm getUserRealm()
    {        
        return _realm;
    }
    
    /* ------------------------------------------------------------ */
    public String getRealmName()
    {        
        return _realmName;
    }
    
    /* ------------------------------------------------------------ */
    public void setRealmName(String realmName)
    {
        if (isStarted())
            throw new IllegalStateException("Handler started");
        _realmName=realmName;
        _realmForced=false;
    }
    
    /* ------------------------------------------------------------ */
    public void setRealm(String realmName, UserRealm realm)
    {
        if (isStarted())
            throw new IllegalStateException("Handler started");
        _realmName=realmName;
        _realm=realm;
        _realmForced=realm!=null;
    }
    
    /* ------------------------------------------------------------ */
    public String getAuthMethod()
    {
        return _authMethod;
    }
    
    /* ------------------------------------------------------------ */
    public void setAuthMethod(String method)
    {
        if (isStarted())
            throw new IllegalStateException("Handler started");
        if (!__BASIC_AUTH.equals(method) && !__FORM_AUTH.equals(method))
            throw new IllegalArgumentException("Not supported: "+method);
        _authMethod = method;
    }

    /* ------------------------------------------------------------ */
    public String getLoginPage()
    {
      return _formLoginPage;
    }

    /* ------------------------------------------------------------ */
    public void setLoginPage(String page)
    {
        if ( ! page.startsWith("/") )
            page = "/" + page;
        _formLoginPage = page;
    }

    /* ------------------------------------------------------------ */
    public String getErrorPage()
    {
        return _formErrorPage;
    }
    
    /* ------------------------------------------------------------ */
    public void setErrorPage(String page)
    {
        if ( ! page.startsWith("/") )
            page = "/" + page;
        _formErrorPage = page;
    }
    
    /* ------------------------------------------------------------ */
    public void addSecurityConstraint(String pathSpec,
                                      SecurityConstraint sc)
    {
        List scs = (List)_constraintMap.get(pathSpec);
        if (scs==null)
        {
            scs=new ArrayList(2);
            _constraintMap.put(pathSpec,scs);
        }
        scs.add(sc);
        
        Code.debug("added ",sc," at ",pathSpec);
    }

    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        // Check there is a realm
        if (_realmName!=null && _realmName.length()>0)
        {
            
            if (!_realmForced)
                _realm = getHttpContext().getHttpServer()
                    .getRealm(_realmName);
            super.start();
            if (_realm==null)
                Code.warning("Unknown realm: "+_realmName+" for "+this);
        }
        // Or that we have some contraints.
        else if (_constraintMap.size()>0)
        {
            Iterator i = _constraintMap.values().iterator();
            while(i.hasNext())
            {
                Iterator j= ((ArrayList)i.next()).iterator();
                while(j.hasNext())
                {
                    SecurityConstraint sc = (SecurityConstraint)j.next();
                    if (sc.isAuthenticate())
                    {
                        Code.warning("No Realm set for "+this);
                        super.start();
                        return;
                    }
                }
            }
        }
        // If method is FORM
        if (__BASIC_AUTH.equalsIgnoreCase(_authMethod))
        {
            _authenticator=new BasicAuthenticator();
            Code.debug("BasicAuthenticator=",_authenticator);
        }
        
        // If method is FORM
        else if (__FORM_AUTH.equalsIgnoreCase(_authMethod))
        {
            // Make sure that we have both login and error page set
            // before handling any form login requests
            if ( _formLoginPage == null || _formLoginPage.equals("") ||
                 _formErrorPage == null || _formErrorPage.equals("") ||
                 _realm==null)
            {
                Code.warning("Form realm, login and/or error page not set correctly");
            }
            else
            {
                // look for FormAuthenticator
                try
                {
                    Iterator iter = getHttpContext().getHttpHandlers().iterator();
                    while (iter.hasNext())
                    {
                        HttpHandler handler=(HttpHandler)iter.next();
                        if (handler instanceof FormAuthenticator)
                        {
                            _authenticator=(FormAuthenticator)handler;
                            ((FormAuthenticator)_authenticator)
                                .formAuthInit(_formLoginPage,_formErrorPage);
                            Code.debug("FormAuthenticator=",_authenticator);
                            break;
                        }
                    }
                }
                catch(Exception e)
                {
                    Code.warning("Failed to initialize FORM auth",e);
                }
                if (_authenticator==null)
                    Code.warning("FormAuthenticator HttpHandler is required for FORM authentication");
                    
            }
        }

        super.start();
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        // Get all path matches
        List scss =_constraintMap.getMatches(pathInContext);
        if (scss!=null)
        {          
            Code.debug("Security Constraint on ",pathInContext," against ",scss);
            
            // for each path match
        matches:
            for (int m=0;m<scss.size();m++)
            {
                // Get all constraints
                Map.Entry entry=(Map.Entry)scss.get(m);
                if (Code.verbose())
                    Code.debug("Check ",pathInContext," against ",entry);

                List scs = (List)entry.getValue();
                
                switch (SecurityConstraint.check(scs,
                                                 _authenticator,
                                                 _realm,
                                                 pathInContext,
                                                 pathParams,
                                                 request,
                                                 response))
                {
                  case -1: return; // Auth failed.
                  case 0: continue; // No constraints matched
                  case 1: break matches; // Passed a constraint.
                }
            }
        }
        
        // Handle form security check
        if (_authenticator!=null &&
            pathInContext.endsWith(FormAuthenticator.__J_SECURITY_CHECK))
        {
            Code.debug("FORM j_security_check");
            _authenticator.authenticated(_realm,
                                         pathInContext,
                                         pathParams,
                                         request,
                                         response);
        }
    }
}

