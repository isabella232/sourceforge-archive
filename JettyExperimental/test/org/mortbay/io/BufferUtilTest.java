/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.io;

import junit.framework.TestCase;

/* ------------------------------------------------------------------------------- */
/**
 * 
 */
public class BufferUtilTest extends TestCase
{

    /**
     * Constructor for BufferUtilTest.
     * @param arg0
     */
    public BufferUtilTest(String arg0)
    {
        super(arg0);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(BufferUtilTest.class);
    }

    public void testToInt()
        throws Exception
    {
        Buffer buf[] = 
        {
            new ByteArrayBuffer("0"),
            new ByteArrayBuffer(" 42 "),
            new ByteArrayBuffer("   43abc"),
            new ByteArrayBuffer("-44"),
            new ByteArrayBuffer(" - 45;"),
            new ByteArrayBuffer("-2147483648"),
            new ByteArrayBuffer("2147483647"),
        };
        
        int val[] =
        {
            0,42,43,-44,-45,-2147483648,2147483647
        };
        
        for (int i=0;i<buf.length;i++)
            assertEquals("t"+i, val[i], BufferUtil.toInt(buf[i]));
    }

    public void testPutInt()
        throws Exception
    {
        int val[] =
        {
            0,42,43,-44,-45,-2147483648,2147483647
        };
        
        String str[] =
        {
            "0","42","43","-44","-45","-2147483648","2147483647"
        };
        
        Buffer buffer = new ByteArrayBuffer(12);

        for (int i=0;i<val.length;i++)
        {
            buffer.clear();
            BufferUtil.putInt(buffer,val[i]);
            assertEquals("t"+i,str[i],buffer.toString());
        }       
    }
}
