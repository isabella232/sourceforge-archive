// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.sun.java.util.collections.*;
import com.sun.java.util.collections.Vector;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import java.text.*;


/* ------------------------------------------------------------ */
/** HTTP Fields.
 * A collection of HTTP header and or Footer fields.
 * This class is not synchronized and needs to be protected from
 * concurrent access.
 * @see
 * @version 1.0 Wed Sep 29 1999
 * @author Greg Wilkins (gregw)
 */
public class HttpFields extends HashMap
{
    /* ------------------------------------------------------------ */
    /** General Fields
     */
    public final static String
        CacheControl = "Cache-Control",
        Connection = "Connection",
        Date = "Date",
        Pragma = "Pragma",
        Trailer = "Trailer",
        TransferEncoding = "Transfer-Encoding",
        Upgrade = "Upgrade",
        Via = "Via",
        Warning = "Warning";
        
    /* ------------------------------------------------------------ */
    /** Entity Fields
     */
    public final static String
        Allow = "Allow",
        ContentEncoding = "Content-Encoding",
        ContentLanguage = "Content-Language",
        ContentLength = "Content-Length",
        ContentLocation = "Content-Location",
        ContentMD5 = "Content-MD5",
        ContentRange = "Content-Range",
        ContentType = "Content-Type",
        Expires = "Expires",
        LastModified = "Last-Modified";
    
    /* ------------------------------------------------------------ */
    /** Request Fields
     */
    public final static String
        Accept = "Accept",
        AcceptCharset = "Accept-Charset",
        AcceptEncoding = "Accept-Encoding",
        AcceptLanguage = "Accept-Language",
        Authorization = "Authorization",
        Expect = "Expect",
        From = "From",
        Host = "Host",
        IfMatch = "If-Match",
        IfModifiedSince = "If-Modified-Since",
        IfNoneMatch = "If-None-Match",
        IfRange = "If-Range",
        IfUnmodifiedSince = "If-Unmodified-Since",
        MaxForwards = "Max-Forwards",
        ProxyAuthentication = "Proxy-Authentication",
        Range = "Range",
        Referer = "Referer",
        TE = "TE",
        UserAgent = "User-Agent";

    /* ------------------------------------------------------------ */
    /** Response Fields
     */
    public final static String
        AcceptRanges = "Accept-Ranges",
        Age = "Age",
        ETag = "ETag",
        Location = "Location",
        ProxyAuthenticate = "Proxy-Authenticate",
        RetryAfter = "Retry-After",
        Server = "Server",
        Vary = "Vary",
        WwwAuthenticate = "WWW-Authenticate";
     
    /* ------------------------------------------------------------ */
    /** Other Fields
     */
    public final static String Cookie = "Cookie";
    public final static String SetCookie = "Set-Cookie";
    
    /* ------------------------------------------------------------ */
    /** Fields Values
     */    
    public final static String Chunked = "chunked";
    public final static String Close = "close";
    public final static String WwwFormUrlEncode = "application/x-www-form-urlencoded";
    
    
    /* ------------------------------------------------------------ */
    /** Single valued Fields
     */  
    public final static String[] __SingleValued=
    {
        Age,Authorization,ContentLength,ContentLocation,ContentMD5,
        ContentRange,ContentType,Date,ETag,Expires,From,Host,
        IfModifiedSince,IfRange,IfUnmodifiedSince,LastModified,
        Location,MaxForwards,ProxyAuthentication,Range,Referer,
        RetryAfter,Server,UserAgent
    };
    public final static Set __singleValuedSet=new HashSet(37);
    static
    {
        for (int i=0;i<__SingleValued.length;i++)
            __singleValuedSet
                .add(StringUtil.asciiToLowerCase(__SingleValued[i]));
    }
    
    /* ------------------------------------------------------------ */
    public final static String __CRLF = "\015\012";
    public final static byte[] __CRLF_B = {(byte)'\015',(byte)'\012'};
    public final static String __COLON = ": ";
    public final static byte[] __COLON_B = {(byte)':',(byte)' '};

