// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.html;


/* -------------------------------------------------------------------- */
/** HTML Link Target.
 * This is a HTML reference (not a CSS Link).
 * @see StyleLink
 */
public class Target extends Block
{

    /* ----------------------------------------------------------------- */
    /** Construct Link.
     * @param target The target name 
     */
    public Target(String target)
    {
        super("A");
        attribute("NAME",target);
    }

    /* ----------------------------------------------------------------- */
    /** Construct Link.
     * @param target The target name 
     * @param link Link Element
     */
    public Target(String target,Object link)
    {
        this(target);
        add(link);
    }
}
