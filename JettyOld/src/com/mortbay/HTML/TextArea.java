// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import java.io.*;
import java.util.*;

/* -------------------------------------------------------------------- */
/** A Text Area within a form
 * <p> The text in the TEXTAREA is handled by the super class, Text
 * @see com.mortbay.HTML.Text
 */
public class TextArea extends Block
{
    /* ----------------------------------------------------------------- */
    /** @param name The name of the TextArea within the form */
    public TextArea(String name)
    {
	super("TEXTAREA");
	attribute("NAME",name);
    }

    /* ----------------------------------------------------------------- */
    /** @param name The name of the TextArea within the form
     * @param s The string in the text area */
    public TextArea(String name, String s)
    {
	this(name);
	add(s);
    }

    /* ----------------------------------------------------------------- */
    public TextArea setSize(int chars,int lines)
    {
	attribute("ROWS",lines);
	attribute("COLS",chars);
	return this;
    }
}

