// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTML;

import java.io.*;

/* -------------------------------------------------------------------- */
/** HTML Style Block
 */
public class Style extends Block
{
    public static final String text_css = "text/css";

    /* ------------------------------------------------------------ */
    /** Construct a Style element
     * @param type Format of Style */
    public Style(String style, String type)
    {
	super("STYLE");
	attribute("TYPE",type);
	add(style);
    }

    /* ------------------------------------------------------------ */
    /** Construct a Style element */
    public Style(String style)
    {
	this(style, text_css);
    }
};

