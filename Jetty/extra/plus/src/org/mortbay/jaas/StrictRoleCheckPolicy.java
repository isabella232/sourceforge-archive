// ======================================================================
//  Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas;

import java.security.Principal;
import java.security.acl.Group;


/* ---------------------------------------------------- */
/** StrictRoleChecPolicy
 * <p>Enforces that if a runAsRole is present, then the
 * role to check must be the same as that runAsRole and
 * the set of static roles is ignored.
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
public class StrictRoleCheckPolicy implements RoleCheckPolicy
{

    public boolean checkRole (Principal role, Principal runAsRole, Group roles)
    {
        //check if this user has had any temporary role pushed onto
        //them. If so, then only check if the user has that role.
        if (runAsRole != null)
        {
            return (role.equals(runAsRole));
        }
        else
        {
            if (roles == null)
                return false;
            
            return roles.isMember (role);
        }
        
    }
    
}
