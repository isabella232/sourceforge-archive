// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.sun.java.util.collections.*;
import com.mortbay.Util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;


// ====================================================================
public class HttpMessage
{
    /* ------------------------------------------------------------ */
    /** Message States
     */
    public final static int
        __MSG_EDITABLE=0,  // Created locally, all set methods enabled
        __MSG_BAD=1,       // Bad message/
        __MSG_RECEIVED=2,  // Received from connection.
        __MSG_SENDING=3,   // Headers sent.
        __MSG_SENT=4;      // Entity and footers sent.

    /* ------------------------------------------------------------ */
    public final static String __HTTP_0_9 ="HTTP/0.9";
    public final static String __HTTP_1_0 ="HTTP/1.0";
    public final static String __HTTP_1_1 ="HTTP/1.1";
    public final static String __HTTP_1_X ="HTTP/1.";

    protected int _state;
    protected String _version;
    protected HttpFields _header;
    protected Map _cookies;
    protected HttpFields _footer;
    protected Connection _connection;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    protected HttpMessage()
    {
        _header=new HttpFields();
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    protected HttpMessage(Connection connection)
    {
        _header=new HttpFields();
        _connection=connection;
    }
    
    /* ------------------------------------------------------------ */
    /** Destroy the header.
     * Help the garbage collector by nulling everything that we can.
     */
    public void destroy()
    {
        _header.destroy();
        _footer.destroy();
        _header=null;
        _footer=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the message state.
     * <PRE>
     * __MSG_EDITABLE = 0 - Created locally, all set methods enabled
     * __MSG_BAD      = 1 - Bad message or send failure.
     * __MSG_RECEIVED = 2 - Received from connection.
     * __MSG_SENDING  = 3 - Headers sent.
     * __MSG_SENT     = 4 - Entity and footers sent.
     * </PRE>
     * @return the state.
     */
    public synchronized int getState()
    {
        return _state;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the protocol version
     * @return return the version.
     */
    public String getVersion()
    {
        return _version;
    }


    /* ------------------------------------------------------------ */
    /** Get a message field.
     * Get a field from a message header. If no header field is found,
     * footer fields are searched.
     * @param name The field name
     * @return field value or null
     */
    public String getField(String name)
    {
        String field = _header.get(name);
        if (field==null && _footer!=null)
            field=_footer.get(name);
        return field;
    }
    
    /* ------------------------------------------------------------ */
    /** Get a multi valued message field.
     * Get a field from a message header. If no header field is found,
     * footer fields are searched.
     * @param name The field name
     * @return field value or null
     */
    public List getFieldValues(String name)
    {
        List field = _header.getValues(name);
        if (field==null && _footer!=null)
            field=_footer.getValues(name);
        return field;
    }

    /* ------------------------------------------------------------ */
    /* Which fields to set?
     * If the message is editable, then a header fields are returned.
     * Otherwise if the message is sending a HTTP/1.1 message,
     * then a footer field is returned.
     * @return Header or Footer fields
     * @exception IllegalStateException Not editable or sending 1.1
     */
    private HttpFields setFields()
    {
        if (_state==__MSG_EDITABLE)
            return _header;
        else if (_state==__MSG_SENDING && _version.equals(__HTTP_1_1))
        {
            if (_footer==null)
                _footer=new HttpFields();
            return _footer;
        }
        
        throw new IllegalStateException("Can't set fields in "+_state);
    }
    

    /* ------------------------------------------------------------ */
    /** Set a field value.
     * If the message is editable, then a header field is set. Otherwise
     * if the message is sending and a HTTP/1.1 version, then a footer
     * field is set.
     * @param name Name of field 
     * @param value New value of field
     * @return Old value of field
     * @exception IllegalStateException Not editable or sending 1.1
     */
    public String setField(String name, String value)
        throws IllegalStateException
    {
        HttpFields fields=setFields();
        return (String) fields.put(name,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Set a multi-value field value.
     * If the message is editable, then a header field is set. Otherwise
     * if the meesage is sending and a HTTP/1.1 version, then a footer
     * field is set.
     * @param name Name of field 
     * @param value New values of field
     * @return Old values of field
     * @exception IllegalStateException Not editable or sending 1.1
     */
    public List setField(String name, List value)
        throws IllegalStateException
    {
        HttpFields fields=setFields();
        List old = fields.getValues(name);
        fields.put(name,value);
        return old;
    }
    
    /* ------------------------------------------------------------ */
    /** Add to a multi-value field value.
     * If the message is editable, then a header field is set. Otherwise
     * if the meesage is sending and a HTTP/1.1 version, then a footer
     * field is set.
     * @param name Name of field 
     * @param value New value to add to the field
     * @exception IllegalStateException Not editable or sending 1.1
     */
    public void addField(String name, String value)
        throws IllegalStateException
    {
        HttpFields fields=setFields();
        fields.add(name,value);
    }
    
    /* -------------------------------------------------------------- */
    /** Get a field as an integer value.
     * Look in header and footer fields.
     * Returns the value of an integer field, or -1 if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     */
    public int getIntField(String name)
    {
        int v=_header.getIntField(name);
        if (v<0 && _footer!=null)
            v=_footer.getIntField(name);
        return v;
    }
    
    /* -------------------------------------------------------------- */
    /** Sets the value of an integer field.
     * Header or Footer fields are set depending on message state.
     * @param name the field name
     * @param value the field integer value
     * @exception IllegalStateException Not editable or sending 1.1
     */
    public void setIntField(String name, int value)
        throws IllegalStateException
    {
        setFields().put(name, Integer.toString(value));
    }
    
    /* -------------------------------------------------------------- */
    /** Get a header as a date value.
     * Look in header and footer fields.
     * Returns the value of a date field, or -1 if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     */
    public long getDateField(String name)
    {
        long d=_header.getDateField(name);
        if (d<0 && _footer!=null)
            d=_footer.getDateField(name);
        return d;
    }
    

    /* -------------------------------------------------------------- */
    /** Sets the value of a date field.
     * Header or Footer fields are set depending on message state.
     * @param name the field name
     * @param value the field date value
     * @exception IllegalStateException Not editable or sending 1.1
     */
    public void setDateField(String name, Date date)
    {
        setFields().put(name,date);
    }
    
    /* -------------------------------------------------------------- */
    /** Sets the value of a date field.
     * Header or Footer fields are set depending on message state.
     * @param name the field name
     * @param value the field date value
     * @exception IllegalStateException Not editable or sending 1.1
     */
    public void setDateField(String name, long date)
    {
        setFields().putDateField(name,date);
    }
    
    /* -------------------------------------------------------------- */
    /** Sets the value of a date field to the current time.
     * Header or Footer fields are set depending on message state.
     * Uses efficient DateCache mechanism.
     * @param name the field name
     * @param value the field date value
     * @exception IllegalStateException Not editable or sending 1.1
     */
    public void setCurrentTime(String name)
    {
        setFields().putCurrentTime(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Set the request version 
     * @param version the  HTTP version string (eg HTTP/1.1)
     * @exception IllegalStateException message is not EDITABLE
     */
    public void setVersion(String version)
    {
        if (_state!=__MSG_EDITABLE)
            throw new IllegalStateException("Not EDITABLE");
        
        version=version.toUpperCase();
        if (version.equals(__HTTP_1_1))
            _version=__HTTP_1_1;
        else if (version.equals(__HTTP_1_0))
            _version=__HTTP_1_0;
        else
            throw new IllegalArgumentException("Unknown version");
    }


    /* ------------------------------------------------------------ */
    /** XXX
     * @param contentEncoding 
     * @exception HttpException 
     */
    public void checkEncodings(boolean contentEncoding)
        throws HttpException
    {
    }
    
    /* ------------------------------------------------------------ */
    /** XXX
     * @param contentEncoding 
     * @exception HttpException 
     */
    public void enableEncodings(boolean contentEncoding)
        throws HttpException
    {
    }

    
    /* -------------------------------------------------------------- */
    /** Character Encoding.
     * Checks the Content-Type header for a charset parameter and return its
     * value if found or ISO-8859-1 otherwise.
     * @return Character Encoding.
     */
    public String getCharacterEncoding()
    {
        String encoding = _header.get(HttpFields.__ContentType);
        if (encoding==null || encoding.length()==0)
            return "ISO-8859-1";
        
        int i=encoding.indexOf(';');
        if (i<0)
            return "ISO-8859-1";
        
        i=encoding.indexOf("charset=",i);
        if (i<0 || i+8>=encoding.length())
            return "ISO-8859-1";
            
        encoding=encoding.substring(i+8);
        i=encoding.indexOf(' ');
        if (i>0)
            encoding=encoding.substring(0,i);
            
        return encoding;
    }
    
    
}














