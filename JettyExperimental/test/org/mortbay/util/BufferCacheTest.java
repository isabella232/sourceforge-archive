/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 28-Apr-2003
 * $Id$
 * ============================================== */
 
package org.mortbay.util;

import junit.framework.TestCase;

import org.mortbay.util.io.Buffer;
import org.mortbay.util.io.BufferCache;
import org.mortbay.util.io.ByteArrayBuffer;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class BufferCacheTest extends TestCase
{ 
	final static String [] S ={"S0","S1","S2","S3"};
	
	BufferCache cache;
	
	public BufferCacheTest(String arg0)
	{
		super(arg0);
	}

	public static void main(String[] args)
	{
		junit.textui.TestRunner.run(BufferCacheTest.class);
	}

	/**
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception
	{
		super.setUp();
		cache=new BufferCache();
		cache.add(S[1],1);
		cache.add(S[2],2);
		cache.add(S[3],3);
	}

	/**
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception
	{
		super.tearDown();
	}

	public void testLookupIndex()
	{
		for (int i=0;i<S.length;i++)
		{
			String s = "S0S1S2S3";
			ByteArrayBuffer buf=new ByteArrayBuffer(s.getBytes(),i*2,2);
			int index=cache.lookupIndex(buf);
			
			if (i>0)
				assertEquals(i,index);
			else
				assertEquals(-1,index);
		}
	}
	

	public void testLookupBuffer()
	{
		for (int i=0;i<S.length;i++)
		{
			String s = "S0S1S2S3";
			ByteArrayBuffer buf=new ByteArrayBuffer(s.getBytes(),i*2,2);
			Buffer b=cache.lookupBuffer(buf);
			
			if (i>0)
				assertEquals(i,b.peek(1)-'0');
			else
				assertEquals(null,b);
		}
	}
	

	public void testNormalizeBuffer()
	{
		for (int i=0;i<S.length;i++)
		{
			String s = "S0S1S2S3";
			ByteArrayBuffer buf=new ByteArrayBuffer(s.getBytes(),i*2,2);
			Buffer b=cache.normalizeBuffer(buf);
			
			assertEquals(S[i],b.toString());
			if (i>0)
				assertTrue(S[i]==b.toString());
			else
				assertTrue(S[i]!=b.toString());
		}
	}
	

	public void testToString()
	{
		for (int i=0;i<S.length;i++)
		{
			String s = "S0S1S2S3";
			ByteArrayBuffer buf=new ByteArrayBuffer(s.getBytes(),i*2,2);
			String b=cache.toString(buf);
			
			assertEquals(S[i],b);
			if (i>0)
				assertTrue(S[i]==b);
			else
				assertTrue(S[i]!=b);
		}
	}
	
}
