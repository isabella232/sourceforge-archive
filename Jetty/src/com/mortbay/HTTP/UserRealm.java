// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;

/* ------------------------------------------------------------ */
/** User Realm.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public interface UserRealm
{
    static public String __UserRole="com.mortbay.HTTP.User";
    
    public String getName();

    public UserPrincipal getUser(String username, HttpRequest request);
}
