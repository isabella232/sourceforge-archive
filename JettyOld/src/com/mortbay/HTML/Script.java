// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================

package com.mortbay.HTML;

import java.io.*;

/* -------------------------------------------------------------------- */
/** HTML Script Block
 */
public class Script extends Block
{
    public static final String javascript = "JavaScript";

    /* ------------------------------------------------------------ */
    /** Construct a script element
     * @param lang Language of Script */
    public Script(String script, String lang)
    {
        super("SCRIPT");
        attribute("LANGUAGE",lang);
        add(script);
    }

    /* ------------------------------------------------------------ */
    /** Construct a JavaScript script element */
    public Script(String script)
    {
        this(script, javascript);
    }
};


