// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.http;

import org.mortbay.util.Code;
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
    static public String __ATTR="org.mortbay.http.UserPrincipal";
    
    /* ------------------------------------------------------------ */
    /** 
     * @return The Realm for this user. 
     */
    public UserRealm getUserRealm();
    
    /* ------------------------------------------------------------ */
    /** Authenticate the users credentials 
     * @param credentials The user credentials, normally a password. 
     * @param request The request to be authenticated. Additional
     * parameters may be extracted or set on this request as needed
     * for the authentication mechanism (none required for BASIC and
     * FORM authentication).
     * @return True if the user credentials are OK.
     */
    public boolean authenticate(String credentials, HttpRequest request);

    /* ------------------------------------------------------------ */
    /** Check if the user is in a role. 
     * @param role A role name.
     * @return True if the user can act in that role.
     */
    public boolean isUserInRole(String role);
}
