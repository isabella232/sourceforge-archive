// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.html;


/* -------------------------------------------------------------------- */
/** HTML List Block.
 * Each Element added to the List (which is a Composite) is treated
 * as a new List Item.
 * @see  org.mortbay.html.Block
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
    /** 
     * @param o The item
     * @return This List.
     */
    public Composite add(Object o)
    {
        super.add("<LI>");
        super.add(o);
        return this;
    }
    
    /* ----------------------------------------------------------------- */
    /** 
     * @return The new Item composite
     */
    public Composite newItem()
    {
        super.add("<LI>");
        Composite composite=new Composite();
        super.add(composite);
        return composite;
    }

    
}