    /* -------------------------------------------------------------- */
    public final static DateCache __dateSend = 
        new DateCache("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
    public final static SimpleDateFormat __dateReceive[] =
    {
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz"),
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss"),
        new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss zzz"),
        new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss"),
        new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss zzz"),
        new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss"),
        new SimpleDateFormat("EEE MMM-dd-yyyy HH:mm:ss zzz"),
        new SimpleDateFormat("EEE MMM-dd-yyyy HH:mm:ss"),
        new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz"),
        new SimpleDateFormat("dd MMM yyyy HH:mm:ss"),
        new SimpleDateFormat("dd-MMM-yy HH:mm:ss zzz"),
        new SimpleDateFormat("dd-MMM-yy HH:mm:ss"),
        new SimpleDateFormat("MMM dd HH:mm:ss yyyy zzz"),
        new SimpleDateFormat("MMM dd HH:mm:ss yyyy"),
        new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy zzz"),
        new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy"),
        new SimpleDateFormat("EEE, MMM dd HH:mm:ss yyyy zzz"),
        new SimpleDateFormat("EEE, MMM dd HH:mm:ss yyyy"),
        new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss zzz"),
        new SimpleDateFormat("EEE, dd-MMM-yy HH:mm:ss"),
        new SimpleDateFormat("EEE dd-MMM-yy HH:mm:ss zzz"),
        new SimpleDateFormat("EEE dd-MMM-yy HH:mm:ss"),
    };
    static
    {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        tz.setID("GMT");
        __dateSend.getFormat().setTimeZone(tz);
        for(int i=0;i<__dateReceive.length;i++)
            __dateReceive[i].setTimeZone(tz);
    }
    
    /* -------------------------------------------------------------- */
    private ArrayList _names= new ArrayList(15);
    private List _readOnlyNames=null;

    /* -------------------------------------------------------------- */
    /** Get enumeration of header _names.
     * Returns an enumeration of strings representing the header _names
     * for this request. 
     */
    public List getFieldNames()
    {
        if (_readOnlyNames==null)
            _readOnlyNames=Collections.unmodifiableList(_names);
        return _readOnlyNames;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param name 
     * @return 
     */
    public boolean containsKey(Object name)
    {
        return containsKey(StringUtil.asciiToLowerCase(name.toString()));
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Returns the value of a  field, or null if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     */
    public Object get(Object name)
    {
        return super.get(StringUtil.asciiToLowerCase(name.toString()));
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Returns the value of a  field, or null if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     */
    public String get(String name)
    {
        return (String)super.get(StringUtil.asciiToLowerCase(name));
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Returns multiple values of a field, or null if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     */
    public List getValues(String name)
    {
        String v=get(name);
        if (v==null)
            return null;

        List list = new ArrayList();

        QuotedStringTokenizer tok =
            new QuotedStringTokenizer(v,", ",true,false);
        String value=null;
        boolean space=false;
        while (tok.hasMoreTokens())
        {
            String token=tok.nextToken();
            if (",".equals(token))
            {
                if (value!=null)
                    list.add(value);
                value=null;
            }
            else if (" ".equals(token))
            {
                space=(value!=null);
            }
            else if (value==null)
            {
                value=token;
                space=false;
            }
            else if (space)
            {
                value+=" "+token;
                space=false;
            }
            else
                value+=token;
        }
        if(value!=null)
            list.add(value);
            
        return list;
    }
    
    /* -------------------------------------------------------------- */
    /** Set a field.
     * @param name the name of the field
     * @param value the value of the field. If null the field is cleared.
     */
    public Object put(Object name,Object value)
    {
        return put(name.toString(),value.toString());
    }
        
    /* -------------------------------------------------------------- */
    /** Set a field.
     * @param name the name of the field
     * @param value the value of the field. If null the field is cleared.
     */
    public String put(String name,String value)
    {
        if (value==null)
            return remove(name);
        
        String lname = StringUtil.asciiToLowerCase(name);
        Object old=super.put(lname,value);
        if (old==null)
        {
            _names.add(name);
            return null;
        }
        return old.toString();
    }

    /* -------------------------------------------------------------- */
    /** Add to or set a field.
     * If the field is allowed to have multiple values, add will build
     * a coma separated list for the value.
     * The values are quoted if they contain comas or quote characters.
     * @param name the name of the field
     * @param value the value of the field.
     * @exception IllegalArgumentException If the name is a single
     *            valued field
     */
    public void add(String name,String value)
        throws IllegalArgumentException
    {
        if (value==null)
            return;
        String lname = StringUtil.asciiToLowerCase(name);
        if (__singleValuedSet.contains(lname))
            throw new IllegalArgumentException("Cannot add single valued field: "+name);
        value=QuotedStringTokenizer.quote(value,", ");
        Object existing=super.get(lname);
        if(existing!=null)
            super.put(lname,existing+", "+value);
        else
            put(name, value);
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a field.
     * @param name 
     */
    public Object remove(Object name)
    {
        return remove(name.toString());
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a field.
     * @param name 
     */
    public String remove(String name)
    {
        String lname = StringUtil.asciiToLowerCase(name);
        _names.remove(name);
        _names.remove(lname);
        Object old=super.remove(lname);
        if (old==null)
            return null;
        return old.toString();
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
     * Returns the value of an integer field, or -1 if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     */
    public  int getIntField(String name)
    {
        String val = valueParameters(get(name),null);
        if (val!=null)
        {
            return Integer.parseInt(val);
        }
        return -1;
    }
    
    /* -------------------------------------------------------------- */
    /** Get a header as a date value.
     * Returns the value of a date field, or -1 if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     */
    public long getDateField(String name)
    {
        String val = valueParameters(get(name),null);
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
     * Sets the value of an integer field.
     * @param name the field name
     * @param value the field integer value
     */
    public void putIntField(String name, int value)
    {
        put(name, Integer.toString(value));
    }

    /* -------------------------------------------------------------- */
    /**
     * Sets the value of a date field.
     * @param name the field name
     * @param value the field date value
     */
    public void putDateField(String name, long date)
    {
        put(name, __dateSend.format(date));
    }

    /* -------------------------------------------------------------- */
    /** Read HttpHeaders from inputStream.
     */
    public void read(InputStream in)
    throws IOException
    {
        ChunkableInputStream cin;
        if (in instanceof ChunkableInputStream)
            cin=(ChunkableInputStream)in;
        else
            cin=new ChunkableInputStream(in);
        
        String last=null;
        char[] buf=null;
        int size=0;
        char[] lbuf=null;
        com.mortbay.Util.LineInput$LineBuffer line_buffer;
        
        while ((line_buffer=cin.readLine(cin.__maxLineLength))!=null)
        {
            // check space in the lowercase buffer
            buf=line_buffer.buffer;
            size=line_buffer.size;
            if (size==0)
                break;
            if (lbuf==null || lbuf.length<line_buffer.size)
                lbuf= new char[buf.length];

            // setup loop state machine
            int state=0;
            int i1=-1;
            int i2=-1;
            String name=null;
            String lname=null;

            // loop for all chars in buffer
            for (int i=0;i<line_buffer.size;i++)
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
                  case 1: // reading name
                      if (c==':')
                      {
                          name=new String(buf,i1,i2-i1+1);
                          lname=new String(lbuf,i1,i2-i1+1);  
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

            if (lname==null || lname.length()==0)
            {
                if (state>=2 && last!=null)
                {
                    // Continuation line
                    String existing=(String)get(last);
                    StringBuffer sb = new StringBuffer(existing);
                    sb.append(' ');
                    sb.append(new String(buf,i1,i2-i1+1));
                    put(last,sb.toString());
                }
                continue;
            }
            
            // Handle repeated headers
            String existing=(String)get(lname);
            if (existing!=null)
            {
                if (__singleValuedSet.contains(lname))
                {
                    Code.warning("Ignored duplicate single value header: "+
                                 name);
                }
                else
                {
                    StringBuffer sb = new StringBuffer(existing);
                    sb.append(", ");
                    sb.append(new String(buf,i1,i2-i1+1));
                    put(lname,sb.toString());
                }
            }
            else
            {
                super.put(lname,new String(buf,i1,i2-i1+1));
                _names.add(name);
                last=lname;
            }
        }
    }

    
    /* -------------------------------------------------------------- */
    /* Write Extra HTTP headers.
     */
    protected void write(OutputStream out, HttpFields extra)
        throws IOException
    {
        synchronized(out)
        {
            int size=_names.size();
            for(int k=0;k<size;k++)
            {
                String name = (String)_names.get(k);
                String value = get(name);
                out.write(name.getBytes());
                out.write(__COLON_B);
                out.write(value.getBytes());
                out.write(__CRLF_B);
            }
            if (extra!=null)
                extra.write(out,null);
            else
                out.write(__CRLF_B);
        
            out.flush();
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Destroy the header.
     * Help the garbage collector by null everything that we can.
     */
    public void destroy()
    {
        clear();
        if (_names!=null)
            _names.clear();
        _names=null;
        _readOnlyNames=null;
    }

    
    /* ------------------------------------------------------------ */
    /** Get field value parameters.
     * Some field values can have parameters.  This method separates
     * the value from the parameters and optionally populates a
     * map with the paramters. For example:<PRE>
     *   FieldName : Value ; param1=val1 ; param2=val2
     * </PRE>
     * @param value The Field value, possibly with parameteres.
     * @param parameters A map to populate with the parameters, or null
     * @return The value.
     */
    public static String valueParameters(String value, Map parameters)
    {
        if (value==null)
            return null;
        
        int i = value.indexOf(";");
        if (i<0)
            return value;
        if (parameters==null)
            return value.substring(0,i).trim();

        StringTokenizer tok1 =
            new QuotedStringTokenizer(value.substring(i),";",false,true);
        while(tok1.hasMoreTokens())
        {
            String token=tok1.nextToken();
            StringTokenizer tok2 =
                new QuotedStringTokenizer(token,"= ");
            if (tok2.hasMoreTokens())
            {
                String paramName=tok2.nextToken();
                String paramVal=null;
                if (tok2.hasMoreTokens())
                    paramVal=tok2.nextToken();
                parameters.put(paramName,paramVal);
            }
        }
        
        return value.substring(0,i).trim();
    }

}

