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
    private static final String[] headerTags = {
        "H1", "H2", "H3", "H4", "H5", "H6"
    };

    /* ----------------------------------------------------------------- */
    /* Construct a heading and add Element, String or Object
     * @param level The level of the heading
     * @param o The Element, String or Object of the heading.
     */
    public Heading(int level,Object o)
    {
        super((level <= headerTags.length) ? headerTags[level-1] : "H"+level);
        add(o);
    }
}

