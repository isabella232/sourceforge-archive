// ======================================================================
//  Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas;

import java.security.Principal;
import java.security.acl.Group;


public interface RoleCheckPolicy 
{
    /* ------------------------------------------------ */
    /** Check if a role is either a runAsRole or in a set of roles
     * @param role the role to check
     * @param runAsRole a pushed role (can be null)
     * @param roles a Group whose Principals are role names
     * @return 
     */
    public boolean checkRole (Principal role, Principal runAsRole, Group roles);
    
}
