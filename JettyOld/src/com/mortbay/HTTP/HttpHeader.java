// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Base.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import java.text.*;


// ====================================================================
public class HttpHeader
{
    public final static String ContentType = "Content-Type";
    public final static String TransferEncoding="Transfer-Encoding";
    public final static String Chunked = "chunked";
    public final static String ContentLength = "Content-Length";
    public final static String WwwFormUrlEncode = "application/x-www-form-urlencoded";
    public final static String WwwAuthenticate = "WWW-Authenticate";
    public final static String Authorization = "Authorization";
    public final static String Host = "Host";
    public final static String Date = "Date";
    public final static String Cookie = "Cookie";
    public final static String SetCookie = "Set-Cookie";
    public final static String Connection = "Connection";
    public final static String Close = "close";
    public final static String Referer="Referer";
    public final static String Expires="Expires";
    public final static String UserAgent="User-Agent";
    public final static String IfModifiedSince="If-Modified-Since";
    public final static String IfUnmodifiedSince="If-Unmodified-Since";
    public final static String LastModified="Last-Modified";
    public final static String[] SingleValued=
    {
        "age", "authorization", "content-length", "content-location", "content-md5",
        "content-range", "content-type", "date", "etag", "expires", "from", "host",
        "if-modified-since", "if-range", "if-unmodified-since", "last-modified",
        "location", "max-forwards", "proxy-authorization", "range", "referer",
        "retry-after", "server", "user-agent"
    };
    public final static Hashtable __singleValuedMap=new Hashtable(37);
    static
    {
        for (int i=0;i<SingleValued.length;i++)
            __singleValuedMap.put(SingleValued[i],SingleValued[i]);
    }

    public final static String CRLF = "\015\012";
    public final static byte[] __CRLF = {(byte)'\015',(byte)'\012'};
    public final static byte[] __COLON = {(byte)':',(byte)' '};
    public final static String COLON = ": ";

    public final static String HTTP_1_0 ="HTTP/1.0"   ;
    public final static String HTTP_1_1 ="HTTP/1.1"   ;

    /* -------------------------------------------------------------- */
    public final static DateCache __dateSend =
        new DateCache("EEE, dd MMM yyyy HH:mm:ss 'GMT'",
                      Locale.US);
    private final static String __dateReceiveFmt[] =
    {
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, dd MMM yyyy HH:mm:ss",
        "EEE dd MMM yyyy HH:mm:ss zzz",
        "EEE dd MMM yyyy HH:mm:ss",
        "EEE MMM dd yyyy HH:mm:ss zzz",
        "EEE MMM dd yyyy HH:mm:ss",
        "EEE MMM-dd-yyyy HH:mm:ss zzz",
        "EEE MMM-dd-yyyy HH:mm:ss",
        "dd MMM yyyy HH:mm:ss zzz",
        "dd MMM yyyy HH:mm:ss",
        "dd-MMM-yy HH:mm:ss zzz",
        "dd-MMM-yy HH:mm:ss",
        "MMM dd HH:mm:ss yyyy zzz",
        "MMM dd HH:mm:ss yyyy",
        "EEE MMM dd HH:mm:ss yyyy zzz",
        "EEE MMM dd HH:mm:ss yyyy",
        "EEE, MMM dd HH:mm:ss yyyy zzz",
        "EEE, MMM dd HH:mm:ss yyyy",
        "EEE, dd-MMM-yy HH:mm:ss zzz",
        "EEE, dd-MMM-yy HH:mm:ss",
        "EEE dd-MMM-yy HH:mm:ss zzz",
        "EEE dd-MMM-yy HH:mm:ss",
    };
    public static SimpleDateFormat __dateReceive[];
    static
    {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        tz.setID("GMT");
        __dateSend.setTimeZone(tz);

        __dateReceive = new SimpleDateFormat[__dateReceiveFmt.length];
        for(int i=0;i<__dateReceive.length;i++)
        {
            __dateReceive[i] =
                new SimpleDateFormat(__dateReceiveFmt[i],Locale.US);
            __dateReceive[i].setTimeZone(tz);
        }
    }

    /* -------------------------------------------------------------- */
    private Hashtable keyMap= new Hashtable(15);
    private Vector keys= new Vector(15);

