// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.StringTokenizer;

/* ------------------------------------------------------------ */
/** Extension of Properties to allow nesting of Properties and default values
 * This class extends Properties and uses a "." separated notation to allow
 * nesting of property values. Property keys such as "a.b" and "a.c" cat be
 * retrieved (and set) as normal, but it is possible to retrieve a
 * PropertyTree by get("a"), and then use get("b") and get("c") to achieve
 * the same thing (although most times this is unneccessary). This makes it
 * easy to have nested sets of values in the same properties file and iterate
 * over nested keys.
 *
 * <p>The "*" can also be used as a default key and when matched, its value
 * will be returned if there is no explicitly matching key.
 * <br>E.G<pre>
 * a.b.* = foo      # Match anything starting in "a.b"
 * *.c = bar        # Match anything ending in "c"
 * a.*.c = flob     # Match anything starting in "a" that ends in a "c"
 * * = blob         # Match anything
 * </pre>
 * All the standard Properties methods work as usual, but keys such as
 * "a.b.c" can be used to retrieve nested values. The Properties methods,
 * <code>propertyNames</code> only returns proper key names, not default
 * keys, but <code>list</code> and <code>save</code> will write out the
 * default keys and their values.
 *
 * <p> This class adds methods to navigate over different elements of the
 * tree: <ul>
 *   <li> <code>keys</code>: Return an Enumeration of the names of the
 * direct children of this tree.
 *   <li> <code>getNode</code>: Retrieve a direct child node of this
 * PropertyTree by name (if it exists).
 * </ul>
 *
 * <p> To aid in constructing and saving Properties files,
 * <code>getConverter</code> will convert Dictionaries into PropertyTrees
 * recursively.
 *
 * <strong>Note:</strong> If keys for "a.b.c=xxx" and "a.b=xxx" exist, then
 * doing a get("a.b") will return the value for "a.b" rather than returning
 * the PropertyTree representing "a.b". Because of this it is recommended
 * that Properties files not be written to contain such keys where nested
 * PropertyTrees need to be retrieved.
 */
public class PropertyTree extends Properties
{
    /* ------------------------------------------------------------ */
    private Hashtable children = new Hashtable();
    private Object value = null;
    // The default values
    private PropertyTree defaultValues = null;
    
    /* ------------------------------------------------------------ */
    public PropertyTree()
    {}
    
    /* ------------------------------------------------------------ */
    /** Private constructor for building sub-nodes
     * @param defaults Whether this represents a set of default values
     */
    private PropertyTree(boolean defaults)
    {
	// This way, if we can't find a value on lookup, we skip it and pass
	// over to our defaultVals, i.e. we recurse over ourself down the
	// path...
	if (defaults) defaultValues = this;
    }
    
