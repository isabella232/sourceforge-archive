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
import com.mortbay.Util.Password;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/* ------------------------------------------------------------ */
/** 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class SecurityHandler extends NullHandler
{
    public final static String __BASIC_AUTH="BASIC";
    
    PathMap _constraintMap=new PathMap();
    String _authMethod=__BASIC_AUTH;
    Map _authRealmMap;
    String _realmName ;
    UserRealm _realm ;

    
    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public UserRealm getRealm()
    {        
        return _realm;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param authRealm 
     */
    public void setRealm(String realmName)
    {
        _realmName=realmName;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public String getAuthMethod()
    {
        return _authMethod;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param authRealm 
     */
    public void setAuthMethod(String method)
    {
        if (!__BASIC_AUTH.equals(method))
            throw new IllegalArgumentException("Not supported: "+method);
        _authMethod = method;
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
    {
        _realm = getHandlerContext().getHttpServer()
            .getRealm(_realmName);
        if (_realm==null)
            throw new IllegalStateException("Unknown realm: "+_realmName);
        super.start();
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
                Code.debug("Auth ",pathInContext," against ",entry);
                
                List scs = (List)entry.getValue();
                // for each constraint
                for (int c=0;c<scs.size();c++)
                {
                    SecurityConstraint sc=(SecurityConstraint)scs.get(c);

                    // Check the method applies
                    if (!sc.forMethod(request.getMethod()))
                        continue;

                    // Check roles
                    if (sc.isAuthenticated())
                    {
                        if (!authenticatedInRole(request,response,sc.roles()))
                            return;
                    }

                    // Check data
                    if (sc.getDataConstraint()!=SecurityConstraint.DC_NONE &&
                        !"https".equals(request.getScheme()))
                    {
                        response.sendError(HttpResponse.__403_Forbidden);
                        return;
                    }
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
        else
            response.sendError(HttpResponse.__401_Unauthorized);

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
                Code.debug("Not in role "+_realmName);
                response.setField(HttpFields.__ContentType,"text/html");
                response.setStatus(HttpResponse.__401_Unauthorized);
                response.setField(HttpFields.__WwwAuthenticate,
                                  "basic realm=\""+_realmName+'"');
                OutputStream out = response.getOutputStream();
                out.write("<HTML><BODY><H1>Authentication Failed</H1></BODY></HTML>\nUser not in an Authorized role".getBytes());
                out.flush();
                response.commit();
                request.setHandled(true);
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
            credentials = B64Code.decode(credentials,"ISO-8859-1");
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
        response.setField(HttpFields.__ContentType,"text/html");
        response.setStatus(HttpResponse.__401_Unauthorized);
        response.setField(HttpFields.__WwwAuthenticate,
                          "basic realm=\""+_realmName+'"');
        OutputStream out = response.getOutputStream();
        out.write("<HTML><BODY><H1>Authentication Failed</H1></BODY></HTML>"
                  .getBytes());
        out.flush();
        response.commit();
        request.setHandled(true);
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

