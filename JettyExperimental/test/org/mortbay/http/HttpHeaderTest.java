/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import java.util.Enumeration;

import junit.framework.TestCase;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;

/* ------------------------------------------------------------------------------- */
/**
 * 
 */
public class HttpHeaderTest extends TestCase
{

    /**
     * Constructor for HttpHeaderTest.
     * @param arg0
     */
    public HttpHeaderTest(String arg0)
    {
        super(arg0);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HttpHeaderTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testPut()
        throws Exception
    {
        HttpHeader header = new HttpHeader();
        
        header.put("name0", "value0");
        header.put("name1", "value1");
        
        assertEquals("value0",header.get("name0"));
        assertEquals("value1",header.get("name1"));
        assertEquals(null,header.get("name2"));
        
        int matches=0;
        Enumeration e = header.getFieldNames();
        while (e.hasMoreElements())
        {
            Object o=e.nextElement();
            if ("name0".equals(o))
                matches++;
            if ("name1".equals(o))
                matches++;
        }
        assertEquals(2, matches);
        
        matches=0;
        e = header.getValues("name0");
        assertEquals(true, e.hasMoreElements());
        assertEquals(e.nextElement(), "value0");
        assertEquals(false, e.hasMoreElements());
    }
    
    public void testCachedPut()
        throws Exception
    {
        HttpHeader header = new HttpHeader();
        
        header.put("Content-Type", "gzip");
        header.put("Connection", "keep-alive");
        
        assertTrue(HttpHeaderValues.GZIP==header.get(HttpHeaders.CONTENT_TYPE));
        assertTrue(HttpHeaderValues.KEEP_ALIVE==header.get(HttpHeaders.CONNECTION));

        int matches=0;
        Enumeration e = header.getFieldNames();
        while (e.hasMoreElements())
        {
            Object o=e.nextElement();
            if (o==HttpHeaders.CONTENT_TYPE)
                matches++;
            if (o==HttpHeaders.CONNECTION)
                matches++;
        }
        assertEquals(2, matches);
        
        
    }
    
    public void testRePut()
        throws Exception
    {
        HttpHeader header = new HttpHeader();
        
        header.put("name0", "value0");
        header.put("name1", "xxxxxx");
        header.put("name2", "value2");

        assertEquals("value0",header.get("name0"));
        assertEquals("xxxxxx",header.get("name1"));
        assertEquals("value2",header.get("name2"));
        
        header.put("name1", "value1");
        
        assertEquals("value0",header.get("name0"));
        assertEquals("value1",header.get("name1"));
        assertEquals("value2",header.get("name2"));
        assertEquals(null,header.get("name3"));
        
        int matches=0;
        Enumeration e = header.getFieldNames();
        while (e.hasMoreElements())
        {
            Object o=e.nextElement();
            if ("name0".equals(o))
                matches++;
            if ("name1".equals(o))
                matches++;
            if ("name2".equals(o))
                matches++;
        }
        assertEquals(3, matches);
        
        matches=0;
        e = header.getValues("name1");
        assertEquals(true, e.hasMoreElements());
        assertEquals(e.nextElement(), "value1");
        assertEquals(false, e.hasMoreElements());
    }
    
    public void testRemovePut()
        throws Exception
    {
        HttpHeader header = new HttpHeader();
        
        header.put("name0", "value0");
        header.put("name1", "value1");
        header.put("name2", "value2");

        assertEquals("value0",header.get("name0"));
        assertEquals("value1",header.get("name1"));
        assertEquals("value2",header.get("name2"));
        
        header.remove("name1");
        
        assertEquals("value0",header.get("name0"));
        assertEquals(null,header.get("name1"));
        assertEquals("value2",header.get("name2"));
        assertEquals(null,header.get("name3"));
        
        int matches=0;
        Enumeration e = header.getFieldNames();
        while (e.hasMoreElements())
        {
            Object o=e.nextElement();
            if ("name0".equals(o))
                matches++;
            if ("name1".equals(o))
                matches++;
            if ("name2".equals(o))
                matches++;
        }
        assertEquals(2, matches);
        
        matches=0;
        e = header.getValues("name1");
        assertEquals(false, e.hasMoreElements());
    }

    
    public void testAdd()
        throws Exception
    {
        HttpHeader header = new HttpHeader();
        
        header.add("name0", "value0");
        header.add("name1", "valueA");
        header.add("name2", "value2");

        assertEquals("value0",header.get("name0"));
        assertEquals("valueA",header.get("name1"));
        assertEquals("value2",header.get("name2"));
        
        header.add("name1", "valueB");
        
        assertEquals("value0",header.get("name0"));
        assertEquals("valueA",header.get("name1"));
        assertEquals("value2",header.get("name2"));
        assertEquals(null,header.get("name3"));
        
        int matches=0;
        Enumeration e = header.getFieldNames();
        while (e.hasMoreElements())
        {
            Object o=e.nextElement();
            if ("name0".equals(o))
                matches++;
            if ("name1".equals(o))
                matches++;
            if ("name2".equals(o))
                matches++;
        }
        assertEquals(3, matches);
        
        matches=0;
        e = header.getValues("name1");
        assertEquals(true, e.hasMoreElements());
        assertEquals(e.nextElement(), "valueA");
        assertEquals(true, e.hasMoreElements());
        assertEquals(e.nextElement(), "valueB");
        assertEquals(false, e.hasMoreElements());
        
        header.setStatus(200);
        System.out.println(header);
    }
    
    public void testReuse()
        throws Exception
    {
        HttpHeader header = new HttpHeader();
        Buffer n1=new ByteArrayBuffer("name1");
        Buffer va=new ByteArrayBuffer("value1");
        Buffer vb=new ByteArrayBuffer(10);
        vb.put((byte)'v');
        vb.put((byte)'a');
        vb.put((byte)'l');
        vb.put((byte)'u');
        vb.put((byte)'e');
        vb.put((byte)'1');
        
        header.put("name0", "value0");
        header.put(n1,va);
        header.put("name2", "value2");
        
        assertEquals("value0",header.get("name0"));
        assertEquals("value1",header.get("name1"));
        assertEquals("value2",header.get("name2"));
        assertEquals(null,header.get("name3"));
        
        header.remove(n1);
        assertEquals(null,header.get("name1"));
        header.put(n1,vb);
        assertEquals("value1",header.get("name1"));
        assertTrue(va.toString()==header.get("name1"));
        
        int matches=0;
        Enumeration e = header.getFieldNames();
        while (e.hasMoreElements())
        {
            Object o=e.nextElement();
            if ("name0".equals(o))
                matches++;
            if ("name1".equals(o))
                matches++;
            if ("name2".equals(o))
                matches++;
        }
        assertEquals(3, matches);
        
        matches=0;
        e = header.getValues("name1");
        assertEquals(true, e.hasMoreElements());
        assertEquals(e.nextElement(), "value1");
        assertEquals(false, e.hasMoreElements());
    }
    
    public void testDateFields()
        throws Exception
    {
        HttpHeader header = new HttpHeader();
        
        header.put("D1", "Fri, 31 Dec 1999 23:59:59 GMT");
        header.put("D2", "Friday, 31-Dec-99 23:59:59 GMT");
        header.put("D3", "Fri Dec 31 23:59:59 1999");
        header.put("D4", "Mon Jan 1 2000 00:00:01");
        header.put("D5", "Tue Feb 29 2000 12:00:00");

        long d1 = header.getDateField("D1");
        long d2 = header.getDateField("D2");
        long d3 = header.getDateField("D3");
        long d4 = header.getDateField("D4");
        long d5 = header.getDateField("D5");
        assertTrue(d1>0);
        assertTrue(d2>0);
        assertEquals(d1,d2);
        assertEquals(d2,d3);
        assertEquals(d3+2000,d4);
        assertEquals(951825600000L,d5);
        
        header.putDateField("D2",d1);
        assertEquals("Fri, 31 Dec 1999 23:59:59 GMT",header.get("D2"));
    }
    
    public void testIntFields()
        throws Exception
    {
        HttpHeader header = new HttpHeader();
        
        header.put("I1", "42");
        header.put("I2", " 43 99");
        header.put("I3", "-44;");
        header.put("I4", " - 45abc");
        header.put("N1", " - ");
        header.put("N2", "xx");
        
        int i1=header.getIntField("I1");
        int i2=header.getIntField("I2");
        int i3=header.getIntField("I3");
        int i4=header.getIntField("I4");
        
        try{
            header.getIntField("N1");
            assertTrue(false);
        }
        catch(NumberFormatException e)
        {
            assertTrue(true);
        }
        
        try{
            header.getIntField("N2");
            assertTrue(false);
        }
        catch(NumberFormatException e)
        {
            assertTrue(true);
        }
        
        assertEquals(42,i1);
        assertEquals(43,i2);
        assertEquals(-44,i3);
        assertEquals(-45,i4);
        
        header.putIntField("I5", 46);
        header.putIntField("I6",-47);
        assertEquals("46",header.get("I5"));
        assertEquals("-47",header.get("I6"));
       
    }
    
    public void testPutBuffer()
        throws Exception
    {
        HttpHeader header = new HttpHeader();
        header.put("name0", "value0");
        String s;
        
        header.setStatus(411);
        header.setVersion(HttpVersions.HTTP_1_1_ORDINAL);
        Buffer buffer = new ByteArrayBuffer(1024);
        header.put(buffer);
        s=buffer.toString();
        assertEquals("HTTP/1.1 411 Length Required\r\nname0: value0\r\n\r\n",s);

        buffer.clear();
        header.setStatus(0);
        header.setVersion(HttpVersions.HTTP_1_0_ORDINAL);
        header.setMethod(HttpMethods.GET_ORDINAL); 
        header.setURI("/foo");
        header.put(buffer);
        s=buffer.toString();
        assertEquals("GET /foo HTTP/1.0\r\nname0: value0\r\n\r\n",s);
          
    }
}
