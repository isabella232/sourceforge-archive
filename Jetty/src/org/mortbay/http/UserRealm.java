// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.http;
import java.security.Principal;

/* ------------------------------------------------------------ */
/** User Realm.
 *
 * This interface should be specialized to provide specific user
 * lookup and authentication using arbitrary methods.
 *
 * For SSO implementation sof UserRealm should also implement SSORealm.
 *
 * @see SSORealm
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public interface UserRealm
{
    /* ------------------------------------------------------------ */
    public String getName();

    /* ------------------------------------------------------------ */
    /** Get the principal for a username
     */
    public Principal getUserPrincipal(String username);
    
    /* ------------------------------------------------------------ */
    /** Authenticate a users credentials.
     * Implementations of this method may adorn the calling context to
     * assoicate it with the authenticated principal (eg ThreadLocals). If
     * such context associations are made, they should be considered valid
     * until a UserRealm.deAuthenticate(UserPrincipal) call is made for this
     * UserPrincipal.
     * @param username The username. 
     * @param credentials The user credentials, normally a String password. 
     * @param request The request to be authenticated. Additional
     * parameters may be extracted or set on this request as needed
     * for the authentication mechanism (none required for BASIC and
     * FORM authentication).
     * @return The authenticated UserPrincipal.
     */
    public Principal authenticate(String username,
                                  Object credentials,
                                  HttpRequest request);

    /* ------------------------------------------------------------ */
    /** Check authentication status.
     * 
     * Implementations of this method may adorn the calling context to
     * assoicate it with the authenticated principal (eg ThreadLocals). If
     * such context associations are made, they should be considered valid
     * until a UserRealm.deAuthenticate(UserPrincipal) call is made for this
     * UserPrincipal.
     *
     * @return True if this user is still authenticated.
     */
    public boolean isAuthenticated(Principal user);
    
    /* ------------------------------------------------------------ */
    /** Check if the user is in a role. 
     * @param role A role name.
     * @return True if the user can act in that role.
     */
    public boolean isUserInRole(Principal user, String role);
    
    /* ------------------------------------------------------------ */
    /** Dissassociate the calling context with a Principal.
     * This method is called when the calling context is not longer
     * associated with the Principal.  It should be used by an implementation
     * to remove context associations such as ThreadLocals.
     * The UserPrincipal object remains authenticated, as it may be
     * associated with other contexts.
     * @param user A UserPrincipal allocated from this realm.
     */
    public void disassociate(Principal user);
    
    /* ------------------------------------------------------------ */
    /** Push role onto a Principal.
     * This method is used to add a role to an existing principal.
     * @param user An existing UserPrincipal or null for an anonymous user.
     * @param role The role to add.
     * @return A new UserPrincipal object that wraps the passed user, but
     * with the added role.
     */
    public Principal pushRole(Principal user, String role);


    /* ------------------------------------------------------------ */
    /** Pop role from a Principal.
     * @param user A UserPrincipal previously returned from pushRole
     * @return The principal without the role.  Most often this will be the
     * original UserPrincipal passed.
     */
    public Principal popRole(Principal user);
    
}
