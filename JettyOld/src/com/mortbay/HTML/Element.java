// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTML;
import com.mortbay.Base.*;
import java.util.*;
import java.io.*;

/* -------------------------------------------------------------------- */
/** HTML Element
 * <p>This abstract class is the base for all HTML Elements.
 * The feature of an abstract HTML Element is that it can be added to
 * HTML Pages, HTML Composites and several other HTML Elements derivations.
 * Elements may also have attributes set, which are handled by the derived
 * Element.
 * @see class Page, class Composite
 * @version $Id$
 * @author Greg Wilkins
*/
public abstract class Element
{
    /* ----------------------------------------------------------------- */
    public static final String
        noAttributes="",
	ALIGN="ALIGN",
	LEFT="LEFT",
	RIGHT="RIGHT",
	CENTER="CENTER",
	VALIGN="VALIGN",
	TOP="TOP",
	BOTTOM="BOTTOM",
	MIDDLE="MIDDLE",
	WIDTH="WIDTH",
	HEIGHT="HEIGHT",
	SIZE="SIZE",
	COLOR="COLOR",
	BGCOLOR="BGCOLOR",
	STYLE="STYLE",
	CLASS="CLASS",
	ID="ID";
    
	
    
    /* ----------------------------------------------------------------- */
    /** Dimensions >=0 if set*/
    private int width=-1;
    private int height=-1;
    private int size=-1;

    /* ----------------------------------------------------------------- */
    /** The space separated string of HTML element attributes
     */
    private String attributes=null;
    protected Hashtable attributeMap=null;

    /* ----------------------------------------------------------------- */
    /** Default constructor
     */
    public Element(){}

    /* ----------------------------------------------------------------- */
    /** Construct with attributes
     * @param attributes The initial attributes of the element
     */
    public Element(String attributes)
    {
	attributes(attributes);
    }

    /* ----------------------------------------------------------------- */
    /** Write element to a Writer
     * This abstract method is called by the Page or other containing
     * Element to write the HTML for this element. This must be implemented
     * by the derived Element classes.
     * @param out Writer to write the element to.
     */
    public abstract void write(Writer out)
	 throws IOException;

    /* ----------------------------------------------------------------- */
    /** Write Element to an OutputStream
     * Calls print(Writer) and checks errors
     * Elements that override this method should also override
     * write(Writer) to avoid infinite recursion.
     * @param out OutputStream to write the element to.
     */
    public void write(OutputStream out)
	 throws IOException
    {
	Writer writer = new OutputStreamWriter(out);
	write(writer);
	writer.flush();
    }

    /* ----------------------------------------------------------------- */
    public String attributes()
    {
	if (attributes==null && attributeMap==null)
	    return noAttributes;

	StringBuffer buf = new StringBuffer(128);
	synchronized(buf)
	{
	    if(attributes!=null && attributes.length()>0)
	    {
		buf.append(' ');
		buf.append(attributes);
	    }
	
	    if (attributeMap!=null)
	    {
		Enumeration e = attributeMap.keys();
		while (e.hasMoreElements())
		{
		    buf.append(' ');
		    String a = (String)e.nextElement();
		    buf.append(a);
		    buf.append('=');
		    buf.append(attributeMap.get(a).toString());
		}
	    }
	}

	return buf.toString();
    }

    /* ----------------------------------------------------------------- */
    /** Add element Attributes
     * The attributes are added to the Element attributes (separated with
     * a space). The attributes are available to the derived class in the
     * protected member String <I>attributes</I>
     * @deprecated Use attribute(String).
     * @param attributes String of HTML attributes to add to the element.
     * @return This Element so calls can be chained.
     */
    public Element attributes(String attributes)
    {
	if (Code.debug() && attributes.indexOf("=")>=0)
	    Code.warning("Set attribute with old method: "+attributes+
			 " on " + getClass().getName());
	
	if (this.attributes==null)
	    this.attributes=attributes;
	else
	    this.attributes += ' '+attributes;
	return this;
    }
    
    /* ----------------------------------------------------------------- */
    /** Add element Attributes
     * The attributes are added to the Element attributes (separated with
     * a space). The attributes are available to the derived class in the
     * protected member String <I>attributes</I>
     * @param attributes String of HTML attributes to add to the element.
     * @return This Element so calls can be chained.
     */
    public Element attribute(String attributes)
    {
	if (Code.debug() && attributes.indexOf("=")>=0)
	    Code.warning("Set attribute with old method: "+attributes+
			 " on " + getClass().getName());
	
	if (this.attributes==null)
	    this.attributes=attributes;
	else
	    this.attributes += ' '+attributes;
	return this;
    }
    
    /* ----------------------------------------------------------------- */
    /** Add quoted element Attributes and value.
     * @param attribute String of HTML attribute tag
     * @param value String value of the attribute to be quoted
     * @return This Element so calls can be chained.
     */
    public Element attribute(String attribute, Object value)
    {
	if (attributeMap==null)
	    attributeMap=new Hashtable(10);
	
	if (value!=null)
	    attributeMap.put(attribute,"\""+value+'"');
	return this;
    }
    
