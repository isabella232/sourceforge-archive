// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;




// ====================================================================
public class StringUtil
{
    /**
     * returns the next index of a character from the chars string
     */
    public static int indexFrom(String s,String chars)
    {
	for (int i=0;i<s.length();i++)
	   if (chars.indexOf(s.charAt(i))>=0)
	      return i;
	return -1;
    }


    /**
     * replace substrings within string.
     */
    public static String replace(String s, String sub, String with)
    {
	StringBuffer buf = new StringBuffer(s.length()*2);

	int c=0;
	int i=0;
	while ((i=s.indexOf(sub,c))!=-1)
	{
	    buf.append(s.substring(c,i));
	    buf.append(with);
	    c=i+sub.length();
	}
	if (c<s.length())
	   buf.append(s.substring(c,s.length()));
	
	return buf.toString();
    }
}
