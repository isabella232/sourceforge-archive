// ===========================================================================
// Copyright (c) 1996-2003 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.http;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.Portable;
import org.mortbay.util.DateCache;
import org.mortbay.util.QuotedStringTokenizer;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** HTTP Fields.
 * A collection of HTTP header and or Trailer fields.
 * This class is not synchronized and needs to be protected from
 * concurrent access.
 *
 * This class is not synchronized as it is expected that modifications
 * will only be performed by a single thread.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class HttpHeader
{
    /* ------------------------------------------------------------ */
    public final static String __separators = ", \t";    

    /* -------------------------------------------------------------- */
    public final static DateCache __dateCache = 
        new DateCache("EEE, dd MMM yyyy HH:mm:ss 'GMT'",Locale.US);
    public final static SimpleDateFormat __dateSend = 
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'",Locale.US);
    public final static SimpleDateFormat __dateCookie = 
        new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss 'GMT'",Locale.US);
    public final static String __01Jan1970=
        '"'+HttpHeader.__dateSend.format(new Date(0))+'"';
    
    /* ------------------------------------------------------------ */
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
        __dateCache.setTimeZone(tz); 
        __dateCookie.setTimeZone(tz);
       
        __dateReceive = new SimpleDateFormat[__dateReceiveFmt.length];
        for(int i=0;i<__dateReceive.length;i++)
        {
            __dateReceive[i] =
                new SimpleDateFormat(__dateReceiveFmt[i],Locale.US);
            __dateReceive[i].setTimeZone(tz);
        }
    }

    
    /* -------------------------------------------------------------- */
    protected Buffer _method;
    protected Buffer _uri;
    protected Buffer _version;
    protected int _status=0;
    protected Buffer _reason;   
    protected ArrayList _fields=new ArrayList(20);
    protected int _revision;
    protected HashMap _bufferMap=new HashMap(32);

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public HttpHeader()
    {}

    
    /**
     * @return
     */
    public Buffer getMethod()
    {
        return _method;
    }

    /**
     * @return
     */
    public Buffer getReason()
    {
        return _reason;
    }

    /**
     * @return
     */
    public int getStatus()
    {
        return _status;
    }

    /**
     * @return
     */
    public Buffer getUri()
    {
        return _uri;
    }

    /**
     * @return HttpVersions cache Buffer
     */
    public Buffer getVersion()
    {
        return _version;
    }
    
    /**
     * @return HttpVersions cache ordinal
     */
    public int getVersionOrdinal()
    {
        return HttpVersions.getOrdinal(_version);
    }

    /**
     * @param buffer
     */
    public void setMethod(Buffer buffer)
    {
        _method= buffer;
    }

    /**
     * @param method
     */
    public void setMethod(String method)
    {
        _method= HttpMethods.CACHE.get(method);
    }

    /**
     * @param ordinal
     */
    public void setMethod(int ordinal)
    {
        _method= HttpMethods.CACHE.get(ordinal);
    }

    /**
     * @param buffer
     */
    public void setReason(Buffer buffer)
    {
        _reason= buffer;
    }

    /**
     * @param i
     */
    public void setStatus(int i)
    {
        _status= i;
    }

    /**
     * @param buffer
     */
    public void setURI(Buffer buffer)
    {
        _uri= buffer;
    }

    /**
     * @param buffer
     */
    public void setURI(String uri)
    {
        _uri= new ByteArrayBuffer(uri);
    }

    /**
     * @param buffer
     */
    public void setVersion(Buffer buffer)
    {
        _version= buffer;
    }

    /**
     * @param buffer
     */
    public void setVersion(String version)
    {
        _version= HttpVersions.CACHE.lookup(version);
    }

    /**
     * @param buffer
     */
    public void setVersion(int ordinal)
    {
        _version= HttpVersions.CACHE.get(ordinal);
    }
    
    /* -------------------------------------------------------------- */
    /** Get enumeration of header _names.
     * Returns an enumeration of strings representing the header _names
     * for this request. 
     */
    public Enumeration getFieldNames()
    {
        return new Enumeration()
            {
                int i=0;
                Field field=null;

                public boolean hasMoreElements()
                {
                    if (field!=null)
                        return true;
                    while (i<_fields.size())
                    {
                        Field f=(Field)_fields.get(i++);
                        if (f!=null &&  f._prev==null && f._revision==_revision)
                        {
                            field=f;
                            return true;
                        }
                    }
                    return false;
                }

                public Object nextElement()
                    throws NoSuchElementException
                {
                    if (field!=null || hasMoreElements())
                    {
                        String n=field._name.toString();
                        field=null;
                        return n;
                    }
                    throw new NoSuchElementException();
                }
            };
    }
    
    /* -------------------------------------------------------------- */
    /** Get enumeration of header _names.
     * Returns an enumeration of strings representing the header _names
     * for this request. 
     */
    public Enumeration getFieldNameBuffers()
    {
        return new Enumeration()
        {
            int i= 0;
            Field field= null;

            public boolean hasMoreElements()
            {
                if (field != null)
                    return true;
                while (i < _fields.size())
                {
                    Field f= (Field)_fields.get(i++);
                    if (f != null && f._prev == null && f._revision == _revision)
                    {
                        field= f;
                        return true;
                    }
                }
                return false;
            }

            public Object nextElement() throws NoSuchElementException
            {
                if (field != null || hasMoreElements())
                {
                    Buffer n= field._name;
                    field= null;
                    return n;
                }
                throw new NoSuchElementException();
            }
        };
    }
    /* ------------------------------------------------------------ */
    protected Field getField(String name)
    {       
        return (Field)_bufferMap.get(HttpHeaders.CACHE.lookup(name));
    }
    
    /* ------------------------------------------------------------ */
    protected Field getField(Buffer name)
    {       
        return (Field)_bufferMap.get(name);
    }
    
    /* ------------------------------------------------------------ */
    public boolean containsKey(String name)
    {
        return _bufferMap.containsKey(HttpHeaders.CACHE.lookup(name));
    }
    
    /* -------------------------------------------------------------- */
    /**
     * @return the value of a field, or null if not found. For
     * multiple fields of the same name, only the first is returned.
     * @param name the case-insensitive field name
     */
    public String get(String name)
    {
        Field field=getField(name);
        if (field!=null && field._revision==_revision)
            return field._value.toString();
        return null;
    }
    
    /* -------------------------------------------------------------- */
    /**
     * @return the value of a field, or null if not found. For
     * multiple fields of the same name, only the first is returned.
     * @param name the case-insensitive field name
     */
    public Buffer get(Buffer name)
    {
        Field field=getField(name);
        if (field!=null && field._revision==_revision)
            return field._value;
        return null;
    }
    
    /* -------------------------------------------------------------- */
    /** Get multi headers
     * @return Enumeration of the values, or null if no such header.
     * @param name the case-insensitive field name
     */
    public Enumeration getValues(String name)
    {
        final Field field= getField(name);
        if (field == null)
            return null;

        return new Enumeration()
        {
            Field f= field;
            public boolean hasMoreElements()
            {
                while (f != null && f._revision != _revision)
                    f= f._next;
                return f != null;
            }
            public Object nextElement() throws NoSuchElementException
            {
                if (f == null)
                    throw new NoSuchElementException();
                Field n= f;
                do f= f._next;
                while (f != null && f._revision != _revision);
                return n._value.toString();
            }
        };
    }
    
    /* -------------------------------------------------------------- */
    /** Get multi headers
     * @return Enumeration of the value Buffers, or null if no such header.
     * @param name the case-insensitive field name
     */
    public Enumeration getValues(Buffer name)
    {
        final Field field= getField(name);

        if (field == null)
            return null;

        return new Enumeration()
        {
            Field f= field;
            public boolean hasMoreElements()
            {
                while (f != null && f._revision != _revision)
                    f= f._next;
                return f != null;
            }
            public Object nextElement() throws NoSuchElementException
            {
                if (f == null)
                    throw new NoSuchElementException();
                Field n= f;
                do f= f._next;
                while (f != null && f._revision != _revision);
                return n._value;
            }
        };
    }
    
    /* -------------------------------------------------------------- */
    /** Get multi field values with separator.
     * The multiple values can be represented as separate headers of
     * the same name, or by a single header using the separator(s), or
     * a combination of both. Separators may be quoted.
     * @param name the case-insensitive field name
     * @param separators String of separators.
     * @return Enumeration of the values, or null if no such header.
     */
    public Enumeration getValues(String name,final String separators)
    {
        final Enumeration e = getValues(name);
        if (e==null)
            return null;
        return new Enumeration()
            {
                QuotedStringTokenizer tok=null;
                public boolean hasMoreElements()
                {
                    if (tok!=null && tok.hasMoreElements())
                            return true;
                    while (e.hasMoreElements())
                    {
                        String value=(String)e.nextElement();
                        tok=new QuotedStringTokenizer(value,separators,false,false);
                        if (tok.hasMoreElements())
                            return true;
                    }
                    tok=null;
                    return false;
                }
                        
                public Object nextElement()
                    throws NoSuchElementException
                {
                    if (!hasMoreElements())
                        throw new NoSuchElementException();
                    String next=(String) tok.nextElement();
		    if (next!=null)next=next.trim();
		    return next;
                }
            };
    }
    
    /* -------------------------------------------------------------- */
    /** Set a field.
     * @param name the name of the field
     * @param value the value of the field. If null the field is cleared.
     */
    public void put(String name,String value)
    {
        Buffer n=HttpHeaders.CACHE.lookup(name);
        Buffer v=HttpHeaderValues.CACHE.lookup(value);
        put(n,v);
    }

    /* -------------------------------------------------------------- */
    /** Set a field.
     * @param name the name of the field
     * @param value the value of the field. If null the field is cleared.
     */
    public void put(Buffer name,Buffer value)
    {
        if (value==null)
            {remove(name);return;} 
        
        Field field=(Field)_bufferMap.get(name);
        
        // Look for value to replace.
        if (field!=null)
        {
            field.reset(value,_revision);
            field=field._next;
            while(field!=null)
            {
                field.clear();
                field=field._next;
            }
            return;    
        }
        else
        {
            // new value;
            field=new Field(name,value,_revision);
            _fields.add(field);
            _bufferMap.put(name,field);
        }
    }
    
        
    /* -------------------------------------------------------------- */
    /** Set a field.
     * @param name the name of the field
     * @param list the List value of the field. If null the field is cleared.
     */
    public void put(String name,List list)
    {
        if (list==null || list.size()==0)
        {
            remove(name);
            return;
        }
        Buffer n = HttpHeaders.CACHE.lookup(name);
        
        Object v=list.get(0);
        if (v!=null)
            put(n,HttpHeaderValues.CACHE.lookup(v.toString()));
        else
            remove(n);
        
        if (list.size()>1)
        {    
            java.util.Iterator iter = list.iterator();
            iter.next();
            while(iter.hasNext())
            {
                v=iter.next();
                if (v!=null)
                    put(n,HttpHeaderValues.CACHE.lookup(v.toString()));
            }
        }
    }

    
    /* -------------------------------------------------------------- */
    /** Add to or set a field.
     * If the field is allowed to have multiple values, add will add
     * multiple headers of the same name.
     * @param name the name of the field
     * @param value the value of the field.
     * @exception IllegalArgumentException If the name is a single
     *            valued field and already has a value.
     */
    public void add(String name,String value)
        throws IllegalArgumentException
    {
        Buffer n=HttpHeaders.CACHE.lookup(name);
        Buffer v=HttpHeaderValues.CACHE.lookup(value);
        add(n,v);
    }

    /* -------------------------------------------------------------- */
    /** Add to or set a field.
     * If the field is allowed to have multiple values, add will add
     * multiple headers of the same name.
     * @param name the name of the field
     * @param value the value of the field.
     * @exception IllegalArgumentException If the name is a single
     *            valued field and already has a value.
     */
    public void add(Buffer name,Buffer value)
        throws IllegalArgumentException
    {
    
        if (value==null)
            throw new IllegalArgumentException("null value");
        
        Field field=(Field)_bufferMap.get(name);
        Field last=null;
        if (field!=null)
        {
            while(field!=null && field._revision==_revision)
            {
                last=field;
                field=field._next;
            }
        }

        if (field!=null)    
            field.reset(value,_revision);
        else
        {
            // create the field
            field=new Field(name,value,_revision);
            
            // look for chain to add too
            if(last!=null)
            {
                field._prev=last;
                last._next=field;    
            }
            else
                _bufferMap.put(name, field);
                
            _fields.add(field);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a field.
     * @param name 
     */
    public void remove(String name)
    {
        remove (HttpHeaders.CACHE.lookup(name));
    }

    /* ------------------------------------------------------------ */
    /** Remove a field.
     * @param name 
     */
    public void remove(Buffer name)
    {
        Field field=(Field)_bufferMap.get(name);

        if (field!=null)
        {
            while(field!=null)
            {
                field.clear();
                field=field._next;
            }
        }     
    }
   
    /* -------------------------------------------------------------- */
    /** Get a header as an integer value.
     * Returns the value of an integer field or -1 if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     * @exception NumberFormatException If bad integer found
     */
    public int getIntField(String name)
        throws NumberFormatException
    {
        Field field = getField(name);
        if (field!=null)
            return BufferUtil.toInt(field._value);
        
        return -1;
    }
   
    /* -------------------------------------------------------------- */
    /** Get a header as an integer value.
     * Returns the value of an integer field or -1 if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     * @exception NumberFormatException If bad integer found
     */
    public int getIntField(Buffer name)
        throws NumberFormatException
    {
        Field field = getField(name);
        if (field!=null)
            return BufferUtil.toInt(field._value);
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
        if (val==null)
            return -1;

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
                    Date date=(Date)__dateReceive[i].parseObject(val);
                    return date.getTime();
                }
                catch(java.lang.Exception e)
                {}
            }
        }

        throw new IllegalArgumentException("Cannot convert date: "+val);
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
     * @param date the field date value
     */
    public void putDateField(String name, Date date)
    {
        put(name, __dateSend.format(date));
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Adds the value of a date field.
     * @param name the field name
     * @param date the field date value
     */
    public void addDateField(String name, Date date)
    {
        add(name, __dateSend.format(date));
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Adds the value of a date field.
     * @param name the field name
     * @param date the field date value
     */
    public void addDateField(String name, long date)
    {
        add(name, __dateSend.format(new Date(date)));
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Sets the value of a date field.
     * @param name the field name
     * @param date the field date value
     */
    public void putDateField(String name, long date)
    {
        put(name, __dateSend.format(new Date(date)));
    }

    
    /* -------------------------------------------------------------- */
    public void write(Writer writer)
        throws IOException
    {
        synchronized(writer)
        {
            if (_status>0)
            {
                if (_method!=null)
                    Portable.throwIllegalState("status and method");
                if (_version==null)
                    _version=HttpVersions.CACHE.get(HttpVersions.HTTP_1_1_ORDINAL);
                writer.write(_version.toString());
                writer.write(' ');
                writer.write(_status);
                writer.write(' ');
                if (_reason==null)
                {
                    _reason=HttpStatus.CACHE.get(_status);
                    if (_reason==null)
                        _reason=HttpStatus.CACHE.get(HttpStatus.ORDINAL_999_Unknown);
                }
                writer.write(_reason.toString());
            }
            else
            {
                if (_method==null)
                    Portable.throwIllegalState("no status or method");
                writer.write(_method.toString());
                writer.write(' ');
                writer.write(_uri.toString());
                if (_version!=null)
                {
                    writer.write(' ');
                    writer.write(_version.toString());
                }
            }
            writer.write(StringUtil.CRLF);
            for (int i=0;i<_fields.size();i++)
            {
                Field field=(Field)_fields.get(i);
                if (field!=null && field._revision==_revision)
                    field.write(writer);
            }
            writer.write(StringUtil.CRLF);
        }
    }
    
    /* -------------------------------------------------------------- */
    public void put(Buffer buffer) throws IOException
    {
        if (_status>0)
        {
            if (_method!=null)
                Portable.throwIllegalState("status and method");
            if (_version==null)
                _version=HttpVersions.CACHE.get(HttpVersions.HTTP_1_1_ORDINAL);
            buffer.put(_version);
            buffer.put((byte)' ');
            BufferUtil.putDecInt(buffer, _status);
            buffer.put((byte)' ');
            if (_reason==null)
            {
                _reason=HttpStatus.CACHE.get(_status);
                if (_reason==null)
                    _reason=HttpStatus.CACHE.get(HttpStatus.ORDINAL_999_Unknown);
            }
            buffer.put(_reason);
        }
        else
        {
            if (_method==null)
                Portable.throwIllegalState("no status or method");
            buffer.put(_method);
            buffer.put((byte)' ');
            buffer.put(_uri);
            if (_version!=null)
            {
                buffer.put((byte)' ');
                buffer.put(_version);
            }
        }
        BufferUtil.putCRLF(buffer);
        
        for (int i= 0; i < _fields.size(); i++)
        {
            Field field= (Field)_fields.get(i);
            if (field != null && field._revision == _revision)
                field.put(buffer);
        }
        BufferUtil.putCRLF(buffer);
    }
       
    /* -------------------------------------------------------------- */
    public String toString()
    {
        try
        {
            ByteArrayBuffer buffer = new ByteArrayBuffer(4096);
            put(buffer);
            return buffer.toString();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return null;
    }

    /* ------------------------------------------------------------ */
    /** Clear the header.
     */
    public void clear()
    {
        _method=null;
        _uri=null;
        _version=null;
        _status=0;
        _reason=null;
        
        _revision++;
        if (_revision>1000000)
        {
            _revision=0;
            for (int i=_fields.size();i-->0;)
            {
                Field field=(Field)_fields.get(i);
                if (field!=null)
                    field.clear();
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Destroy the header.
     * Help the garbage collector by null everything that we can.
     */
    public void destroy()
    {   
        for (int i=_fields.size();i-->0;)
        {
            Field field=(Field)_fields.get(i);
            if (field!=null)
                field.destroy();
        }
        _fields=null;
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
        
        int i = value.indexOf(';');
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

    /* ------------------------------------------------------------ */
    /** Add fields from another HttpFields instance.
     * Single valued fields are replaced, while all others are added.
     * @param fields 
     */
    public void add(HttpHeader fields)
    {
        if (fields==null)
            return;

        Enumeration enum = fields.getFieldNames();
        while( enum.hasMoreElements() )
        {
            String name = (String)enum.nextElement();
            Enumeration values = fields.getValues(name);
            while(values.hasMoreElements())
                add(name,(String)values.nextElement());
        }
    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static final class Field
    {
        Buffer _name;
        Buffer _value;
        Field _next;
        Field _prev;
        int _revision;

        /* ------------------------------------------------------------ */
        Field(Buffer name, Buffer value, int revision)
        {
            _name=name.asReadOnlyBuffer();
            _value=value.asReadOnlyBuffer();
            _next=null;
            _prev=null;
            _revision=revision;
        }
        
        void clear()
        {
            _revision=-1;
        }
        
        /* ------------------------------------------------------------ */
        void destroy()
        {
            _name=null;
            _value=null;
            _next=null;
            _prev=null;
        }
        
        /* ------------------------------------------------------------ */
        /** Reassign a value to this field.
         * Checks if the value is the same as that in the char array, if so
         * then just reuse existing value.
         */
        void reset(Buffer value, int revision)
        {  
            if (_value==null || !_value.equals(value))
                _value=value.asReadOnlyBuffer();
            _revision=revision;
        }
        
        /* ------------------------------------------------------------ */
        void write(Writer writer)
            throws IOException
        {
            writer.write(_name.toString());
            writer.write(":");
            writer.write(_value.toString());
            writer.write(StringUtil.CRLF);  
        }
        
        /* ------------------------------------------------------------ */
        void put(Buffer buffer)
            throws IOException
        {
           buffer.put(_name);
           buffer.put((byte)':');
           buffer.put((byte)' ');
           buffer.put(_value);
           BufferUtil.putCRLF(buffer);
        }

        /* ------------------------------------------------------------ */
        String getDisplayName()
        {
            return _name.toString();
        }
        
        /* ------------------------------------------------------------ */
        public String toString()
        {
            return ("["+
                (_prev==null?"":"<-")+
                getDisplayName()+"="+_value+
                (_next==null?"":"->")+
                "]");
        }
    }

}
