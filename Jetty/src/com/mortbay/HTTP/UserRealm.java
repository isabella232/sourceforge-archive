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
    public String getName();

    public UserPrincipal getUser(String username, HttpRequest request);
}
