// ======================================================================
//  Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas;

import java.security.Principal;
import java.util.HashMap;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.UserRealm;
import org.mortbay.jaas.callback.AbstractCallbackHandler;
import org.mortbay.jaas.callback.DefaultCallbackHandler;



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
	private static Log log = LogFactory.getLog(JAASUserRealm.class);
	
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

            //user has been previously authenticated, but
            //re-authentication has been requested, so remove them
            if (userPrincipal != null)
                userMap.remove(userPrincipal);
                
            
            if (callbackHandler == null)
            {
                log.warn("No CallbackHandler configured: using DefaultCallbackHandler");
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
            log.warn(e);
            return null;
        }     
    }

    
    /* ------------------------------------------------------------ */
    public boolean isAuthenticated(Principal user)
    {
        // TODO This is not correct if auth can expire! We need to
        // get the user out of the cache
        return (userMap.get(user.getName()) != null);
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
