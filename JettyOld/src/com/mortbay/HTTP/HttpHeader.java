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
    public final static String UserAgent="User-Agent";
    public final static String IfModifiedSince="If-Modified-Since";
    public final static String IfUnmodifiedSince="If-Unmodified-Since";
    
    /* -------------------------------------------------------------- */
    public final static String CRLF = "\015\012";
    public final static byte[] __CRLF = {(byte)'\015',(byte)'\012'};
    public final static byte[] __COLON = {(byte)':',(byte)' '};
    
    public final static String HTTP_1_0 ="HTTP/1.0"   ;
    public final static String HTTP_1_1 ="HTTP/1.1"   ;

    /* -------------------------------------------------------------- */
    public final static SimpleDateFormat __dateSend = 
	new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
    public final static SimpleDateFormat __dateReceive[] =
    {
	new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"),
	new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss zzz"),
	new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy"),
	new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz"),
	new SimpleDateFormat("dd-MMM-yy HH:mm:ss zzz"),
	new SimpleDateFormat("dd MMM yyyy HH:mm:ss"),
	new SimpleDateFormat("dd-MMM-yy HH:mm:ss"),
	new SimpleDateFormat("MMM dd HH:mm:ss yyyy")
    };
    static
    {
	TimeZone tz = TimeZone.getTimeZone("GMT");
	tz.setID("GMT");
	__dateSend.setTimeZone(tz);
	for(int i=0;i<__dateReceive.length;i++)
	    __dateReceive[i].setTimeZone(tz);
    }
    
    /* -------------------------------------------------------------- */
    private Hashtable keyMap= new Hashtable();
    private Vector keys= new Vector();

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
     * @param keythe case-insensitive header field name
     */
    public String getHeader(String key)
    {
	return (String)keyMap.get(StringUtil.asciiToLowerCase(key));
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
	String s;
	while ((s=in.readLine())!=null)
	{
	    if (s.length()==0)
	       break;
	    
	    int c = s.indexOf(':',0);
	    if (c>0)
	    {
		String key = s.substring(0,c).trim();
		String value = s.substring(c+1,s.length()).trim();
		String lkey = StringUtil.asciiToLowerCase(key);
		String ev = (String) keyMap.get(lkey);
		if (ev!=null)
		    keyMap.put(lkey,ev+';'+value);
		else
		{
		    keyMap.put(lkey,value);
		    keys.addElement(key);
		}
	    }
	    else
		Code.warning("header field without ':'");
	}
    }

    
    /* -------------------------------------------------------------- */
    /* Write Extra HTTP headers.
     */
    protected void write(OutputStream out, String extra)
    throws IOException
    {
	ByteArrayOutputStream buf= new ByteArrayOutputStream();
	synchronized(buf)
	{
	    int size=keys.size();
	    for(int k=0;k<size;k++)
	    {
		String key = (String)keys.elementAt(k);
		String value = getHeader(key);
		buf.write(key.getBytes());
		buf.write(__COLON);
		buf.write(value.getBytes());
		buf.write(__CRLF);
	    }
	    if (extra!=null&&extra.length()>0)
	    {
		buf.write(extra.getBytes());
		buf.write(__CRLF);
	    }
	    buf.write(__CRLF);
	    buf.writeTo(out);
	}
	
	out.flush();
    }
    
    /* -------------------------------------------------------------- */
    protected void write(OutputStream out)
    throws IOException
    {
	write(out,null);
    }
    
    /* -------------------------------------------------------------- */
    public String toString()
    {
	try
	{
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    write(bos);
	    return bos.toString();
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
	String val = getHeader(name);
	if (val!=null)
	{
	    int sc=val.indexOf(';');
	    if (sc>0)
		val=val.substring(0,sc);
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
	String val = getHeader(name);
	if (val!=null)
	{
	    int sc=val.indexOf(';');
	    if (sc>0)
		val=val.substring(0,sc);
	    for (int i=0;i<__dateReceive.length;i++)
	    {
		try{
		    Date date=(Date)__dateReceive[i].parseObject(val);
		    return date.getTime();
		}
		catch(java.lang.Exception e)
		{}
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
	setHeader(name, __dateSend.format(new Date(date)));
    }
}


