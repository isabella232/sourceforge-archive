// ======================================================================
//  Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas;

import java.util.HashMap;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.UserRealm;
import org.mortbay.jaas.callback.AbstractCallbackHandler;
import org.mortbay.jaas.callback.DefaultCallbackHandler;
import org.mortbay.util.Log;
import java.security.Principal;



/* ---------------------------------------------------- */
/** JAASUserRealm
 * <p>
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see
 * @version 1.0 Mon Apr 14 2003
 * @author Jan Bartel (janb)
 */
public class JAASUserRealm implements UserRealm
{
    protected String realmName;
    protected String loginModuleName;
    protected AbstractCallbackHandler callbackHandler;
    protected HashMap userMap;
    protected RoleCheckPolicy roleCheckPolicy;
    
    public JAASUserRealm ()
    {
        userMap = new HashMap();
    }
    
    public JAASUserRealm(String name)
    {
        this();
        realmName = name;
    }

    public String getName()
    {
        return realmName;
    }

    public void setName (String name)
    {
        realmName = name;
    }

    public void setLoginModuleName (String name)
    {
        loginModuleName = name;
    }


    public void setCallbackHandler (AbstractCallbackHandler handler)
    {
        callbackHandler = handler;
    }

    public void setRoleCheckPolicy (RoleCheckPolicy policy)
    {
        roleCheckPolicy = policy;
    }

    public Principal getUserPrincipal(String username)
    {
        return (Principal)userMap.get(username);
    }
    
    public Principal authenticate(String username,
                                  Object credentials,
                                  HttpRequest request)
    {
        try
        {
            JAASUserPrincipal userPrincipal = (JAASUserPrincipal)userMap.get(username);

            if (userPrincipal != null)
                return userPrincipal;
            
                
            //user has not been authenticated
            if (callbackHandler == null)
            {
                Log.warning ("No CallbackHandler configured: using DefaultCallbackHandler");
                callbackHandler = new DefaultCallbackHandler();
            }

            callbackHandler.setUserName(username);
            callbackHandler.setCredential(credentials);
            

            //set up the login context
            LoginContext loginContext = new LoginContext(loginModuleName,
                                                         callbackHandler);

            loginContext.login();

            //login success
            userPrincipal = new JAASUserPrincipal(username);
            userPrincipal.setSubject(loginContext.getSubject());
            userPrincipal.setRoleCheckPolicy (roleCheckPolicy);
            
            userMap.put (username, userPrincipal);
            
            return userPrincipal;       
        }
        catch (LoginException e)
        {
            Log.warning (e);
            return null;
        }     
    }

    
    /* ------------------------------------------------------------ */
    public boolean isAuthenticated(Principal user)
    {
        // XXX This is not correct if auth can expire!
        return user instanceof JAASUserPrincipal;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isUserInRole(Principal user, String role)
    {
        if (user instanceof JAASUserPrincipal)
            return ((JAASUserPrincipal)user).isUserInRole(role);
        return false;
    }
    
    /* ------------------------------------------------------------ */
    public void disassociate(Principal user)
    {
        if (user != null)
            ((JAASUserPrincipal)user).disassociate();
    }

    
    /* ------------------------------------------------------------ */
    public Principal pushRole(Principal user, String role)
    {
        ((JAASUserPrincipal)user).pushRole(role);
        return user;
    }
    
    /* ------------------------------------------------------------ */
    public Principal popRole(Principal user)
    {
        ((JAASUserPrincipal)user).popRole();
        return user;
    }

}
