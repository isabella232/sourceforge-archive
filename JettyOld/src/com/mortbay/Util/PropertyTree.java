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
 * the same thing (although most times this is unnecessary). This makes it
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
 * <p>
 * PropertyTree produces debug output about put if debug verbosity is
 * greater than 9. If it is greater than 19, gets are also in the debug.
 * <p>
 * To aid in constructing and saving Properties files,
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
    private String prefix=null;
    private PropertyTree parent=null;
    private boolean trim=true;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * Equivalent to PropertyTree(true);
     */
    public PropertyTree()
    {}
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param trimLoadValues If true, all values are trimmed during loads.
     */
    public PropertyTree(boolean trimLoadValues)
    {
	trim=trimLoadValues;
    }
    
    /* ------------------------------------------------------------ */
    /** Construct from Properties
     * @param properties 
     */
    public PropertyTree(Properties properties)
    {
	load(properties);
    }

    /* ------------------------------------------------------------ */
    private PropertyTree(PropertyTree parent,String node)
    {
	if (Code.verbose()) Code.debug("Subtree at ",node);
	this.prefix=node+".";
	
	Vector tokens=parent.getTokens(node);
	String[] prefixes=new String[tokens.size()];
	for (int i=0;i<tokens.size();i++)
	    prefixes[i]=((i>0)?prefixes[i-1]:"")+tokens.elementAt(i)+".";

	String[] wilds=new String[tokens.size()];
	for (int i=1;i<wilds.length;i++)
	    wilds[i]=prefixes[i-1]+"*";
	wilds[0]="*";

	if (Code.verbose(9))
	{
	    Code.debug("prefixes=",DataClass.toString(prefixes));
	    Code.debug("wilds=",DataClass.toString(wilds));
	}

	boolean wildPrefix = prefix.endsWith("*.");
	
	Hashtable keyMap = new Hashtable(parent.size()+3);
	Enumeration e = parent.keys();
	while (e.hasMoreElements())
	{
	    String k=(String)e.nextElement();
	    
	    if (k.startsWith(prefix) && !prefix.startsWith("*"))
	    {
		Object v=parent.get(k);
		
		String tk=k.substring(prefix.length());
		keyMap.put(tk,k);
		put(tk,v);
		if (Code.verbose(99)) Code.debug("map key ",tk,"-->",k);

		if (wildPrefix)
		{
		    String wk="*."+tk;
		    String ok=(String)keyMap.get(wk);
		    if (ok==null || k.length()>ok.length())
		    {
			keyMap.put(wk,k);
			put(wk,v);
			if (Code.verbose(99)) Code.debug("map new wild ",wk,"-->",k);
		    }
		}
		
		continue;
	    }

	    for (int i=wilds.length;i-->0;)
	    {
		if (k.startsWith(wilds[i]))
		{
		    String tk=k.substring(wilds[i].length());
		    String wk="*"+tk;
		    if (tk.length()>0)
			tk=tk.substring(1);
		    String ok=(String)keyMap.get(wk);
		    if (ok==null || k.length()>ok.length())
		    {
			Object v=parent.get(k);
			
			keyMap.put(wk,k);
			put(wk,v);
			if (Code.verbose(99)) Code.debug("map wild ",wk,"-->",k);

			if (tk.length()==0)
			    continue;
			
			ok=(String)keyMap.get(tk);
			if (ok==null || k.length()>ok.length())
			{
			    keyMap.put(tk,k);
			    put(tk,v);
			    if (Code.verbose(99))Code.debug("map exwild ",tk,"-->",k);
			}
		    }
		    continue;
		}
	    }
	}
	this.parent=parent;
    }
    

    /* ------------------------------------------------------------ */
    public void load(InputStream in)
	throws IOException
    {
	super.load(in);

	Enumeration e=keys();
	while (e.hasMoreElements())
	{
	    Object k=e.nextElement();
	    String v=(String)get(k);

	    put(k,trim?v.trim():v);    
	}
    }

    /* ------------------------------------------------------------ */
    public void load(Properties properties)
    {
	Enumeration e=properties.keys();
	while (e.hasMoreElements())
	{
	    Object k=e.nextElement();
	    String v=(String)properties.get(k);
	    put(k,trim?v.trim():v);    
	}
    }
    
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
	
	if (Code.verbose(19)) Code.debug("Get ",realKey,"(",key,")=",value);
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
	if (Code.verbose(9)) Code.debug("Put ",key,"=",value);
	String keyStr=key.toString();
	putTokenKey(keyStr,keyStr);
	Object v=null;
	if (parent!=null)
	    v=parent.put(parentKey((String)key),value);
	
	return super.put(key,value);
    }

    /* ------------------------------------------------------------ */
    public Object setProperty(String key,String value)
    {
	return (String)put(key,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Override Hashtable.remove() */
    public synchronized Object remove(Object key)
    {
	if (parent!=null)
	    parent.remove(parentKey((String)key));
	
	Object value=super.get(key);
	if (value!=null)
	{
	    if (Code.verbose(9)) Code.debug("Remove ",key);
	    putTokenKey(key.toString(),null);
	    return super.remove(key);
	}

	String realKey=getTokenKey(key.toString());
	if (realKey!=null)
	{
	    if (Code.verbose(9)) Code.debug("Remove ",realKey,"(",key,")");
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
    /** Return a sub tree of the PropertyTree.
     * Changes made in the sub tree are reflected in the original tree,
     * unless the sub tree is cloned.
     * @param name The name of the sub node
     * @return null if none.
     */
    public PropertyTree getTree(String key)
    {
	if (prefix!=null && prefix.endsWith("*.") && key.startsWith("*"))
	    return this;
	return new PropertyTree(this,key);
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
    /** Enumerate top level tree node names.
     * @return Enumeration of tree node names.
     */
    public Enumeration getNodes()
    {
	return getNodes("");
    }
    
    /* ------------------------------------------------------------ */
    /** Enumerate tree node names below given node.
     * @param key Key of the node.
     * @return Enumeration of tree node names.
     */
    public Enumeration getNodes(String key)
    {
	Vector tokens=getTokens(key);
	Node node=rootNode;
	int index=0;
	while(index<tokens.size())
	{
	    Node subNode = (Node)node.get(tokens.elementAt(index));
	    if (subNode==null)
		return null;
	    node=subNode;
	    index++;
	}
	return node.keys();
    }
    
    /* ------------------------------------------------------------ */
    public Vector getVector(String key, String separators)
    {
	String values=getProperty(key);
	if (values==null)
	    return null;

	Vector v=new Vector();
	StringTokenizer tok=new StringTokenizer(values,separators);
	while(tok.hasMoreTokens())
	    v.addElement(tok.nextToken());
	return v;
    }
    
    /* ------------------------------------------------------------ */
    public boolean getBoolean(String key)
    {
	String value=getProperty(key);
	if (value==null || value.length()==0)
	    return false;

	return "1tTyYoO".indexOf(value.charAt(0))>=0;
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
    private String parentKey(String key)
    {
	return StringUtil.replace(prefix+key,"*.*","*");    
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
};











