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
 * "a.b.c" can be used to retrieve nested values. 
 *
 * <p> To aid in constructing and saving Properties files,
 * <code>getConverter</code> will convert Dictionaries into PropertyTrees
 * recursively.
 */

public class PropertyTree extends Properties
{
    
    /* ------------------------------------------------------------ */
    class Node extends Hashtable
    {
	Node() {super(13);}
	public String key=null;
	public String toString()
	{
	    return  "["+key+":"+super.toString()+"]";
	}
    }
    
    /* ------------------------------------------------------------ */
    private Node rootNode=new Node();

    /* ------------------------------------------------------------ */
    public PropertyTree()
    {}
    
    /* ------------------------------------------------------------ */
    /** Override Hashtable.get() */
    public synchronized Object get(Object key)
    {
	Object realKey=key;
	Object value=super.get(realKey);
	if (value==null)
	{
	    realKey=getTokenKey(key.toString());
	    if (realKey!=null)
		value=super.get(realKey);
	}
	
	Code.debug("Get ",realKey,"(",key,")=",value);
	return value;
    }

    /* ------------------------------------------------------------ */
    /** Override Properties.getProperty() */
    public String getProperty(String key)
    {
	return (String)get(key);
    }
    
    /* ------------------------------------------------------------ */
    /** Override Hashtable.put() */
    public synchronized Object put(Object key, Object value)
    {
	Code.debug("Put ",key,"=",value);
	String keyStr=key.toString();
	putTokenKey(keyStr,keyStr);
	return super.put(key,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Override Properties.getProperty() */
    public String setProperty(String key,String value)
    {
	return (String)put(key,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Override Hashtable.remove() */
    public synchronized Object remove(Object key)
    {
	Object value=super.get(key);
	if (value!=null)
	{
	    Code.debug("Remove ",key);
	    putTokenKey(key.toString(),null);
	    return super.remove(key);
	}

	String realKey=getTokenKey(key.toString());
	if (realKey!=null)
	{
	    Code.debug("Remove ",realKey,"(",key,")");
	    putTokenKey(realKey,null);
	    return super.remove(realKey);
	}
	return null;
    }   
    
    /* ------------------------------------------------------------ */
    /** From Properties */
    public synchronized void save(OutputStream out,String header)
    {
	PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
	writer.print("# ");
	writer.println(header);
	list(writer);
    }
    
    /* ------------------------------------------------------------ */
    /** Return a sub node of this PropertyTree
     * @param name The name of the sub node
     * @return null if none.
     */
    public PropertyTree getTree(String key)
    {
	if (key.indexOf('*')>=0)
	    throw new Error("Can't wildcard subtree");
	return new SubPropertyTree(this,key);
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration propertyNames()
    {
	return keys();
    }

    /* ------------------------------------------------------------ */
    public Enumeration elements()
    {
	final Enumeration keys=keys();
	return new Enumeration(){
	    public boolean hasMoreElements(){
		return keys.hasMoreElements();
	    }
	    public Object nextElement(){
		return get(keys.nextElement());
	    }
	};	
    }
    
    /* ------------------------------------------------------------ */
    public Object clone()
    {
	PropertyTree pt = new PropertyTree();
	Enumeration e = keys();
	while(e.hasMoreElements())
	{
	    String k = (String)e.nextElement();
	    pt.put(k,get(k));
	}
	return pt;
    }
    
    /* ------------------------------------------------------------ */
    private void putTokenKey(String key,String tokenKey)
    {
	Vector tokens=getTokens(key);
	Node node=rootNode;
	int index=0;
	while(index<tokens.size())
	{
	    Node subNode = (Node)node.get(tokens.elementAt(index));
	    if (subNode==null)
	    {
		subNode=new Node();
		node.put(tokens.elementAt(index),subNode);
	    }
	    node=subNode;
	    index++;
	}
	node.key=tokenKey;
    }    

    /* ------------------------------------------------------------ */
    private String getTokenKey(String key)
    {
	Vector tokens=getTokens(key);
	String tokenKey=getTokenKey(rootNode,tokens,0);
	if (Code.verbose(9)) Code.debug(key,"-->",tokenKey);
	return tokenKey;
    }
    
    /* ------------------------------------------------------------ */
    private String getTokenKey(Node node, Vector tokens, int index)
    {
	String key=null;
	if (tokens.size()==index)
	    key=node.key;
	else
	{
	    // expand named nodes
	    Node subNode=(Node)node.get(tokens.elementAt(index));
	    if (subNode!=null)
		key=getTokenKey(subNode,tokens,index+1);

	    // if no key, try wild expansions
	    subNode=(Node)node.get("*");
	    while (subNode!=null && key==null && index<tokens.size())
		key=getTokenKey(subNode,tokens,++index);
	}
	return key;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Turn the key into a list of tokens */
    private static Vector getTokens(String key)
    {
	if (key != null)
	{
	    Vector v = new Vector(10);
	    StringTokenizer tokens = new StringTokenizer(key.toString(), ".");
	    while (tokens.hasMoreTokens())
		v.addElement(tokens.nextToken());
	    return v;
	}
	return null;
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

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    static class SubPropertyTree extends PropertyTree
    {
	public final String prefix;
	public final PropertyTree tree;
	public final Vector tokens;
	public final String[] prefixes;
	public final String[] wilds;
	public Hashtable keyMap=null;
	
	
	/* ------------------------------------------------------------ */
	SubPropertyTree(PropertyTree tree,String node)
	{
	    prefix=node+".";
	    this.tree=tree;
	    tokens=tree.getTokens(node);
	    prefixes=new String[tokens.size()];
	    for (int i=0;i<tokens.size();i++)
		prefixes[i]=((i>0)?prefixes[i-1]:"")+tokens.elementAt(i)+".";
	    wilds=new String[tokens.size()];
	    for (int i=0;i<wilds.length;i++)
		wilds[i]=((i>0)?prefixes[i-1]:"")+"*.";
	}
	
	/* ------------------------------------------------------------ */
	/** Override Hashtable.get() */
	public Object get(Object key)
	{
	    if (keyMap==null)
		this.keys();
	    String k=(String)keyMap.get(key);
	    if (k==null)
		k = prefix+key;
	    return tree.get(k);
	}
    
	/* ------------------------------------------------------------ */
	/** Override Hashtable.put() */
	public Object put(Object key, Object value)
	{
	    keyMap=null;
	    key = prefix+key;
	    return tree.put(key,value);
	}
    
	/* ------------------------------------------------------------ */
	/** Override Hashtable.remove() */
	public Object remove(Object key)
	{
	    key = prefix+key;
	    return tree.remove(key);
	}
    
	/* ------------------------------------------------------------ */
	/** Return a sub node of this PropertyTree
	 * @param name The name of the sub node
	 * @return null if none.
	 */
	public PropertyTree getTree(String key)
	{
	    key = prefix+key;
	    return new SubPropertyTree(tree,key);
	}

	/* ------------------------------------------------------------ */
	public Enumeration keys()
	{
	    keyMap=new Hashtable();
	    Enumeration e = tree.keys();
	    while (e.hasMoreElements())
	    {
		String k=(String)e.nextElement();
		if (k.startsWith(prefix))
		{
		    keyMap.put(k.substring(prefix.length()),k);
		    continue;
		}

		for (int i=wilds.length;i-->0;)
		{
		    if (k.startsWith(wilds[i]))
		    {
			String wk="*."+k.substring(wilds[i].length());
			String ok=(String)keyMap.get(wk);
			if (ok==null || k.length()>ok.length())
			    keyMap.put(wk,k);
			continue;
		    }
		}
	    }
	    return keyMap.keys();
	}
	
	/* ------------------------------------------------------------ */
	public String toString()
	{
	    StringBuffer buf=new StringBuffer();
	    synchronized(buf)
	    {
		buf.append("{");
		boolean first=true;
		Enumeration e = this.keys();
		while(e.hasMoreElements())
		{
		    if (!first)
			buf.append(", ");
		    String k = (String)e.nextElement();
		    buf.append(k);
		    buf.append("=");
		    buf.append(get(k));
		    first=false;
		}
		buf.append("}");
	    }
	    return buf.toString();
	}
    }
};











