// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import com.mortbay.Base.Test;
import java.io.FileInputStream;
import java.util.*;

public class PropertyTreeTest
{
    /* ------------------------------------------------------------ */
    public static void main(String argv[])
	throws Exception
    {
	Test test = new Test("PropertyTree");
	PropertyTree props = new PropertyTree();
	props.load(new FileInputStream("PropertyTreeTest.prp"));
	Code.debug(props);
	
	test.checkEquals(props.get("a.f.g.c"), "4", "a.f.g.c");
	test.checkEquals(props.get("a.b.g.c"), "4", "a.b.g.c");
	test.checkEquals(props.get("a.b.c"), "2", "a.b.c");
	test.checkEquals(props.get("a"), "foo", "a");
	test.checkEquals(props.get("a.c"), null, "a.c");
	test.checkEquals(props.get("b.c"), null, "b.c");
	test.checkEquals(props.get("a.c.d"), "wow", "a.c.d");
	test.checkEquals(props.get("b.d"), "wow", "b.d");
	test.checkEquals(props.get("d"), null, "d");
	test.checkEquals(props.get("e.a.f.g.c"), "all", "e.a.f.g.c");
	test.checkEquals(props.get("e.a.b.g.c"), "all", "e.a.b.g.c");
	test.checkEquals(props.get("e.a.b.c"), "all", "e.a.b.c");
	test.checkEquals(props.get("e.a.c.d"), "except", "e.a.c.d");
	test.checkEquals(props.get("e.b.c"), "these", "e.b.c");
	test.checkEquals(props.get("e.b.d"), "except", "e.b.d");
	test.checkEquals(props.get("a.c.d.f.l"), null, "a.c.d.f.l");
	test.checkEquals(props.get("a.c.d.j.k.l"), "complex1", "a.c.d.j.k.l");
	test.checkEquals(props.get("a.c.d.f.k.l"), "complex2", "a.c.d.f.k.l");
	test.checkEquals(props.get("b.c.d.j.k.l"), "complex3", "b.c.d.j.k.l");
	test.checkEquals(props.get("a.c.d.j.k.l"), "complex1", "a.c.d.j.k.l");
	test.checkEquals(props.get("a.c.d.j.x.y.l"), "complex4", "a.c.d.j.x.y.l");
	test.checkEquals(props.get("a.c.d.j.x.y.z.l"), "complex5", "a.c.d.j.x.y.z.l");
	test.checkEquals(props.get("a.b.c.d"), "ambig1", "a.b.c.d");
	test.checkEquals(props.get("a.b.d.d"), "ambig1", "a.b.d.d");
	test.checkEquals(props.get("a.b.x.d.x.d"), "ambig3", "a.b.x.d.x.d");
	test.checkEquals(props.getProperty("a.b.d"), "wow", "getProperty(a.b.d)");
	test.checkEquals(props.getProperty("a.b.c"), "2", "getProperty(a.b.c)");
	test.checkEquals(props.getProperty("a.c", "def"), "def", "getProperty(a.c, def)");
	test.checkEquals(props.get("h.f.g"), "hfstar", "h.f.g");
	test.checkEquals(props.get("h.f.g.g"), "hfstar", "h.f.g.g");

	PropertyTree sub = props.getTree("a");
	test.checkEquals(sub.get("f.g.c"), "4", "[a.]f.g.c");
	test.checkEquals(sub.get("b.g.c"), "4", "[a.]b.g.c");
	test.checkEquals(sub.get("b.c"), "2", "[a.]b.c");
	test.checkEquals(sub.get("c"), null, "[a.]c");
	test.checkEquals(sub.get("c.d"), "wow", "[a.]c.d");
	test.checkEquals(sub.get("b.d"), "wow", "[a.]b.d");
	test.checkEquals(sub.get("d"), "wow", "[a.]d");
	test.checkEquals(sub.get("e.a.c.d"), "ambig2", "[a.]e.a.c.d");
	test.checkEquals(sub.get("c.d.j.k.l"), "complex1", "[a.]c.d.j.k.l");
	test.checkEquals(sub.get("b.c.d"), "ambig1", "[a.]b.c.d");
	test.checkEquals(sub.get("b.d.d"), "ambig1", "[a.]b.d.d");
	test.checkEquals(sub.get("b.x.d.x.d"), "ambig3", "[a.]b.x.d.x.d");
	test.checkEquals(sub.getProperty("b.d"), "wow", "getProperty([a.]b.d)");
	test.checkEquals(sub.getProperty("b.c"), "2", "getProperty([a.]b.c)");
	test.checkEquals(sub.getProperty("c", "def"), "def", "getProperty([a.]c, def)");
	props=new PropertyTree();
	String[] init =
	{
	    "*","*.b.c","a.*.c","a.b.*","*.b.*","a.*",
	    "*.b","*.B","a.b.c","a.*.b","a.*.B"
	};
	for (int i=0;i<init.length;i++)
	    props.put(init[i],new Integer(i));
	sub=props.getTree("a");
	test.checkEquals(sub.get("*.b"),new Integer(9),"subtree get");
	
	sub=sub.getTree("b");	
	test.checkEquals(sub.toString(),
			 "{c=8, *.b.c=1, *.B=10, *=3, *.b.*=4, *.c=2, *.b=9}",
			 "SubTree");
	Properties clone = (Properties)sub.clone();
	test.checkEquals(clone.toString(),
			 "{c=8, *.b.c=1, *.B=10, *=3, *.b.*=4, *.c=2, *.b=9}",
			 "SubTree");
	sub.put("C","C");
	test.checkContains(props.toString(),"a.b.C=C","Subtree changed");
	clone.put("C","X");
	test.checkContains(props.toString(),"a.b.C=C","clone changed");
	sub.put("*.B","B");
	test.checkContains(props.toString(),"a.b.*.B=B","Subtree changed");
	test.checkContains(props.toString(),"a.*.B=10","Subtree changed");

	Enumeration e=sub.elements();
	String v=sub.toString();
	while(e.hasMoreElements())
	{
	    String ev="="+e.nextElement();
	    test.checkContains(v,ev,"Elements");
	    v=v.substring(0,v.indexOf(ev))+v.substring(v.indexOf(ev)+ev.length());
	}
	
	Vector nodes;
	nodes=enum2vector(props.getNodes(""));
	test.checkEquals(nodes.size(),2,"Get root node");
	test.check(nodes.contains("a"),"Get root node");
	test.check(nodes.contains("*"),"Get root node");
	
	nodes=enum2vector(props.getNodes("a"));
	test.checkEquals(nodes.size(),2,"Get a node");
	test.check(nodes.contains("b"),"Get a node");
	test.check(nodes.contains("*"),"Get a node");
	
	nodes=enum2vector(props.getNodes("*"));
	test.checkEquals(nodes.size(),2,"Get wild node");
	test.check(nodes.contains("b"),"Get wild node");
	test.check(nodes.contains("B"),"Get wild node");
	
	nodes=enum2vector(props.getNodes("a.*"));
	test.checkEquals(nodes.size(),3,"Get node");
	test.check(nodes.contains("b"),"Get node");
	test.check(nodes.contains("B"),"Get node");
	test.check(nodes.contains("c"),"Get node");
	
	test.report();
    }
    /* ------------------------------------------------------------ */
    static Vector enum2vector(Enumeration e)
    {
	Vector v = new Vector();
	while (e.hasMoreElements())
	    v.addElement(e.nextElement());
	return v;
    }
    
};

