// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;

import com.mortbay.Util.Code;
import java.security.Principal;


/* ------------------------------------------------------------ */
/** User Principal.
 * Extends the security principal with a method to check if the user is in a
 * role. 
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public interface UserPrincipal extends Principal
{
    static public String __ATTRIBUTE_NAME="com.mortbay.HTTP.UserPrincipal";
    public boolean isUserInRole(String role);
}
