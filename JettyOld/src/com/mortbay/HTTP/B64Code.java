// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Util.*;
import java.net.*;


// =======================================================================
public class B64Code
{
    // ------------------------------------------------------------------
    static final char[] b2c=
    {
	'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P',
	'Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f',
	'g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v',
	'w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/'
    };

    static final char pad = '=';

    // ------------------------------------------------------------------
    static byte[] c2b = null;

    // ------------------------------------------------------------------
    static public String decode(String s)
    {
	if (c2b==null)
	{
	    c2b = new byte[256];
	    
	    for (byte b=0;b<64;b++)
	       c2b[(byte)b2c[b]]=b;
	}

	byte[] nibble = new byte[4];
	char[] decode = new char[s.length()];
	int d=0;
	int n=0;
	byte b;
	for (int i=0;i<s.length();i++)
	{
	    char c = s.charAt(i);
	    nibble[n] = c2b[(int)c];
	    
	    if (c==pad)
	       break;
	    
	    switch(n)
	    {
	      case 0:
		n++;
		break;
		
	      case 1:
		b=(byte)(nibble[0]*4 + nibble[1]/16);
		decode[d++]=(char)b;
		n++;
		break;
		
	      case 2:
		b=(byte)((nibble[1]&0xf)*16 + nibble[2]/4);
		decode[d++]=(char)b;
		n++;
		break;
		
	      default:
		b=(byte)((nibble[2]&0x3)*64 + nibble[3]);
		decode[d++]=(char)b;
		n=0;
		break;
	    }
	}

	String decoded = new String(decode,0,d);
	return decoded;
    }
}
