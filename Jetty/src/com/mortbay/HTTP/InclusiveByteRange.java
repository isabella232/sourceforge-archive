package com.mortbay.HTTP;

import com.mortbay.Util.Code;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

/* ------------------------------------------------------------ */
/** class for dealing with byte ranges
 * <PRE>
 * 
 *   parses the following types of byte ranges:
 * 
 *       bytes=100-499
 *       bytes=-300
 *       bytes=100-
 *       bytes=1-2,2-3,6-,-2
 *
 *   given an entity length, converts range to string
 * 
 *       bytes 100-499/500
 * 
 * </PRE>
 * 
 * @see RFC2616 3.12, 14.16, 14.35.1, 14.35.2
 * @version $version$
 * @author Helmut Hissen
 */


public class InclusiveByteRange {

    long first = 0;
    long last  = 0;    

    public InclusiveByteRange(long first, long last)
    {
        this.first = first;
        this.last = last;
    }
    
    public long getFirst()
    {
        return first;
    }

    public long getLast()
    {
        return last;
    }    

    public static List parseRangeHeaders(List reqRangeHeaders)
    {
        ListIterator rit = reqRangeHeaders.listIterator();
        List validRanges = new ArrayList();

        
        // walk through all Range headers
        while (rit.hasNext())
        {
            String header = (String) rit.next();
            StringTokenizer tok = new StringTokenizer(header,"=,",false);

            // read all byte ranges for this header 
            while (tok.hasMoreTokens())
            {
                try{
                    String t=tok.nextToken().trim();
                    
                    long first = 0;
                    long last  = -1;
                    int d=t.indexOf("-");
                    if (d<0)
                    {           
                        if ("bytes".equals(t))
                            continue;
                        Code.warning("Bad range format: "+t+" in "+reqRangeHeaders);
                        continue;
                    }
                    else if (d==0)
                    {
                        if (d+1<t.length())
                        last = Long.parseLong(t.substring(d+1).trim());
                    }
                    else if (d+1<t.length())
                    {
                        first = Long.parseLong(t.substring(0,d).trim());
                        last = Long.parseLong(t.substring(d+1).trim());
                    }
                    else
                        first = Long.parseLong(t.substring(0,d).trim());
                    
                    
                    if (first == -1 && last == -1)
                        continue;
                    
                    if (first != -1 && last != -1 && (first > last))
                        continue;
                    
                    validRanges.add(new InclusiveByteRange(first, last));
                }
                catch(Exception e)
                {
                    Code.ignore(e);
                }
            }
        }
        return validRanges;
    }

    /* ------------------------------------------------------------ */
    public long getSize(long size)
    {
        if (last<0)
            return size;
        if (last>=size)
            throw new IllegalArgumentException("invalid size "+size+" for "+this);
        return last-first+1;
    }


    /* ------------------------------------------------------------ */
    public String toHeaderRangeString(long size)
    {
        StringBuffer sb = new StringBuffer(40);
        sb.append("bytes ");
        sb.append(getFirst());
        sb.append('-');
        if (getLast()<0)
            sb.append(size-1);
        else
            sb.append(getLast());
        sb.append("/");
        sb.append(getSize(size));
        return sb.toString();
    }

    /* ------------------------------------------------------------ */
    public static String to416HeaderRangeString(long size)
    {
        StringBuffer sb = new StringBuffer(40);
        sb.append("bytes */");
        sb.append(size);
        return sb.toString();
    }


    /* ------------------------------------------------------------ */
    public String toString()
    {
        StringBuffer sb = new StringBuffer(60);
        sb.append(Long.toString(first));
        sb.append("-");
        sb.append(Long.toString(last));
        return sb.toString();
    }


    /* ------------------------------------------------------------ */
    public static void main(String [] args)
    {
        ArrayList al = new ArrayList(args.length);
        for (int i = 0; i < args.length; i++) {
             al.add(args[i]);
        }
        List parsed = parseRangeHeaders(al);
        System.out.println(parsed);
        ListIterator ali = parsed.listIterator();
        while (ali.hasNext()) {
            InclusiveByteRange ibr = (InclusiveByteRange) ali.next();
            System.out.println(ibr.toHeaderRangeString(1000));
        }
    }

}



