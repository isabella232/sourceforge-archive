// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import java.io.*;
import java.util.*;


/* -------------------------------------------------------------------- */
/** HTML List Block
 * Each Element added to the List (which is a Composite) is treated
 * as a new List Item.
 * @see  com.mortbay.HTML.Block
 */
public class List extends Block
{
    /* ----------------------------------------------------------------- */
    public static final String Unordered="UL";
    public static final String Ordered="OL";
    public static final String Menu="MENU";
    public static final String Directory="DIR";
    
    /* ----------------------------------------------------------------- */
    public List(String type)
    {
        super(type);
    }   
    
    /* ----------------------------------------------------------------- */
    public Composite add(Object o)
    {
        super.add("<LI>");
        super.add(o);
        return this;
    }
}