    /* -------------------------------------------------------------- */
    /** Get enumeration of header names.
     * Returns an enumeration of strings representing the header names
     * for this request.
     */
    public Enumeration getHeaderNames()
    {
        return keys.elements();
    }

    /* -------------------------------------------------------------- */
    /**
     * Returns the value of a  header field, or null if not found.
     * The case of the header field name is ignored.
     * @param key the case-insensitive header field name
     */
    public String getHeader(String key)
    {
        return (String)keyMap.get(StringUtil.asciiToLowerCase(key));
    }

    /* -------------------------------------------------------------- */
    /**
     * Returns the value of a  header field, or null if not found.
     * The case of the header field name is ignored. Any parameters
     * found in the header are stripped.
     * @param key the case-insensitive header field name
     */
    String getHeaderNoParams(String key)
    {
        String val = getHeader(key);
        if (val!=null)
        {
            int sc=val.indexOf(';');
            if (sc>0)
                val=val.substring(0,sc);
        }
        return val;
    }

    /* -------------------------------------------------------------- */
    /** Set a header field.
     */
    public void setHeader(String key,String value)
    {
        String lkey = StringUtil.asciiToLowerCase(key);

        if (value==null)
        {
            keyMap.remove(lkey);
            keys.removeElement(key);
            keys.removeElement(lkey);
        }
        else
        {
            if (keyMap.put(lkey,value)==null)
                keys.addElement(key);
        }
    }

    /* -------------------------------------------------------------- */
    /** Read HttpHeaders from inputStream.
     */
    public void read(HttpInputStream in)
    throws IOException
    {
        com.mortbay.HTTP.HttpInputStream.CharBuffer cbuf;
        char[] lbuf=null;
        String last=null;
        while ((cbuf=in.readCharBufferLine())!=null)
        {
            // check for empty line to end headers
            if (cbuf.size==0)
                break;

            // check space in the lowercase buffer
            char[] buf = cbuf.chars;
            if (lbuf==null || lbuf.length < cbuf.size)
                lbuf= new char[cbuf.chars.length];

            // setup loop state machine
            int state=0;
            int i1=-1;
            int i2=-1;
            String key=null;
            String lkey=null;

            // loop for all chars in buffer
            for (int i=0;i<cbuf.size;i++)
            {
                char c=buf[i];

                switch(state)
                {
                  case 0: // leading white
                      if (c==' ' || c=='\t')
                      {
                          // continuation line
                          state=2;
                          continue;
                      }
                      state=1;
                      i1=i;
                      i2=i-1;
                  case 1: // reading key
                      if (c==':')
                      {
                          key=new String(buf,i1,i2-i1+1);
                          lkey=new String(lbuf,i1,i2-i1+1);
                          state=2;
                          i1=i;i2=i-1;
                          continue;
                      }
                      if (c>='A'&&c<='Z')
                      {
                          lbuf[i]=(char)(('a'-'A')+c);
                          i2=i;
                      }
                      else
                      {
                          lbuf[i]=c;
                          if (c!=' ' && c!='\t')
                              i2=i;
                      }
                      continue;

                  case 2: // skip whitespace after :
                      if (c==' ' || c=='\t')
                          continue;
                      state=3;
                      i1=i;
                      i2=i-1;

                  case 3: // looking for last non-white
                      if (c!=' ' && c!='\t')
                          i2=i;
                }
                continue;
            }

            if (lkey==null || lkey.length()==0)
            {
                if (state>=2 && last!=null)
                {
                    // Continuation line
                    String existing=(String)keyMap.get(last);
                    StringBuffer sb = new StringBuffer(existing);
                    sb.append(' ');
                    sb.append(buf,i1,i2-i1+1);

                    keyMap.put(last,sb.toString());
                }
                continue;
            }

            // Handle repeated headers
            String existing=(String)keyMap.get(lkey);
            if (existing!=null)
            {
                if (__singleValuedMap.containsKey(lkey))
                {
                    Code.warning("Ignored duplicate single value header: "+
                                 key);

                    // XXX Don't throw here as IE4 breaks the rules
                    // throw new IOException("Duplicate single value headers");
                }
                else
                {
                    StringBuffer sb = new StringBuffer(existing);
                    sb.append(',');
                    sb.append(buf,i1,i2-i1+1);
                    keyMap.put(lkey,sb.toString());
                }
            }
            else
            {
                keyMap.put(lkey,new String(buf,i1,i2-i1+1));
                keys.addElement(key);
                last=lkey;
            }
        }

    }

