// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.XML;
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
    public TestConfiguration nested;
    public Object testObject;
    public int testInt;
    public URL url;
    
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
}






