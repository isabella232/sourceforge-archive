// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Tools.Converter;

import com.mortbay.Util.Code;
import com.mortbay.Util.Test;
import com.mortbay.Tools.PropertyTree;
import java.io.FileInputStream;

public class ObjectConverterTest
{
    /* ------------------------------------------------------------ */
    public static class TestClass1
    {
        public boolean OK = false;
        public String toString(){
            return "OK:"+OK;
        }
    }
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
    }
    /* ------------------------------------------------------------ */
    public static class B 
    {
	public int array[] = 
	{
	    1, 2, 3, 4, 5, 6
	};
	public boolean falsE = false;
	public java.util.Date date = new java.util.Date(110001111);
	public boolean equals(Object other){
	    if (!(other instanceof B)) return false;
	    B o = (B)other;
	    boolean same = o.falsE == falsE;
	    if (array == null)
		same = same && o.array == null;
	    else {
		same = same && array.length == o.array.length;
		for (int i = 0; same && i < array.length; i++)
		    same = same && array[i] == o.array[i];
	    }
	    return same && date.equals(o.date);
	}
    }
    public static class A {
	public int six = 6;
	public String blah = "blah";
	private B b = new B();
	public B getB()
	{
	    return b;
	}
	public void setB(B b_)
	{
	    b = b_;
	}
	public boolean equals(Object other){
	    if (!(other instanceof A)) return false;
	    A o = (A)other;
	    boolean same = six == o.six;
	    if (blah == null)
		same = same && o.blah == null;
	    else
		same = same && blah.equals(o.blah);
	    if (b == null)
		same = same && o.b == null;
	    else
		same = same && b.equals(o.b);
	    return same;
	}
    }
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static void main(String argv[])
    {
        Test test = new Test("ObjectConverter:convert");
	PropertyTree testProps = null;
	ConverterSet cs = new ConverterSet();
	cs.registerPrimitiveConverters();
	cs.register(new ObjectConverter());
        try
        {
	    testProps = new PropertyTree();
            testProps.load(new FileInputStream("ObjectConverterTest.prp"));

	    PropertyTree props = testProps.getTree("conversions");
            Class testClass2 = (new TestClass2()).getClass();
            Object converted = cs.convert(props, testClass2, cs);
            test.check(converted instanceof TestClass2,
		       "Converted to TestClass2");
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
	test = new Test("ObjectConverter:unsafeConvert");
	try
        {
	    PropertyTree props = testProps.getTree("conversions");
            Class testClass2 = (new TestClass2()).getClass();
            Object converted = cs.unsafeConvert(props, testClass2, cs);
            test.check(converted instanceof TestClass2,
		       "Converted to TestClass2");
            TestClass2 inst = (TestClass2)converted;
            test.check(inst.intTest1 == 99, "intTest1 not changed");
            test.check(inst.intTest2 == 999, "intTest2");
            test.check(inst.floatTest == 9.99F, "floatTest");
            test.checkEquals(inst.stringTest, "This is a field", "stringTest");
            test.checkEquals(inst.propTest, "This is a property", "propTest");
            test.check(inst.complexTest != null, "complexTest set");
            test.check(inst.complexTest.OK, "complexTest.OK");

	    props = testProps.getTree("fails");
	    
	    try {
		converted = cs.unsafeConvert(props, testClass2, cs);
		test.check(false, "should have thrown Exception!");
	    } catch (Exception ex){
		test.check(ex instanceof ObjectConverter.ObjectConvertFail,
			   "Threw ObjectConverter.ObjectConvertFail");
		ObjectConverter.ObjectConvertFail errors =
		    (ObjectConverter.ObjectConvertFail)ex;
		test.checkEquals(errors.getMessage(),
				 "Error converting field(s)",
				 "Error Message");
		ConvertFail intFail =
		    (ConvertFail)errors.getErrors().get("intTest2");
		test.check(intFail instanceof ConvertFail,
			   "intTest2 error type");
		test.checkEquals(intFail.getMessage(),
			 "java.lang.NumberFormatException: notAChanceInt",
			 "intTest2 Msg");
		ConvertFail floatFail =
		    (ConvertFail)errors.getErrors().get("floatTest");
		test.check(floatFail instanceof ConvertFail,
			   "floatTest error type");
		test.checkEquals(floatFail.getMessage(),
			 "java.lang.NumberFormatException: notAChanceFloat",
			 "floatFail Msg");
		Code.debug(ex.toString());
	    }
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
	try {
	    PropertyTreeConverter cv = new PropertyTreeConverter();
	    A a = new A();
	    PropertyTree props =
		(PropertyTree)cv.convert(a, PropertyTree.class, null);
	    test = new
		Test("PropertyTreeConverter/ObjectConverter orthogonality");
	    cs.register(new ArrayConverter(","));
	    A a2 = (A)cs.convert(props, a.getClass(), null);
	    test.checkEquals(a, a2, "A == A->props->A");
        }
	catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
	
        test.report();
    }
    /* ------------------------------------------------------------ */
}
