// ======================================================================
//  Copyright (C) 2002 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas;

import java.io.Serializable;
import java.security.Principal;
import org.mortbay.util.Code;



/* ---------------------------------------------------- */
/** JAASPrincipal
 * <p>Impl class of Principal interface.
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
 * @version 1.0 Tue Apr 15 2003
 * @author Jan Bartel (janb)
 */
public class JAASPrincipal implements Principal, Serializable
{
    private String name = null;
    
    
    public JAASPrincipal(String userName)
    {
        this.name = userName;
    }


    public boolean equals (Object p)
    {
        if (! (p instanceof Principal))
            return false;

        return getName().equals(((Principal)p).getName());
    }


    public int hashCode ()
    {
        return getName().hashCode();
    }


    public String getName ()
    {
        return this.name;
    }


    public String toString ()
    {
        return getName();
    }
    

    
}

    
