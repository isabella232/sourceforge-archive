// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import java.io.*;
import java.util.*;

/* -------------------------------------------------------------------- */
/** HTML Heading
 */
public class Heading extends Block
{
    /* ----------------------------------------------------------------- */
    /* Construct a heading and add Element, String or Object
     * @param level The leve of the heading
     * @param o The Element, String or Object of the heading.
     */
    public Heading(int level,Object o)
    {
	super("H"+level);
	add(o);
    }
}

