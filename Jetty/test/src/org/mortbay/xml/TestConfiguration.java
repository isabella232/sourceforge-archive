// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.xml;
import java.net.URL;
import java.util.HashMap;

/* ------------------------------------------------------------ */
/** Test XmlConfiguration. 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class TestConfiguration extends HashMap
{
    public static int VALUE=77;

    public TestConfiguration nested;
    public Object testObject;
    public int testInt;
    public URL url;
    public static boolean called=false;
    public Object[] oa;
    public int[] ia;
    private int test;
    public int testField1;
    public int testField2;
    
    public void setTest(Object value)
    {
        testObject=value;
    }
    
    public void setTest(int value)
    {
        testInt=value;
    }

    public void call()
    {
        put("Called","Yes");
    }
    
    public TestConfiguration call(Boolean b)
    {
        nested=new TestConfiguration();
        nested.put("Arg",b);
        return nested;
    }
    
    public void call(URL u,boolean b)
    {
        put("URL",b?"1":"0");
        url=u;
    }

    public String getString()
    {
        return "String";
    }

    public static void callStatic()
    {
        called=true;
    }
    
    public void call(Object[] oa)
    {
        this.oa=oa;
    }
    
    public void call(int[] ia)
    {
        this.ia=ia;
    }
}