    /* ------------------------------------------------------------ */
    /** Override Hashtable.get() */
    public Object get(Object key)
    {
	Code.debug("Get ",key,"=",value);
	Vector v = getTokens(key.toString());
	if (v != null)
	    return getValue(0, v);
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /** Override Hashtable.put() */
    public synchronized Object put(Object key, Object value)
    {
	Code.debug("Put ",key,"=",value);
	Vector v = getTokens(key.toString());
	if (v != null)
	    return putValue(0, v, value);
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /** Override Hashtable.remove() */
    public Object remove(Object key)
    {
	return put(key, null);
    }
    
    /* ------------------------------------------------------------ */
    /** From Properties */
    public String getProperty(String key)
    {
	return (String)get(key);
    }
    
    /* ------------------------------------------------------------ */
    /** From Properties */
    public String getProperty(String key,
  			      String defaultValue)
    {
	String retv = (String)get(key);
	if (retv == null)
	    retv = defaultValue;
	return retv;
    }
    
    /* ------------------------------------------------------------ */
    /** From Properties */
    public synchronized void save(OutputStream out,
				  String header)
    {
	PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
	writer.print("# ");
	writer.println(header);
	list(writer);
    }
    
    /* ------------------------------------------------------------ */
    /** Return a list of the fully-qualified names of the properties in this
     * tree
     */
    public Enumeration propertyNames()
    {
	 Vector v = new Vector();
	 listNames("", v);
	 return v.elements();
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration keys()
    {
	return children.keys();
    }
    
    /* ------------------------------------------------------------ */
    /** Return a sub node of this PropertyTree
     * @param name The name of the sub node
     * @return null if none.
     */
    public PropertyTree getTree(String key)
    {
	Vector v = getTokens(key.toString());
	if (v != null)
	    return getSubTree(0, v);
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /** To remove nested PropertyTrees */
    public Object removeTree(Object key)
    {
	Vector v = getTokens(key.toString());
	if (v != null)
	    return removeSubTree(0, v);
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public void list(PrintStream out)
    {
	PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
	list(writer);
    }
    
    /* ------------------------------------------------------------ */
    public void list(PrintWriter out)
    {
	listValues("", out, "\n");
	out.flush();
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
	StringWriter sw = new StringWriter();
	PrintWriter writer = new PrintWriter(sw);
	listValues("", writer, "\n");
	writer.flush();
	return sw.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Turn the key into a list of tokens */
    private static Vector getTokens(String key)
    {
	if (key != null)
	{
	    Vector v = new Vector();
	    StringTokenizer tokens = new StringTokenizer(key.toString(), ".");
	    while (tokens.hasMoreTokens())
		v.addElement(tokens.nextToken());
	    return v;
	}
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /* Lookup a value in the tree recursively
     * @param index The index into key
     * @param key A list of the key elements
     * @return the value
     */
    private Object getValue(int index, Vector key)
    {
	if (index == key.size())
	    // Must be us! Return our value...
	    return value;
	String elem = key.elementAt(index).toString();
	Object val = children.get(elem);
	PropertyTree subNode = null;
	if (val != null)
	{
	    if (val instanceof PropertyTree)
	    {
		subNode = (PropertyTree)val;
		val = subNode.getValue(index + 1, key);
		if (val != null) return val;
	    }
	    else if (index + 1 == key.size())
	    {
		// val must be what they want...
		return val;
	    }
	}
	
	if (defaultValues == null)
	    return null;
	return defaultValues.getValue(index + 1, key);
    }
    
    /* ------------------------------------------------------------ */
    /* Put a value in the tree recursively
     * @param index The index into key
     * @param key A list of the key elements
     * @return the old value
     */
    private Object putValue(int index, Vector key, Object value)
    {
	Object retv = this.value;
	if (index == key.size())
	{
	    // Must be us!
	    this.value = value;
	    return retv;
	}
	
	String elem = key.elementAt(index).toString();
	// Special case handling if they are trying to put a PropertyTree.
	if (value instanceof PropertyTree && index + 1 == key.size())
	{
	    if (elem.equals("*"))
	    {
		retv = defaultValues;
		defaultValues = (PropertyTree)value;
		return retv;
	    }
	    return children.put(elem, value);
	}
	
	Object val = children.get(elem);
	// Check if there is a node there already...
	if (val != null && val instanceof PropertyTree)
	    return ((PropertyTree)val).putValue(index + 1, key, value);
	// Optimisation
	if (index + 1 == key.size() && !elem.equals("*"))
	{
	    // one of our children!
	    if (value == null)
		return children.remove(elem);
	    else
		return children.put(elem, value);
	}
	// go to a sub-node
	PropertyTree subnode = getSubNode(elem);
	return subnode.putValue(index + 1, key, value);
    }
    
    /* ------------------------------------------------------------ */
    /* retrieve a PropertyTree */
    private PropertyTree getSubTree(int index, Vector key)
    {
	if (index == key.size())
	    return this;
	String elem = key.elementAt(index).toString();
	PropertyTree subnode = getSubNode(elem, false);
	if (subnode == null) return null;
	return subnode.getSubTree(index+1, key);
    }
    
    /* ------------------------------------------------------------ */
    private PropertyTree removeSubTree(int index, Vector key)
    {
	if (index == key.size())
	{
	    children.clear();
	    if (defaultValues != this) defaultValues = null;
	    return this;
	}
	String elem = key.elementAt(index).toString();
	PropertyTree subnode = getSubNode(elem, false);
	if (subnode == null) return null;
	if (index + 1 == key.size() && subnode.value == null){
	    children.remove(elem);
	    return subnode;
	} else {
	    return subnode.removeSubTree(index + 1, key);
	}
    }
    
    /* ------------------------------------------------------------ */
    private PropertyTree getSubNode(String name)
    {
	return getSubNode(name, true);
    }
    
    /* ------------------------------------------------------------ */
    private PropertyTree getSubNode(String name, boolean create)
    {
	boolean defaults = "*".equals(name);
	Object val = null;
	if (defaults)
	{
	    if (defaultValues != null)
		return defaultValues;
	}
	else
	{
	    val = children.get(name);
	    if (val != null && val instanceof PropertyTree)
		return (PropertyTree)val;
	}
	if (create)
	{
	    PropertyTree tree = new PropertyTree(defaults);
	    tree.value = val;
	    if (defaults)
		defaultValues = tree;
	    else
		children.put(name, tree);
	    return tree;
	}
	else
	    return null;
    }
    
    /* ------------------------------------------------------------ */
    private void listValues(String prefix, PrintWriter writer,
			    String postfix)
    {
	String dot = (prefix == null || prefix.equals("")) ? "" : ".";
	if (value != null)
	    writeKeyValue(prefix, writer, postfix, "", value.toString());
	
	for (Enumeration enum = children.keys(); enum.hasMoreElements();)
	{
	    String key = enum.nextElement().toString();
	    Object val = children.get(key);
	    if (val instanceof PropertyTree)
	    {
		PropertyTree tree = (PropertyTree)val;
		tree.listValues(prefix+dot+key, writer, postfix);
	    }
	    else
		writeKeyValue(prefix, writer, postfix, dot + key,
			      val.toString());
	}
	if (defaultValues != null && defaultValues != this)
	    defaultValues.listValues(prefix+dot+"*", writer, postfix);
    }
    
    /* ------------------------------------------------------------ */
    protected static void writeKeyValue(String prefix, PrintWriter writer,
					String postfix, String key,
					String value)
    {
	writer.print(prefix);
	writer.print(key);
	writer.print(": ");
	writer.print(value);
	writer.print(postfix);
    }
    
    /* ------------------------------------------------------------ */
    private void listNames(String prefix, Vector into)
    {
	String dot = (prefix == null || prefix.equals("")) ? "" : ".";
	if (value != null)
	    into.addElement(prefix);
	for (Enumeration enum = children.keys(); enum.hasMoreElements();){
	    String key = enum.nextElement().toString();
	    Object val = children.get(key);
	    if (val instanceof PropertyTree){
		PropertyTree tree = (PropertyTree)val;
		tree.listNames(prefix+dot+key, into);
	    } else
		into.addElement(prefix+dot+key);
	}
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return A Converter for converting Dictionaries into PropertyTrees
     */
    public static Converter getConverter()
    {
	return new Converter(){
	    public Object convert(Object toConvert, Class convertTo,
				  Converter context) {
		if (toConvert.getClass().equals(convertTo))
		    // Already correct type!
		    return toConvert;
		try {
		    if (!convertTo.equals(Class.forName(
					"com.mortbay.Util.PropertyTree")))
			return null;
		} catch (Exception ex){}
		// Make sure we have a dictionary
		if (!(toConvert instanceof Dictionary))
		    return null;
		// Check if already OK
		if (toConvert instanceof PropertyTree)
		    return toConvert;
		PropertyTree pt = new PropertyTree();
		Dictionary dict = (Dictionary)toConvert;
		Converter converter = context == null ? this : context;
		for (Enumeration enum = dict.keys(); enum.hasMoreElements();){
		    Object key = enum.nextElement();
		    Object value = dict.get(key);
		    Object converted =
			converter.convert(value, getClass(), converter);
		    pt.put(key, converted == null ? value : converted);
		}
		return pt;
	    }
	};
    }
};








