// ======================================================================
// Copyright (C) 2003 by Mortbay Consulting Ltd
// $Id$ 
// ======================================================================

package org.mortbay.jaas;

import java.security.Principal;


public class JAASRole extends JAASPrincipal
{
    
    public JAASRole(String name)
    {
        super (name);
    }

    public boolean equals (Object o)
    {
        if (! (o instanceof Principal))
            return false;

        return getName().equals(((Principal)o).getName());
    }
}
