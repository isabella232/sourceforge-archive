// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import com.mortbay.Base.Test;
import java.io.FileInputStream;

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
	System.out.println(props);
	
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
	test.checkEquals(props.getProperty("a.b.c"), "2", "getProperty(a.b.c)");
	test.checkEquals(props.getProperty("a.c", "def"), "def", "getProperty(a.c, def)");
	test.checkEquals(props.get("h.f.g"), "hfstar", "h.f.g");
	test.checkEquals(props.get("h.f.g.g"), "hfstar", "h.f.g.g");
	test.report();
    }
    /* ------------------------------------------------------------ */
};
