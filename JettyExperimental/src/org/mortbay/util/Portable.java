
package org.mortbay.util;

import java.io.UnsupportedEncodingException;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class Portable
{
	public static void arraycopy(byte[] src, int srcOffset, byte[] dst, int dstOffset, int length)
	{
		System.arraycopy(src,srcOffset,dst,dstOffset,length);
	}
	
	public static void throwNotSupported()
	{
		throw new RuntimeException("Not Supported");
	}

	public static void throwIllegalArgument(String msg)
	{
		throw new IllegalArgumentException(msg);
	}
	
	public static void throwIllegalState(String msg)
	{
		throw new IllegalStateException(msg);
	}
	
	public static void throwRuntime(String msg)
	{
		throw new RuntimeException(msg);
	}
	
	public static byte[] getBytes(String s)
	{
		try
        {
            return s.getBytes("ISO8859_1");
        }
        catch (UnsupportedEncodingException e)
        {
            return s.getBytes();
        }
	}
}
