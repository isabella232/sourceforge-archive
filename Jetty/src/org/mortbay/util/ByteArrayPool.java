// ===========================================================================
// Copyright (c) 2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.util;

/* ------------------------------------------------------------ */
/** Byte Array Pool
 * Simple pool for recycling byte arrays of a fixed size.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ByteArrayPool
{
    public static final int __POOL_SIZE=
        Integer.getInteger("org.mortbay.util.ByteArrayPool",20).intValue();
    
    private static byte[][] __pool = new byte[__POOL_SIZE][];
    private static int __in;
    private static int __out;
    private static int __size;
    private static int __lastSize=4096;

    /* ------------------------------------------------------------ */
    public static synchronized byte[] getByteArray()
    {
        if (__size>0)
        {
            byte[] b = __pool[__out++];
            if (__out>=__POOL_SIZE)
                __out=0;
            __size--;

            return b;           
        }
        
        return new byte[__lastSize];
    }
    /* ------------------------------------------------------------ */
    public static synchronized byte[] getByteArray(int size)
    {
        __lastSize=size;
        if (__size>0)
        {
            byte[] b = __pool[__out++];
            if (__out>=__POOL_SIZE)
                __out=0;
            __size--;

            if (b.length==size)
            {
                return b;
            }
           
        }
        
        return new byte[size];
    }

    /* ------------------------------------------------------------ */
    public static synchronized void returnByteArray(byte[] b)
    {
        if (b==null)
            return;
        
        if (__size>0 && b.length!=__pool[__out].length)
            return;
        
        if (__size<__POOL_SIZE)
        {
            __pool[__in++]=b;
            if (__in>=__POOL_SIZE)
                __in=0;
            __size++;
        }
    }
}