    /* -------------------------------------------------------------- */
    public void dump()
    {
	    System.err.println(keyMap);
	    System.err.println(keys);
	    System.err.println(getHeader(IfModifiedSince));
    }


    /* -------------------------------------------------------------- */
    /* Write Extra HTTP headers.
     */
    protected void write(Writer writer, String extra)
        throws IOException
    {
        synchronized(writer)   // Lock on the same object writer.write will use
        {
            int size=keys.size();
            for(int k=0;k<size;k++)
            {
                String key = (String)keys.elementAt(k);
                String value = getHeader(key);
                writer.write(key);
                writer.write(COLON);
                writer.write(value);
                writer.write(CRLF);
            }
            if (extra!=null&&extra.length()>0)
            {
                writer.write(extra);
                writer.write(CRLF);
            }
            writer.write(CRLF);
        }
    }


    /* -------------------------------------------------------------- */
    protected void write(OutputStream out)
    throws IOException
    {
        Writer writer=new OutputStreamWriter(out,"ISO8859_1");
        write(writer,null);
        writer.flush();
    }

    /* -------------------------------------------------------------- */
    protected void write(Writer writer)
    throws IOException
    {
        write(writer,null);
    }

    /* -------------------------------------------------------------- */
    public String toString()
    {
        try
        {
            StringWriter sw = new StringWriter();
            write(sw,null);
            return sw.toString();
        }
        catch(Exception e)
        {}
        return null;
    }

    /* -------------------------------------------------------------- */
    /** Get a header as an integer value.
     * Returns the value of an integer header field, or -1 if not found.
     * The case of the header field name is ignored.
     * @param name the case-insensitive header field name
     */
    public  int getIntHeader(String name)
    {
        String val = getHeaderNoParams(name);
        if (val!=null)
        {
            return Integer.parseInt(val);
        }
        return -1;
    }

    /* -------------------------------------------------------------- */
    /** Get a header as a date value.
     * Returns the value of a date header field, or -1 if not found.
     * The case of the header field name is ignored.
     * @param name the case-insensitive header field name
     */
    public long getDateHeader(String name)
    {
        String val = getHeaderNoParams(name);
        if (val!=null)
        {
            for (int i=0;i<__dateReceive.length;i++)
            {
                try{
                    Date date=(Date)__dateReceive[i].parseObject(val);
                    return date.getTime();
                }
                catch(java.lang.Exception e)
                {}
            }
            if (val.endsWith(" GMT"))
            {
                val=val.substring(0,val.length()-4);
                for (int i=0;i<__dateReceive.length;i++)
                {
                    try{
                        Code.debug("TRY ",val," against ",__dateReceive[i].toPattern());
                        Date date=(Date)__dateReceive[i].parseObject(val);
                        Code.debug("GOT ",date);
                        return date.getTime();
                    }
                    catch(java.lang.Exception e)
                    {
                        Code.ignore(e);
                    }
                }
            }
        }
        return -1;
    }

    /* -------------------------------------------------------------- */
   /**
     * Sets the value of an integer header field.
     * @param name the header field name
     * @param value the header field integer value
     */
    public void setIntHeader(String name, int value)
    {
        setHeader(name, Integer.toString(value));
    }

    /* -------------------------------------------------------------- */
    /**
     * Sets the value of a date header field.
     * @param name the header field name
     * @param value the header field date value
     */
    public void setDateHeader(String name, long date)
    {
        setHeader(name, __dateSend.format(date));
    }


    /* ------------------------------------------------------------ */
    /** Destroy the header.
     * Help the garbage collector by null everything that we can.
     */
    public void destroy()
    {
        if (keyMap!=null)
        {
            keyMap.clear();
            keyMap=null;
        }
        if (keys!=null)
        {
            keys.removeAllElements();
            keys=null;
        }
    }
}














