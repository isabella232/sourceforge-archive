// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import java.io.*;
import java.util.*;

/* -------------------------------------------------------------------- */
/** HTML Form
 * The speicalized Block can contain HTML Form elements as well as
 * any other HTML elements
 */
public class Form extends Block
{
    private String method="POST";

    /* ----------------------------------------------------------------- */
    /** Constructor
     */
    public Form()
    {
	super("FORM");
    }

    /* ----------------------------------------------------------------- */
    /** Constructor
     * @param submitURL The URL to submit the form to
     */
    public Form(String submitURL)
    {
	super("FORM");
	action(submitURL);
    }

    /* ----------------------------------------------------------------- */
    /** Constructor
     * @param submitURL The URL to submit the form to
     */
    public Form action(String submitURL)
    {
	attribute("ACTION",submitURL);
	return this;
    }
    
    /* ----------------------------------------------------------------- */
    /** Set the form target
     */
    public Form target(String t)
    {
	attribute("TARGET",t);
	return this;
    }
    
    /* ----------------------------------------------------------------- */
    /** Set the form method
     */
    public Form method(String m)
    {
	method=m;
	return this;
    }
    
    /* ----------------------------------------------------------------- */
    public void write(Writer out)
	 throws IOException
    {
	attribute("METHOD",method);
	super.write(out);
    }
}




