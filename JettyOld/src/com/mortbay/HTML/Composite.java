// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import com.mortbay.Base.*;
import java.io.*;
import java.util.*;

/* -------------------------------------------------------------------- */
/** HTML Composite Element
 * <p>This class is can be used a either an abstract or concrete
 * holder of other HTML elements.
 * Used directly, it allow multiple HTML Elements to be added which
 * are produced sequentially.
 * Derived used of Composite may wrap each contain Element in
 * special purpose HTML tags (e.g. list).
 *
 * <p>Notes<br>
 * Elements are added to the Composite either as HTML Elements or as
 * Strings.  Other objects added to the Composite are converted to Strings
 * @see class Element
 * @version $Id$
 * @author Greg Wilkins
*/
public class Composite extends Element
{
    /* ----------------------------------------------------------------- */
    /** The vector of elements in this Composite
     */
    protected Vector elements= new Vector(8,0);

    /* ----------------------------------------------------------------- */
    private Composite nest=null;

    /* ----------------------------------------------------------------- */
    /** Default constructor
     */
    public Composite()
    {}
    
    /* ----------------------------------------------------------------- */
    /** Default constructor
     */
    public Composite(String attributes)
    {
        super(attributes);
    }

    /* ----------------------------------------------------------------- */
    /** Add an Object to the Composite by converting it to a Element or
     * String
     * @param o The Object to add. If it is a String or Element, it is
     * added directly, otherwise toString() is called.
     * @return This Composite (for chained commands)
     */
    public Composite add(Object o)
    {
        if (nest!=null)
            nest.add(o);
        else
        {
            if (o!=null)
            {
                if (o instanceof Element)
                {
                    Code.assert(!(o instanceof Page),
                                "Can't insert Page in Composite");
                    elements.addElement(o);
                }
                else if (o instanceof String)
                    elements.addElement(o);
                else 
                    elements.addElement(o.toString());
            }
        }
        return this;
    }
    
    /* ----------------------------------------------------------------- */
    /** Nest a Composite within a Composite
     * The passed Composite is added to this Composite. Adds to
     * this composite are actually added to the nested Composite.
     * Calls to nest are passed the nested Composite
     * @return The Composite to unest on to return to the original
     * state.
     */
    public Composite nest(Composite c)
    {
        if (nest!=null)
            return nest.nest(c);
        else
        {
            add(c);
            nest=c;
        }
        return this;
    }

    /* ----------------------------------------------------------------- */
    /** Explicit set of the Nested component
     * No add is performed. setNest() obeys any current nesting and
     * sets the nesting of the nested component.
     */
    public Composite setNest(Composite c)
    {
        if (nest!=null)
            nest.setNest(c);
        else
            nest=c;
        return this;
    }
    
    /* ----------------------------------------------------------------- */
    /** Recursively unnest the composites
     */
    public Composite unnest()
    {
        if (nest!=null)
            nest.unnest();
        nest = null;
        return this;
    }


    /* ----------------------------------------------------------------- */
    /** The number of Elements in this Composite
     * @return The number of elements in this Composite
     */
    public int size()
    {
        return elements.size();
    }
    
    /* ----------------------------------------------------------------- */
    /** Write the composite.
     * The default implementation writes the elements sequentially. May
     * be overridden for more specialized behaviour.
     * @param out Writer to write the element to.
     */
    public void write(Writer out)
         throws IOException
    {
        for (int i=0; i <elements.size() ; i++)
        {
            Object element = elements.elementAt(i);
          
            if (element instanceof Element)
                ((Element)element).write(out);
            else if (element==null)
                out.write("null");
            else 
                out.write(element.toString());
        }
    }
    
    /* ----------------------------------------------------------------- */
    /** Contents of the composite.
     */
    public String contents()
    {
        StringBuffer buf = new StringBuffer();
        synchronized(buf)
        {
            for (int i=0; i <elements.size() ; i++)
            {
                Object element = elements.elementAt(i);
                if (element==null)
                    buf.append("null");
                else 
                    buf.append(element.toString());
            }
        }
        return buf.toString();
    }

    /* ------------------------------------------------------------ */
    /** Empty the contents of this Composite 
     */
    public Composite reset()
    {
        elements.removeAllElements();
        return unnest();
    }
    
    /* ----------------------------------------------------------------- */
    /* Flush is a package method used by Page.flush() to locate the
     * most nested composite, write out and empty its contents.
     */
    void flush(Writer out)
         throws IOException
    {
        if (nest!=null)
            nest.flush(out);
        else
        {
            write(out);
            elements.removeAllElements();
        }
    }
    
    /* ----------------------------------------------------------------- */
    /* Flush is a package method used by Page.flush() to locate the
     * most nested composite, write out and empty its contents.
     */
    void flush(OutputStream out)
         throws IOException
    {
        flush(new OutputStreamWriter(out,"UTF8"));
    }
}
