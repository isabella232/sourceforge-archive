/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */
 
package org.mortbay.util.io;

import org.mortbay.util.Portable;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class BufferUtil
{
	static final byte SPACE= 0x20;
	static final byte MINUS= '-';
	
	public static int toInt(Buffer buffer)
	{
		int val=0;
		boolean started=false;
		boolean minus=false;
		for (int i=buffer.offset();i<buffer.limit();i++)
		{
			byte b=buffer.peek(i);
			if (b < SPACE)
			{
				if(started)
					break;
			}
			else if (b>='0' && b<='9')
			{
				val=val*10+(b-'0');
				started=true;
			}
			else if (b==MINUS && !started)
			{
				minus=true;
				started=true;
			}
			else
				break;
		}
		if (started)
			return minus?(-val):val;
		Portable.throwIllegalArgument("bad number");
		return -1;
	}

}
