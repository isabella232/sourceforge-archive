// ======================================================================
//  Copyright (C) 2002 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.acl.Group;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import org.mortbay.util.Code;
import org.mortbay.http.UserPrincipal;


/* ---------------------------------------------------- */
/** JAASUserPrincipal
 * <p>Implements the JAAS version of the 
 *  org.mortbay.http.UserPrincipal interface.
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
public class JAASUserPrincipal implements UserPrincipal 
{
    
    
    /* ------------------------------------------------ */
    /** RoleStack
     * <P>
     *
     */
    public static class RoleStack
    {
        private static ThreadLocal local = new ThreadLocal();
        

        public static boolean empty ()
        {
            Stack s = (Stack)local.get();

            if (s == null)
                return false;

            return s.empty();
        }
        


        public static void push (JAASRole role)
        {
            Stack s = (Stack)local.get();

            if (s == null)
            {
                s = new Stack();
                local.set (s);
            }

            s.push (role);
        }


        public static void pop ()
        {
            Stack s = (Stack)local.get();

            if ((s == null) || s.empty())
                return;

            s.pop();
        }

        public static JAASRole peek ()
        {
            Stack s = (Stack)local.get();
            
            if ((s == null) || (s.empty()))
                return null;
            
            
            return (JAASRole)s.peek();
        }
        
        public static void clear ()
        {
            Stack s = (Stack)local.get();

            if ((s == null) || (s.empty()))
                return;

            s.clear();
        }
        
    }
    
    //holds the JAAS Credential and roles associated with
    //this UserPrincipal 
    private Subject subject = null;

    private static RoleStack runAsRoles = new RoleStack();
    
    private RoleCheckPolicy roleCheckPolicy = null;

    private String name = null;
    

    
    
    
    /* ------------------------------------------------ */
    /** Constructor. 
     * @param name the name identifying the user
     */
    public JAASUserPrincipal(String name)
    {
        this.name = name;
    }


    /* ------------------------------------------------ */
    /** Is this user authenticated
     * @return true if authenticated false otherwise
     */
    public boolean isAuthenticated ()
    {
        // we wouldn't have proceeded to create the
        // JAASUserPrincipal if their login didn't succeed, so
        // they must be authentic??

        //tmp
        return true;
    }

    

    /* ------------------------------------------------ */
    /** Check if user is in role
     * @param roleName role to check
     * @return true or false accordint to the RoleCheckPolicy.
     */
    public boolean isUserInRole (String roleName)
    {
        if (roleCheckPolicy == null)
            roleCheckPolicy = new StrictRoleCheckPolicy();

        return roleCheckPolicy.checkRole (new JAASRole(roleName),
                                          runAsRoles.peek(),
                                          getRoles());
    }


    
    /* ------------------------------------------------ */
    /** Determine the roles that the LoginModule has set
     * @return 
     */
    public Group getRoles ()
    {
        Group roleGroup = null;
        
        //try extracting a Group named "Roles" whose members will
        //be Principals that are role names
        Set s = subject.getPrincipals (java.security.acl.Group.class);
        Iterator itor = s.iterator();
        
        while (itor.hasNext() && (roleGroup == null))
        {
            Group g = (Group)itor.next();
            
            if (g.getName().equalsIgnoreCase(JAASGroup.ROLES))
                roleGroup = g;
        }

        Code.debug ("Group named \"Roles\""+(roleGroup==null?"does not exist":"does exist"));
        
        
        if (roleGroup != null)
        {
            Enumeration members = roleGroup.members();
            while (members.hasMoreElements())
                Code.debug ("Member = "+((Principal)members.nextElement()).getName());
            
            return roleGroup;
        }
        

        Code.debug ("Trying to find org.mortbay.jaas.JAASRoles instead");

        //try extracting roles put into the Subject directly
        Set roles = subject.getPrincipals (org.mortbay.jaas.JAASRole.class);
        if (!roles.isEmpty())
        {
            roleGroup = new JAASGroup(JAASGroup.ROLES);
            itor = roles.iterator();
            while (itor.hasNext())
                roleGroup.addMember ((JAASRole)itor.next());

            return roleGroup;
        }
        
        //else - user has no roles
        Code.debug ("User has no roles");
        
        return new JAASGroup(JAASGroup.ROLES);
    }

    /* ------------------------------------------------ */
    /** Set the type of checking for isUserInRole
     * @param policy 
     */
    public void setRoleCheckPolicy (RoleCheckPolicy policy)
    {
        roleCheckPolicy = policy;
    }
    

    /* ------------------------------------------------ */
    /** Temporarily associate a user with a role.
     * @param roleName 
     */
    public void pushRole (String roleName)
    {
        runAsRoles.push (new JAASRole(roleName));
    }

    
    /* ------------------------------------------------ */
    /** Remove temporary association between user and role.
     */
    public void popRole ()
    {
        runAsRoles.pop ();
    }


    /* ------------------------------------------------ */
    /** Clean out any pushed roles that haven't been popped
     */
    public void disassociate ()
    {
        runAsRoles.clear();
    }


    /* ------------------------------------------------ */
    /** Get the name identifying the user
     * @return 
     */
    public String getName ()
    {
        return name;
    }
    
    
    /* ------------------------------------------------ */
    /** Sets the JAAS subject for this user.
     *  The subject contains:
     * <ul>
     * <li> the user's credentials
     * <li> Principal for the user's roles
     * @param subject 
     */
    protected void setSubject (Subject subject)
    {
        this.subject = subject;
    }
    
    /* ------------------------------------------------ */
    /** Provide access to the current Subject
     * @return 
     */
    public Subject getSubject ()
    {
        return this.subject;
    }
    
}
