// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import com.mortbay.Base.Test;
import java.io.FileInputStream;

public class DictionaryConverterTest
{
    /* ------------------------------------------------------------ */
    public static class TestClass1
    {
	public boolean OK = false;
	public String toString(){
	    return "OK:"+OK;
	}
    };
    public static class TestClass2
    {
	public int intTest1 = 99;
	public int intTest2 = 0;
	public float floatTest = 0;
	public String stringTest = null;
	String propTest = null;
	public void setPropTest(String val){
	    propTest = val;
	}
	TestClass1 complexTest = null;
	public void setComplexTest(TestClass1 val){
	    complexTest = val;
	}
	public String toString(){
	    return "i1:"+intTest1+";i2:"+intTest2+";f:"+floatTest+
		";s:"+stringTest+";p:"+propTest+";c:"+complexTest;
	}
    };
    /* ------------------------------------------------------------ */
    public static void main(String argv[])
    {
    	Test test = new Test("DictionaryConverter");
	try
	{
	    PropertyTree props = new PropertyTree();
	    props.load(new FileInputStream("DictionaryConverterTest.prp"));
	    ConverterSet cs = new ConverterSet();
	    cs.registerPrimitiveConverters();
	    cs.register(new DictionaryConverter());
	    Class testClass2 = (new TestClass2()).getClass();
	    Object converted = cs.convert(props, testClass2, cs);
	    test.check(converted instanceof TestClass2, "Converted to TestClass2");
	    TestClass2 inst = (TestClass2)converted;
	    test.check(inst.intTest1 == 99, "intTest1 not changed");
	    test.check(inst.intTest2 == 999, "intTest2");
	    test.check(inst.floatTest == 9.99F, "floatTest");
	    test.checkEquals(inst.stringTest, "This is a field", "stringTest");
	    test.checkEquals(inst.propTest, "This is a property", "propTest");
	    test.check(inst.complexTest != null, "complexTest set");
	    test.check(inst.complexTest.OK, "complexTest.OK");
	}
	catch(Exception e)
	{
	    Code.warning(e);
	    test.check(false,e.toString());
	}
	
	test.report();
    }
    /* ------------------------------------------------------------ */
};
