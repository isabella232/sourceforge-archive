// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.mortbay.util.Code;
import org.mortbay.util.LazyList;



/* ------------------------------------------------------------ */
/** Describe an auth and/or data constraint. 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class SecurityConstraint
    implements Cloneable
{
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public interface Authenticator
    {
        /* ------------------------------------------------------------ */
        /** Authenticate.
         * @return UserPrincipal if authenticated else null 
         * @exception IOException 
         */
        public UserPrincipal authenticated(UserRealm realm,
                                           String pathInContext,
                                           String pathParams,
                                           HttpRequest request,
                                           HttpResponse response)
        throws IOException;

        public String getAuthMethod();
    };
    
    /* ------------------------------------------------------------ */
    public final static int
        DC_NONE=0,
        DC_INTEGRAL=1,
        DC_CONFIDENTIAL=2;
    
    /* ------------------------------------------------------------ */
    public final static String NONE="NONE";
    public final static String ANY_ROLE="*";
    
    /* ------------------------------------------------------------ */
    private String _name;
    private LazyList _methods;
    private List _umMethods;
    private LazyList _roles;
    private List _umRoles;
    private int _dataConstraint=DC_NONE;
    private boolean _anyRole=false;
    private boolean _authenticate=false;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public SecurityConstraint()
    {}

    /* ------------------------------------------------------------ */
    /** Conveniance Constructor. 
     * @param name 
     * @param role 
     */
    public SecurityConstraint(String name,String role)
    {
        setName(name);
        addRole(role);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name 
     */
    public void setName(String name)
    {
        _name=name;
    }    

    /* ------------------------------------------------------------ */
    /** 
     * @param method 
     */
    public synchronized void addMethod(String method)
    {
        _methods=LazyList.add(_methods,method);
    }
    
    /* ------------------------------------------------------------ */
    public List getMethods()
    {
        if (_umMethods==null && _methods!=null)
            _umMethods=Collections.unmodifiableList(LazyList.getList(_methods));
        return _umMethods;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param method Method name.
     * @return True if this constraint applies to the method. If no
     * method has been set, then the constraint applies to all methods.
     */
    public boolean forMethod(String method)
    {
        if (_methods==null)
            return true;
        for (int i=0;i<LazyList.size(_methods);i++)
            if (LazyList.get(_methods,i).equals(method))
                return true;
        return false;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param role The rolename.  If the rolename is '*' all other
     * roles are removed and anyRole is set true and subsequent
     * addRole calls are ignored.
     * Authenticate is forced true by this call.
     */
    public synchronized void addRole(String role)
    {
        _authenticate=true;
        if (ANY_ROLE.equals(role))
        {
            _roles=null;
            _umRoles=null;
            _anyRole=true;
        }
        else if (!_anyRole)
            _roles=LazyList.add(_roles,role);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if any user role is permitted.
     */
    public boolean isAnyRole()
    {
        return _anyRole;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return List of roles for this constraint.
     */
    public List getRoles()
    {
        if (_umRoles==null && _roles!=null)
            _umRoles=Collections.unmodifiableList(LazyList.getList(_roles));
        return _umRoles;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param role 
     * @return True if the constraint contains the role.
     */
    public boolean hasRole(String role)
    {
        return _roles!=null && _roles.contains(role);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param authenticate True if users must be authenticated 
     */
    public void setAuthenticate(boolean authenticate)
    {
        _authenticate=authenticate;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if the constraint requires request authentication
     */
    public boolean isAuthenticate()
    {
        return _authenticate;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if authentication required but no roles set
     */
    public boolean isForbidden()
    {
        return _authenticate && !_anyRole && LazyList.size(_roles)==0;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param c 
     */
    public void setDataConstraint(int c)
    {
        if (c<0 || c>DC_CONFIDENTIAL)
            throw new IllegalArgumentException("Constraint out of range");
        _dataConstraint=c;
    }


    /* ------------------------------------------------------------ */
    /** 
     * @return Data constrain indicator: 0=DC+NONE, 1=DC_INTEGRAL & 2=DC_CONFIDENTIAL
     */
    public int getDataConstraint()
    {
        return _dataConstraint;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if there is a data constraint.
     */
    public boolean hasDataConstraint()
    {
        return _dataConstraint>DC_NONE;
    }
    
    
    /* ------------------------------------------------------------ */
    public Object clone()
    {
        SecurityConstraint sc=new SecurityConstraint();
        sc._name=_name;
        sc._dataConstraint=_dataConstraint;
        sc._anyRole=_anyRole;
        sc._authenticate=_authenticate;
        
        sc._methods=LazyList.clone(_methods);
        sc._roles=LazyList.clone(_roles);
        
        return sc;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "SC{"+_name+
            ","+_methods+
            ","+(_anyRole?"*":(_roles==null?"-":_roles.toString()))+
            ","+(_dataConstraint==DC_NONE
                 ?"NONE}"
                 :(_dataConstraint==DC_INTEGRAL?"INTEGRAL}":"CONFIDENTIAL}"));
    }


    /* ------------------------------------------------------------ */
    public static int check(List constraints,
                            Authenticator authenticator,
                            UserRealm realm,
                            String pathInContext,
                            String pathParams,
                            HttpRequest request,
                            HttpResponse response)
        throws HttpException, IOException
    {
        // for each constraint
        for (int c=0;c<constraints.size();c++)
        {
            SecurityConstraint sc=(SecurityConstraint)constraints.get(c);

            // Check the method applies
            if (!sc.forMethod(request.getMethod()))
                continue;
                    
            // Does this forbid everything?
            if (sc.isForbidden())
            {
                response.sendError(HttpResponse.__403_Forbidden);
                return -1;
            }
                    
            // Does it fail a role check?
            if (sc.isAuthenticate())
            {
                UserPrincipal user = null;
                if (authenticator==null)
                    response.sendError(HttpResponse.__500_Internal_Server_Error);
                else
                    user=authenticator.authenticated(realm,
                                                     pathInContext,
                                                     pathParams,
                                                     request,
                                                     response);
                if (user==null)
                    return -1; // Auth challenge or redirection already sent
                
                if (!sc.isAnyRole())
                {
                    List roles=sc.getRoles();
                    boolean inRole=false;
                    for (int r=roles.size();r-->0;)
                        if (user.isUserInRole(roles.get(r).toString()))
                        {
                            inRole=true;
                            break;
                        }
                    if (!inRole)
                    {
                        Code.warning("AUTH FAILURE: role for "+user.getName());
                        if ("BASIC".equalsIgnoreCase(authenticator.getAuthMethod()))
                            response.sendBasicAuthenticationChallenge(realm);
                        else
                            response.sendError(HttpResponse.__403_Forbidden,
                                               "User not in required role");
                        return -1; // role failed.
                    }
                }
            }
                
            // Does it fail a data constraint
            if (sc.hasDataConstraint() &&
                sc.getDataConstraint() > SecurityConstraint.DC_NONE &&
                !"https".equalsIgnoreCase(request.getScheme()))   
            {
                response.sendError(HttpResponse.__403_Forbidden);
                return -1;
            }
            
            // Matches a constraint that does not fail
            // anything, so must be OK
            return 1;
        }

        // Didn't actually match any constraint.
        return 0;
    }
}