    /* ----------------------------------------------------------------- */
    /** Add quoted element Attributes and value.
     * @param attribute String of HTML attribute tag
     * @param value String value of the attribute to be quoted
     * @return This Element so calls can be chained.
     */
    public Element attribute(String attribute, long value)
    {
	if (attributeMap==null)
	    attributeMap=new Hashtable(10);
	
	attributeMap.put(attribute,Long.toString(value));
	return this;
    }

    /* ----------------------------------------------------------------- */
    /** Convert Element to String.
     * Uses write() to convert the HTML Element to a string.
     * @param String of the HTML element
     */
    public String toString()
    {
	try{
	    StringWriter out = new StringWriter();
	    write(out);
	    out.flush();
	    return out.toString();
	}
	catch(IOException e){
	    Code.ignore(e);
	}
	return null;	
    }
    
    /* ----------------------------------------------------------------- */
    /** left justify
     * Convenience method equivalent to attribute("ALIGN","LEFT"). Not
     * applicable to all Elements.
     */
    public Element left()
    {
	return attribute(ALIGN,LEFT);
    }
    
    /* ----------------------------------------------------------------- */
    /** right justify
     * Convenience method equivalent to attribute("ALIGN","RIGHT"). Not
     * applicable to all Elements.
     */
    public Element right()
    {
	return attribute(ALIGN,RIGHT);
    }
    
    /* ----------------------------------------------------------------- */
    /** Center
     * Convenience method equivalent to attribute("ALIGN","CENTER"). Not
     * applicable to all Elements.
     */
    public Element center()
    {
	return attribute(ALIGN,CENTER);
    }
    
    /* ----------------------------------------------------------------- */
    /** Top align
     * Convenience method equivalent to attribute("VALIGN","TOP"). Not
     * applicable to all Elements.
     */
    public Element top()
    {
	return attribute(VALIGN,TOP);
    }
    
    /* ----------------------------------------------------------------- */
    /** Bottom align
     * Convenience method equivalent to attribute("VALIGN","BOTTOM"). Not
     * applicable to all Elements.
     */
    public Element bottom()
    {
	return attribute(VALIGN,BOTTOM);
    }
    
    /* ----------------------------------------------------------------- */
    /** Middle align
     * Convenience method equivalent to attribute("VALIGN","MIDDLE"). Not
     * applicable to all Elements.
     */
    public Element middle()
    {
	return attribute(VALIGN,MIDDLE);
    }
    
    /* ----------------------------------------------------------------- */
    /** set width
     * Convenience method equivalent to attribute("WIDTH",w). Not
     * applicable to all Elements.
     */
    public Element width(int w)
    {
	width=w;
	return attribute(WIDTH,w);
    }
    
    /* ----------------------------------------------------------------- */
    /** set width
     * Convenience method equivalent to attribute("WIDTH",w). Not
     * applicable to all Elements.
     */
    public Element width(String w)
    {
	width=-1;
	return attribute(WIDTH,w);
    }
    
    /* ----------------------------------------------------------------- */
    public int width()
    {
        return width;
    }
    
    /* ----------------------------------------------------------------- */
    /** set height
     * Convenience method equivalent to attribute("HEIGHT",h). Not
     * applicable to all Elements.
     */
    public Element height(int h)
    {
	height=h;
	return attribute(HEIGHT,h);
    }
    
    /* ----------------------------------------------------------------- */
    /** set height
     * Convenience method equivalent to attribute("HEIGHT",h). Not
     * applicable to all Elements.
     */
    public Element height(String h)
    {
	height=-1;
	return attribute(HEIGHT,h);
    }
    
    /* ----------------------------------------------------------------- */
    public int height()
    {
        return height;
    }
    
    /* ----------------------------------------------------------------- */
    /** set size
     * Convenience method equivalent to attribute("SIZE",s). Not
     * applicable to all Elements.
     */
    public Element size(int s)
    {
	size=s;
	return attribute(SIZE,s);
    }
    
    /* ----------------------------------------------------------------- */
    /** set size
     * Convenience method equivalent to attribute("SIZE",s). Not
     * applicable to all Elements.
     */
    public Element size(String s)
    {
	size=-1;
	return attribute(SIZE,s);
    }
    
    /* ----------------------------------------------------------------- */
    public int size()
    {
        return size;
    }
    
    /* ----------------------------------------------------------------- */
    /** set color
     * Convenience method equivalent to attribute("COLOR",color). Not
     * applicable to all Elements.
     */
    public Element color(String color)
    {
	return attribute(COLOR,color);
    }
    
    /* ----------------------------------------------------------------- */
    /** set BGCOLOR
     * Convenience method equivalent to attribute("BGCOLOR",color). Not
     * applicable to all Elements.
     */
    public Element bgColor(String color)
    {
	return attribute(BGCOLOR,color);
    }
    
    /* ----------------------------------------------------------------- */
    /** set CSS CLASS
     */
    public Element cssClass(String c)
    {
	return attribute(CLASS,c);
    }
    
    /* ----------------------------------------------------------------- */
    /** set CSS ID
     */
    public Element cssID(String id)
    {
	return attribute(ID,id);
    }
    
    /* ----------------------------------------------------------------- */
    /** set Style
     */
    public Element style(String s)
    {
	return attribute(STYLE,s);
    }
}




