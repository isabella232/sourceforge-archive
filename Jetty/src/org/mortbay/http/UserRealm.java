// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.http;

/* ------------------------------------------------------------ */
/** User Realm.
 *
 * This interface should be specialized to provide specific user
 * lookup and authentication using arbitrary methods.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public interface UserRealm
{
    static public String __UserRole="org.mortbay.http.User";
    
    public String getName();

    public UserPrincipal getUser(String username);
}
