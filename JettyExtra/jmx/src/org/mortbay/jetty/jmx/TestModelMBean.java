// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.jmx;

import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;

import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.mortbay.util.Code;


/* ------------------------------------------------------------ */
/** Test ModelMBeanImpl
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class TestModelMBean extends ModelMBeanImpl
{    
    /* ------------------------------------------------------------ */
    private Float www=null;
    private String xxx="init";
    private int yyy=42;
    private boolean zzz=false;

    /* ------------------------------------------------------------ */
    public TestModelMBean()
        throws MBeanException
    {
        defineAttribute(new ModelMBeanAttributeInfo("www",
                                                    "java.lang.Float",
                                                    "Description",
                                                    true,
                                                    true,
                                                    false));
        defineAttribute("xxx");
        defineAttribute("yyy");
        defineAttribute("zzz");
        

        defineOperation("call",MBeanOperationInfo.ACTION);
        defineOperation("call",
                        new String[]{"int","java.lang.String"},
                        MBeanOperationInfo.ACTION);

        try
        {
            defineOperation(new ModelMBeanOperationInfo
                ("Blah Blah Blah",TestModelMBean.class.getMethod
                 ("call",new Class[]{java.net.URL.class})));
        }
        catch(Exception e)
        {
            throw new MBeanException(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    public Float getWww()
    {
        System.err.println("getWww");
        return www;
    }

    /* ------------------------------------------------------------ */
    public void setWww(Float f)
    {
        System.err.println("setWww");
        www=f;
    }

    /* ------------------------------------------------------------ */
    public String getXxx()
    {
        System.err.println("getXxx");
        return xxx;
    }

    /* ------------------------------------------------------------ */
    public void setXxx(String a)
    {
        System.err.println("setXxx");
        xxx=a;
    }

    /* ------------------------------------------------------------ */
    public int getYyy()
    {
        System.err.println("getYyy");
        return yyy;
    }

    /* ------------------------------------------------------------ */
    public void setYyy(int a)
    {
        System.err.println("setYyy");
        yyy=a;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isZzz()
    {
        System.err.println("isZzz");
        return zzz;
    }

    /* ------------------------------------------------------------ */
    public void setZzz(boolean z)
    {
        System.err.println("setZzz");
        zzz=z;
    }

    /* ------------------------------------------------------------ */
    public void call()
    {
        System.err.println("call");
        xxx="Call";
    }
    
    /* ------------------------------------------------------------ */
    public String call(int i, String s)
    {
        System.err.println("call");
        xxx="Call("+i+","+s+")";
        return xxx;
    }
    
    /* ------------------------------------------------------------ */
    public void call(java.net.URL u)
    {
        System.err.println("call");
        xxx=u.toString();
    }

    
}
