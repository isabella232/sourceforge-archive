// ===========================================================================
// Copyright (c) 2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.util;

/* ------------------------------------------------------------ */
/** TYPE Utilities
 * Provides a cache for basic Types.  The cache size can be controlled with
 * the "org.mortbay.util.TypeUtil.IntegerCacheSize" property.
 * @since Jetty 4.1
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class TypeUtil
{
    private static int intCacheSize=
        Integer.getInteger("org.mortbay.util.TypeUtil.IntegerCacheSize",600).intValue();
    private static Integer[] integerCache = new Integer[intCacheSize];
    private static String[] integerStrCache = new String[intCacheSize];

    /* ------------------------------------------------------------ */
    /** Convert int to Integer using cache. 
     */
    public static Integer newInteger(int i)
    {
        if (i<intCacheSize)
        {
            if (integerCache[i]==null)
                integerCache[i]=new Integer(i);
            return integerCache[i];
        }
        return new Integer(i);
    }

    
    /* ------------------------------------------------------------ */
    /** Convert int to String using cache. 
     */
    public static String toString(int i)
    {
        if (i<intCacheSize)
        {
            if (integerStrCache[i]==null)
                integerStrCache[i]=Integer.toString(i);
            return integerStrCache[i];
        }
        return Integer.toString(i);
    }

}
