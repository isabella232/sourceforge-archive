// ===========================================================================
// Copyright (c) 2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.util;
import java.util.ArrayList;

/* ------------------------------------------------------------ */
/** Byte Array Pool
 * Simple pool for recycling byte arrays of a fixed size.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ByteArrayPool
{
    public static final int __POOL_SIZE=20;
    private static ArrayList _pool = new ArrayList(__POOL_SIZE);

    /* ------------------------------------------------------------ */
    public static synchronized byte[] getByteArray(int size)
    {
        while (_pool.size()>0)
        {
            byte[] b = (byte[])_pool.remove(_pool.size()-1);
            if (b.length!=size)
            {
                Code.warning("Wrong buffer size:"+b.length);
                continue;
            }
            return b;
        }
        
        return new byte[size];
    }

    /* ------------------------------------------------------------ */
    public static synchronized void returnByteArray(byte[] b)
    {
        if (b!=null && _pool.size()<__POOL_SIZE)
            _pool.add(b);
    }
}
