// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import java.io.*;
import java.util.*;

/* --------------------------------------------------------------------- */
/** Composite Factory
 * Abstract interface for production of composites
 */
public interface CompositeFactory
{
    public Composite newComposite();
}


