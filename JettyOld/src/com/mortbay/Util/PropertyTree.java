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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.StringTokenizer;

public class PropertyTree extends Properties
{
    /* ------------------------------------------------------------ */
    private Hashtable values = null;
    private Hashtable subNodes = null;
    private PropertyTree defaultValues = null;
    private Object defaultValue = null;
    /* ------------------------------------------------------------ */
    public PropertyTree(){}
    /* ------------------------------------------------------------ */
    /** Private constructor for building sub-nodes
     * @param defaults Whether this represents a set of default values
     */
    private PropertyTree(boolean defaults){
	// This way, if we can't find a value on lookup, we skip it and pass
	// over to our defaultVals, i.e. we recurse over ourself down the
	// path...
	if (defaults) defaultValues = this;
    }
    /* ------------------------------------------------------------ */
    /** Override Hashtable.get() */
    public Object get(Object key){
	if (key != null){
	    Vector v = new Vector();
	    StringTokenizer tokens = new StringTokenizer(key.toString(), ".");
	    while (tokens.hasMoreTokens())
		v.addElement(tokens.nextToken());
	    return getValue(0, v);
	}
	return null;
    }
    /* ------------------------------------------------------------ */
    /** Override Hashtable.put() */
    public synchronized Object put(Object key, Object value){
	PropertyTree node = this;
	// Get the next token...
	StringTokenizer tokens = new StringTokenizer(key.toString(), ".");
	String token = tokens.nextToken();
	while (tokens.hasMoreTokens())
	{
	    if (token.equals("*"))
		node = node.buildDefault();
	    else
		node = node.buildNode(token);
	    token = tokens.nextToken();
	}
	// now deal with the last token...
	if (token.equals("*")) {
	    // A trailing * will match anything, add a default node and
	    // set a default value...
	    node = node.buildDefault();
	}
	return node.valuePut(token, value);
    }
    /* ------------------------------------------------------------ */
    /* Lookup a value on this node */
    private Object valueGet(String key){
	Object retv = null;
	if (values != null)
	    retv = values.get(key);
	if (retv == null)
	    retv = defaultValue;
	return retv;
    }
    /* ------------------------------------------------------------ */
    /* Store a value on this node
     * Storing the key "*" will set a defaultValue
     */
    private Object valuePut(Object key, Object value){
	if ("*".equals(key)) {
	    Object retv = defaultValue;
	    defaultValue = value;
	    return retv;
	}
	if (values == null)
	    values = new Hashtable();
	return values.put(key, value);
    }
    /* ------------------------------------------------------------ */
    /* Lookup a value in the tree recursively
     * @param index The index into key
     * @param key A list of the key elements
     * @return the value
     */
    private Object getValue(int index, Vector key){
	String elem = key.elementAt(index).toString();
	Object retv = null;
	if (index + 1 == key.size()){
	    retv = valueGet(elem);
	    return retv;
	}
	if (subNodes != null){
	    PropertyTree subNode = (PropertyTree)subNodes.get(elem);
	    if (subNode != null)
		retv = subNode.getValue(index+1, key);
	}
	if (retv == null && defaultValues != null)
	    retv = defaultValues.getValue(index+1, key);
	return retv;
    }
    /* ------------------------------------------------------------ */
    /* Build (if not already built) and return a default values PropertyTree */
    private synchronized PropertyTree buildDefault(){
	if (defaultValues == null)
	    defaultValues = new PropertyTree(true);
	return defaultValues;
    }
    /* ------------------------------------------------------------ */
    /* Build (if not already built) and return a names sub PropertyTree */
    private synchronized PropertyTree buildNode(String name){
	if (subNodes == null)
	    subNodes = new Hashtable();
	PropertyTree sn = (PropertyTree)subNodes.get(name);
	if (sn == null){
	    sn = new PropertyTree(false);
	    subNodes.put(name, sn);
	}
	return sn;
    }
    /* ------------------------------------------------------------ */
    /** From Properties */
    public String getProperty(String key){
	return (String)get(key);
    }
    /** From Properties */
    public String getProperty(String key,
  			      String defaultValue){
	String retv = (String)get(key);
	if (retv == null) retv = defaultValue;
	return retv;
    }
    /* ------------------------------------------------------------ */
    private void listValues(String prefix, PrintWriter writer,
			    String postfix){
 	if (values != null)
	    for (Enumeration enum = values.keys(); enum.hasMoreElements();){
		String key = enum.nextElement().toString();
		writeKeyValue(prefix, writer, postfix, key,
			      values.get(key).toString());
	    }
	if (defaultValue != null)
	    writeKeyValue(prefix, writer, postfix, "*",
			  defaultValue.toString());
	if (defaultValues != null && defaultValues != this)
	    defaultValues.listValues(prefix+"*.", writer, postfix);
	if (subNodes != null)
	    for (Enumeration enum = subNodes.keys(); enum.hasMoreElements();){
		String key = enum.nextElement().toString();
		PropertyTree sn = (PropertyTree)subNodes.get(key);
		sn.listValues(prefix+key+".", writer, postfix);
	    }
    }
    /* ------------------------------------------------------------ */
    protected static void writeKeyValue(String prefix, PrintWriter writer,
					String postfix, String key,
					String value){
	writer.print(prefix);
	writer.print(key);
	writer.print("=");
	writer.print(value);
	writer.print(postfix);
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
    private void listNames(String prefix, Vector into){
 	if (values != null)
	    for (Enumeration enum = values.keys(); enum.hasMoreElements();)
		into.addElement(prefix+enum.nextElement());
	if (subNodes != null)
	    for (Enumeration enum = subNodes.keys(); enum.hasMoreElements();){
		String key = enum.nextElement().toString();
		PropertyTree sn = (PropertyTree)subNodes.get(key);
		sn.listNames(prefix+key+".", into);
	    }
    }
    /* ------------------------------------------------------------ */
    /** Return a list of the properties with values on this Tree Node
     * i.e. Property names with no "."'s in them
     */
    public synchronized Enumeration valueNames(){
	if (values == null)
	    values = new Hashtable();
	return values.keys();
    }
    /* ------------------------------------------------------------ */
    /** Return a list of the names of sub-PropertyTrees nodes of this node */
    public synchronized Enumeration nodeNames(){
	if (subNodes == null)
	    subNodes = new Hashtable();
	return subNodes.keys();
    }
    /* ------------------------------------------------------------ */
    /** Return a list of the fully-qualified names of the properties in this
	tree */
    public Enumeration propertyNames(){
	Vector v = new Vector();
	listNames("", v);
	return v.elements();
    }
    /* ------------------------------------------------------------ */
    /** Retrurn a sub node of this PropertyTree
     * @param name The name of the sub node
     * @return null if none.
     */
    public PropertyTree getNode(String name){
	if (subNodes != null)
	    return (PropertyTree)subNodes.get(name);
	return null;
    }
    /* ------------------------------------------------------------ */
    public void list(PrintStream out){
	PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
	list(writer);
    }
    public void list(PrintWriter out){
	listValues("", out, "\n");
	out.flush();
    }
    /* ------------------------------------------------------------ */
    public String toString(){
	StringWriter sw = new StringWriter();
	PrintWriter writer = new PrintWriter(sw);
	listValues("", writer, "\n");
	writer.flush();
	return sw.toString();
    }
    /* ------------------------------------------------------------ */
};
