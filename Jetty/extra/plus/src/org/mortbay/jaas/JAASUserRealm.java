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
import org.mortbay.http.UserPrincipal;
import org.mortbay.http.UserRealm;
import org.mortbay.jaas.callback.AbstractCallbackHandler;
import org.mortbay.jaas.callback.DefaultCallbackHandler;
import org.mortbay.util.Loader;
import org.mortbay.util.Log;



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
    protected String callbackHandlerClass;
    protected HashMap userMap;
    protected RoleCheckPolicy roleCheckPolicy;

    
    /* ---------------------------------------------------- */
    /**
     * UserInfo
     *
     * Information cached for an authenticated user.
     * 
     *
     * 
     *
     */
    protected class UserInfo
    {
        String name;
        JAASUserPrincipal principal;
        LoginContext context;

        public UserInfo (String name, JAASUserPrincipal principal, LoginContext context)
        {
            this.name = name;
            this.principal = principal;
            this.context = context;
        }

        public String getName ()
        {
            return name;
        }

        public JAASUserPrincipal getJAASUserPrincipal ()
        {
            return principal;
        }

        public LoginContext getLoginContext ()
        {
            return context;
        }
    }


    /* ---------------------------------------------------- */
    /**
     * Constructor.
     *
     */
    public JAASUserRealm ()
    {
        userMap = new HashMap();
    }
    

    /* ---------------------------------------------------- */
    /**
     * Constructor.
     *
     * @param name the name of the realm
     */
    public JAASUserRealm(String name)
    {
        this();
        realmName = name;
    }


    /* ---------------------------------------------------- */
    /**
     * Get the name of the realm.
     *
     * @return name or null if not set.
     */
    public String getName()
    {
        return realmName;
    }


    /* ---------------------------------------------------- */
    /**
     * Set the name of the realm
     *
     * @param name a <code>String</code> value
     */
    public void setName (String name)
    {
        realmName = name;
    }



    /**
     * Set the name to use to index into the config
     * file of LoginModules.
     *
     * @param name a <code>String</code> value
     */
    public void setLoginModuleName (String name)
    {
        loginModuleName = name;
    }


    /* ---------------------------------------------------- */
    /**
     * Set up a specifc CallbackHandler. 
     * If not called, then the DefaultCallbackHandler is used.
     *
     * @param handler an <code>String</code> value
     */
    public void setCallbackHandlerClass (String cname)
    {
        callbackHandlerClass = cname;
    }
    
    

    public void setRoleCheckPolicy (RoleCheckPolicy policy)
    {
        roleCheckPolicy = policy;
    }

    
    /* ---------------------------------------------------- */
    /**
     * Authenticate a user.
     * 
     *
     * @param username provided by the user at login
     * @param credentials provided by the user at login
     * @param request a <code>HttpRequest</code> value
     * @return authenticated JAASUserPrincipal or  null if authenticated failed
     */
    public UserPrincipal authenticate(String username,
                                      Object credentials,
                                      HttpRequest request)
    {
        try
        {
            UserInfo info = null;
            synchronized (this)
            {
                info = (UserInfo)userMap.get(username);
            }

            //user has been previously authenticated, but
            //re-authentication has been requested, so flow that 
            //thru all the way to the login module mechanism and
            //remove their previously authenticated status
            //TODO: ensure cache state and "logged in status" are synchronized
            if (info != null)
            {
                synchronized (this)
                {
                    userMap.remove (username);
                }
            }


            AbstractCallbackHandler callbackHandler = null;
            
            //user has not been authenticated
            if (callbackHandlerClass == null)
            {
                Log.warning ("No CallbackHandler configured: using DefaultCallbackHandler");
                callbackHandler = new DefaultCallbackHandler();
            }
            else
                callbackHandler = (AbstractCallbackHandler)Loader.loadClass(JAASUserRealm.class, callbackHandlerClass).getConstructors()[0].newInstance(new Object[0]);


            callbackHandler.setUserName(username);
            callbackHandler.setCredential(credentials);
            

            //set up the login context
            LoginContext loginContext = new LoginContext(loginModuleName,
                                                         callbackHandler);

            loginContext.login();

            //login success
            JAASUserPrincipal userPrincipal = new JAASUserPrincipal(username);
            userPrincipal.setSubject(loginContext.getSubject());
            userPrincipal.setRoleCheckPolicy (roleCheckPolicy);
            
            synchronized (this)
            {
                userMap.put (username, new UserInfo (username, userPrincipal, loginContext));
            }
            
            return userPrincipal;       
        }
        catch (Exception e)
        {
            Log.warning (e);
            return null;
        }     
    }

    

    /* ---------------------------------------------------- */
    /**
     * Removes any auth info associated with eg. the thread.
     *
     * @param user a UserPrincipal to disassociate
     */
    public void disassociate(UserPrincipal user)
    {
        if (user != null)
            ((JAASUserPrincipal)user).disassociate();
    }

    

    /* ---------------------------------------------------- */
    /**
     * Temporarily adds a role to a user.
     *
     * Temporarily granting a role pushes the role onto a stack
     * of temporary roles. Temporary roles must therefore be
     * removed in order.
     *
     * @param user the UserPrincipal to which to add the role
     * @param role the role name
     * @return the UserPrincipal with the role added
     */
    public UserPrincipal pushRole(UserPrincipal user, String role)
    {
        ((JAASUserPrincipal)user).pushRole(role);
        return user;
    }
    

    /* ---------------------------------------------------- */
    /**
     * Remove a role temporarily granted to a user.
     *
     * @param user the UserPrincipal from whom to remove the role
     * @return the UserPrincipal
     */
    public UserPrincipal popRole(UserPrincipal user)
    {
        ((JAASUserPrincipal)user).popRole();
        return user;
    }



    /* ---------------------------------------------------- */
    /**
     * Logout a previously logged in user.
     * This can only work for FORM authentication
     * as BasicAuthentication is stateless.
     * 
     * The user's LoginContext logout() method is called.
     * @param user an <code>UserPrincipal</code> value
     */
    public void logout(UserPrincipal user)
    {
        try
        {
            if (!(user instanceof JAASUserPrincipal))
                throw new IllegalArgumentException (user + " is not a JAASUserPrincipal");
            
            String key = ((JAASUserPrincipal)user).getName();
            
            UserInfo info  = null;
            synchronized (this)
            {
                info = (UserInfo)userMap.get(key);
            }
            
            if (info == null)
                Log.warning ("Logout called for user="+user+" who is NOT in the authentication cache");
            else 
                info.getLoginContext().logout();
            
            synchronized (this)
            {
                userMap.remove (key);
            }
            Log.event (user+" has been LOGGED OUT");
        }
        catch (LoginException e)
        {
            Log.warning (e);
        }
    }
    
}
