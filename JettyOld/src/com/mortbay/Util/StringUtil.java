// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;




// ====================================================================
public class StringUtil
{
    private static char[] lowercases = {
          '\000','\001','\002','\003','\004','\005','\006','\007',
          '\010','\011','\012','\013','\014','\015','\016','\017',
          '\020','\021','\022','\023','\024','\025','\026','\027',
          '\030','\031','\032','\033','\034','\035','\036','\037',
          '\040','\041','\042','\043','\044','\045','\046','\047',
          '\050','\051','\052','\053','\054','\055','\056','\057',
          '\060','\061','\062','\063','\064','\065','\066','\067',
          '\070','\071','\072','\073','\074','\075','\076','\077',
          '\100','\141','\142','\143','\144','\145','\146','\147',
          '\150','\151','\152','\153','\154','\155','\156','\157',
          '\160','\161','\162','\163','\164','\165','\166','\167',
          '\170','\171','\172','\133','\134','\135','\136','\137',
          '\140','\141','\142','\143','\144','\145','\146','\147',
          '\150','\151','\152','\153','\154','\155','\156','\157',
          '\160','\161','\162','\163','\164','\165','\166','\167',
          '\170','\171','\172','\173','\174','\175','\176','\177' };

    /**
     * fast lower case conversion. Only works on ascii (not unicode)
     * @author Jesper J�rgensen, Caput
     * @param s the string to convert
     * @return a lower case version of s
     */
    public static String asciiToLowerCase(String s)
    {
        char[] c = s.toCharArray();
        for(int i=c.length;i-->0;)
        {
            if(c[i]<=127)
                c[i] = lowercases[c[i]];
        }
        return(new String(c));
    }
    
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
	int c=0;
	int i=s.indexOf(sub,c);
	if (i == -1)
	    return s;
    
	StringBuffer buf = new StringBuffer(s.length()+with.length());

	do
	{
	    buf.append(s.substring(c,i));
	    buf.append(with);
	    c=i+sub.length();
	} while ((i=s.indexOf(sub,c))!=-1);
    
	if (c<s.length())
	    buf.append(s.substring(c,s.length()));
    
	return buf.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Test main
     * @param args args
     */
    public static void main(String[] args)
    {
        StringBuffer b = new StringBuffer(256);
        for(int i=32;i<127;i++)
        {
            b.append((char) i);
        }
        System.out.println(b.toString());
        System.out.println("->");
        System.out.println(asciiToLowerCase(b.toString()));
    }
}
