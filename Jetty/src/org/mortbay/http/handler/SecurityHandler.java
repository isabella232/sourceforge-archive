// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.PathMap;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.HashUserRealm;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.UserRealm;
import org.mortbay.http.UserPrincipal;
import org.mortbay.util.B64Code;
import org.mortbay.util.Code;
import org.mortbay.util.StringUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.lang.reflect.Method;

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

    PathMap _constraintMap=new PathMap();
    String _authMethod=__BASIC_AUTH;
    Map _authRealmMap;
    String _realmName ;
    UserRealm _realm ;

    
    /* ------------------------------------------------------------ */
    public interface FormAuthenticator
    {
        public final static String __J_SECURITY_CHECK="j_security_check";
        public final static String __J_USERNAME="j_username";
        public final static String __J_PASSWORD="j_password";
        
        /* ------------------------------------------------------------ */
        /** 
         * @param shandler 
         * @param pathInContext 
         * @param request 
         * @param response 
         * @return True if the request has been authenticated. 
         * @exception IOException 
         */
        public boolean formAuthenticated(SecurityHandler shandler,
                                         String pathInContext,
                                         String pathParams,
                                         HttpRequest request,
                                         HttpResponse response)
        throws IOException;
    };
    String _formLoginPage;
    String _formErrorPage;
    FormAuthenticator _formAuthenticator;
    
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
            _realm = getHandlerContext().getHttpServer()
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
                    if (sc.isAuthenticated())
                    {
                        Code.warning("No Realm set for "+this);
                        super.start();
                        return;
                    }
                }
            }
        }

        // If method is FORM
        if (__FORM_AUTH.equals(_authMethod))
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
                    Iterator iter = getHandlerContext().getHandlers().iterator();
                    while (iter.hasNext())
                    {
                        HttpHandler handler=(HttpHandler)iter.next();
                        if (handler instanceof FormAuthenticator)
                        {
                            _formAuthenticator=(FormAuthenticator)handler;
                            Code.debug("FormAuthenticator=",_formAuthenticator);
                            break;
                        }
                    }
                }
                catch(Exception e)
                {
                    Code.warning("Failed to initialize FORM auth",e);
                }
                if (_formAuthenticator==null)
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
                // for each constraint
                for (int c=0;c<scs.size();c++)
                {
                    SecurityConstraint sc=(SecurityConstraint)scs.get(c);

                    // Check the method applies
                    if (!sc.forMethod(request.getMethod()))
                        continue;
                    
                    // Does this forbid everything?
                    if (!sc.isAuthenticated() &&!sc.hasDataConstraint())
                    {
                        response.sendError(HttpResponse.__403_Forbidden);
                        return;
                    }
                    
                    // Does it fail a role check?
                    if (sc.isAuthenticated() &&
                        !sc.hasRole(SecurityConstraint.NONE) &&
                        !authenticatedInRole(pathInContext,pathParams,request,response,sc.roles()))
                        // return as an auth challenge will have been set
                        return;
                    
                    // Does it fail a data constraint
                    if (sc.hasDataConstraint() &&
                        !"https".equalsIgnoreCase(request.getScheme()))   
                    {
                        response.sendError(HttpResponse.__403_Forbidden);
                        return;
                    }
                    
                    // Matches a constraint that does not fail
                    // anything, so must be OK
                    break matches;    
                }
            }
        }

        if (_formAuthenticator!=null &&
            pathInContext.endsWith(FormAuthenticator.__J_SECURITY_CHECK))
        {
            Code.debug("FORM j_security_check");
            _formAuthenticator
                .formAuthenticated(this,pathInContext,pathParams,request,response);
        }
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if request is authenticated in the role.
     */
    private boolean authenticatedInRole(String pathInContext,
                                        String pathParams,
                                        HttpRequest request,
                                        HttpResponse response,
                                        Iterator roles)
        throws IOException
    {
        boolean userAuth=false;
        
        if (__BASIC_AUTH.equals(_authMethod))
            userAuth=basicAuthenticated(request,response);
        else if (__FORM_AUTH.equals(_authMethod))
        {
            if (_formAuthenticator==null)
            {
                response.sendError(HttpResponse.__500_Internal_Server_Error);
                return false;
            }
            return _formAuthenticator
                .formAuthenticated(this,pathInContext,pathParams,request,response);
        }
        else
        {
            response.setField(HttpFields.__WwwAuthenticate,
                              "basic realm=\""+_realmName+'"');
            response.sendError(HttpResponse.__401_Unauthorized);
        }
        
        if (!userAuth)
            return false;

        // Check if user is in a role that is suitable
        boolean inRole=false;
        while(roles.hasNext())
        {
            String role=roles.next().toString();            
            if (request.isUserInRole(role))
            {
                inRole=true;
                break;
            }
        }

        // If no role reject authentication.
        if (!inRole)
        {
            Code.warning("AUTH FAILURE: role for "+
                         request.getUserPrincipal().getName());
            if (__BASIC_AUTH.equals(_authMethod))
            {
                response.setField(HttpFields.__WwwAuthenticate,
                                  "basic realm=\""+_realmName+'"');
                response.sendError(HttpResponse.__401_Unauthorized);
            }
            else if (__FORM_AUTH.equals(_authMethod))
            {
                response.sendRedirect(_formErrorPage);
            }
            else
                response.sendError(HttpResponse.__403_Forbidden);
            return false;
        }
        
        return userAuth && inRole;
    }
    

    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    private boolean basicAuthenticated(HttpRequest request,
                                       HttpResponse response)
        throws IOException
    {
        String credentials =
            request.getField(HttpFields.__Authorization);
        
        if (credentials!=null )
        {
            Code.debug("Credentials: "+credentials);
            credentials =
                credentials.substring(credentials.indexOf(' ')+1);
            credentials = B64Code.decode(credentials,StringUtil.__ISO_8859_1);
            int i = credentials.indexOf(':');
            String username = credentials.substring(0,i);
            String password = credentials.substring(i+1);
            

            if (_realm!=null)
            {
                UserPrincipal user = _realm.getUser(username,request);
                if (user!=null && user.authenticate(password))
                {
                    request.setAttribute(HttpRequest.__AuthType,"BASIC");
                    request.setAttribute(HttpRequest.__AuthUser,username);
                    request.setAttribute(UserPrincipal.__ATTR,user);
                    return true;
                }
                
                Code.warning("AUTH FAILURE: user "+username);
            }
        }
        
        Code.debug("Unauthorized in "+_realmName);
        response.setField(HttpFields.__WwwAuthenticate,
                          "basic realm=\""+_realmName+'"');
        response.sendError(HttpResponse.__401_Unauthorized);
        return false;
    }


    /* ------------------------------------------------------------ */
    /** 
     * @deprecated use HttpServer.addRealm()
     */
    public synchronized void addUser(String username, String password)
    {
        Code.warning("addUser deprecated, use HttpServer.addRealm()");
    }    
}

