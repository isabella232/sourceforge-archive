// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.HTTP.HttpException;
import com.mortbay.HTTP.HttpFields;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.HttpResponse;
import com.mortbay.HTTP.PathMap;
import com.mortbay.HTTP.SecurityConstraint;
import com.mortbay.HTTP.HashUserRealm;
import com.mortbay.HTTP.UserRealm;
import com.mortbay.HTTP.UserPrincipal;
import com.mortbay.Util.B64Code;
import com.mortbay.Util.Code;
import com.mortbay.Util.StringUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;


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

    PathMap _constraintMap=new PathMap();
    String _authMethod=__BASIC_AUTH;
    Map _authRealmMap;
    String _realmName ;
    UserRealm _realm ;
    String _formLoginPage;
    String _formErrorPage;

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
        if (_realmName!=null && _realmName.length()>0)
        {
            _realm = getHandlerContext().getHttpServer()
                .getRealm(_realmName);
            super.start();
            if (_realm==null)
                Code.warning("Unknown realm: "+_realmName+" for "+this);
        }
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
            super.start();
        }
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        Code.debug("Authenticate "+pathInContext);

        // Get all path matches
        List scss =_constraintMap.getMatches(pathInContext);
        if (scss!=null)
        {            
            // for each path match 
            for (int m=0;m<scss.size();m++)
            {
                // Get all constraints
                Map.Entry entry=(Map.Entry)scss.get(m);
                if (Code.verbose())
                    Code.debug("Auth ",pathInContext," against ",entry);
                
                List scs = (List)entry.getValue();
                // for each constraint
                for (int c=0;c<scs.size();c++)
                {
                    SecurityConstraint sc=(SecurityConstraint)scs.get(c);

                    // Check the method applies
                    if (!sc.forMethod(request.getMethod()))
                        continue;

                    // Does this forbid everything?
                    if (!sc.isAuthenticated() && !sc.hasDataConstraint())    
                        response.sendError(HttpResponse.__403_Forbidden);

                    
                    // Does it fail a role check?
                    if (sc.isAuthenticated() &&
                        !sc.hasRole(SecurityConstraint.NONE) &&
                        authenticatedInRole(request,response,sc.roles()))
                        // return as an auth challenge will have been set
                        return;
                    
                    // Does it fail a data constraint
                    if (sc.hasDataConstraint() &&
                        "https".equalsIgnoreCase(request.getScheme()))   
                        response.sendError(HttpResponse.__403_Forbidden);
                        
                    // Matches a constraint that does not fail
                    // anything, so must be OK
                    return;    
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    private boolean authenticatedInRole(HttpRequest request,
                                        HttpResponse response,
                                        Iterator roles)
        throws IOException
    {
        boolean userAuth=false;
        
        if (__BASIC_AUTH.equals(_authMethod))
            userAuth=basicAuthenticated(request,response);
        else if (__FORM_AUTH.equals(_authMethod))
            userAuth=formAuthenticated(request, response);
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
     * @return
     */
    private boolean formAuthenticated(HttpRequest request,
                                       HttpResponse response)
        throws IOException
    {
        /* Check the session object for login info. If user is
         * already logged in then just return true;
         */

        String uri = request.getURI().toString();
        /*** STORE ORIGINAL REQUEST URI SOMEWHERE... *******/

        if ( uri.substring(uri.lastIndexOf("/")+1).equals("j_security_check") )
        {
          String username = request.getParameter("j_username");
          String password = request.getParameter("j_password");

          if (_realm!=null)
          {
                  UserPrincipal user = _realm.getUser(username,request);
                  if (user!=null && user.authenticate(password))
                  {
                      request.setAttribute(HttpRequest.__AuthType,"FORM");
                      request.setAttribute(HttpRequest.__AuthUser,username);
                      request.setAttribute(UserPrincipal.__ATTR,user);

                      // Store user login info in session object

                      // response.sendRedirect( /**** WHERE IS THE OLD URI STORED? **********/);

                      return true;
                  }

                  Code.warning("AUTH FAILURE: user "+username);
          }
        }

        Code.debug("Unauthorized in "+_realmName);

        // Make sure that we have both login and error page set
        // before handling any form login requests
        if ( _formLoginPage == null || _formLoginPage.equals("")
            || _formErrorPage == null || _formErrorPage.equals("") )
        {
          response.sendError(HttpResponse.__404_Not_Found);
          Code.debug("Form login and/or error page not set correctly");
        }

        // OK, user has not logged in. Send login page.
        response.sendRedirect(_formLoginPage);

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

