/*
 * Created on 20-Mar-2003
 *
 */
package org.mortbay.util;

/**
 * @author gregw
 *
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
}
