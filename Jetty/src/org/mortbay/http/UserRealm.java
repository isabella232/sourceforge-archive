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

    /* ------------------------------------------------------------ */
    /** Deauthenticate the calling context with a Principal.
     * This method is called when the calling context is not longer
     * associated with the Principal.  It should be used by an implementation
     * to remove context associations such as ThreadLocals.
     * The UserPrincipal object remains authenticated, as it may be
     * associated with other contexts.
     * @param user A UserPrincipal allocated from this realm.
     */
    public void deAuthenticate(UserPrincipal user);
    
    /* ------------------------------------------------------------ */
    /** Push role onto a Principal.
     * This method is used to add a role to an existing principal.
     * @param user An existing UserPrincipal or null for an anonymous user.
     * @param role The role to add.
     * @return A new UserPrincipal object that wraps the passed user, but
     * with the added role.
     */
    public UserPrincipal pushRole(UserPrincipal user, String role);


    /* ------------------------------------------------------------ */
    /** Pop role from a Principal.
     * @param user A UserPrincipal previously returned from pushRole
     * @return The principal without the role.  Most often this will be the
     * original UserPrincipal passed.
     */
    public UserPrincipal popRole(UserPrincipal user);
    
}
