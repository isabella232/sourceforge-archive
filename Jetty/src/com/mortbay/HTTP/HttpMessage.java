// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;

import com.mortbay.Util.Code;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;


/* ------------------------------------------------------------ */
/** HTTP Message base.
 * This class forms the basis of HTTP requests and replies. It provides
 * header fields, content and optional trailer fields, while managing the
 * state of the message.
 *
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
abstract public class HttpMessage
{
    /* ------------------------------------------------------------ */
    /** Message States
     */
    public final static int
        __MSG_EDITABLE=0,  // Created locally, all set methods enabled
        __MSG_BAD=1,       // Bad message/
        __MSG_RECEIVED=2,  // Received from connection.
        __MSG_SENDING=3,   // Headers sent.
        __MSG_SENT=4;      // Entity and trailers sent.

    public final static String[] __state =
    {
        "EDITABLE",
        "BAD",
        "RECEIVED",
        "SENDING",
        "SENT"
    };
    
    /* ------------------------------------------------------------ */
    public final static String __HTTP_0_9 ="HTTP/0.9";
    public final static String __HTTP_1_0 ="HTTP/1.0";
    public final static String __HTTP_1_1 ="HTTP/1.1";
    public final static String __HTTP_1_X ="HTTP/1.";

    protected int _state;
    protected String _version;
    protected HttpFields _header;
    protected HttpFields _trailer;
    protected boolean _acceptTrailer;
    protected HttpConnection _connection;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    protected HttpMessage()
    {
        _header=new HttpFields();
        if (Code.verbose(999))
            Code.debug("New");
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    protected HttpMessage(HttpConnection connection)
    {
        _header=new HttpFields();
        _connection=connection;
        if (Code.verbose(999))
            Code.debug("New ",connection);
    }

    /* ------------------------------------------------------------ */
    /** 
     */
    protected void reset()
    {
        _state=__MSG_EDITABLE;
        _header=new HttpFields();
        _trailer=null;

        // XXX - also need to cancel any encodings added to output stream!
        
    }
    
    /* ------------------------------------------------------------ */
    /** XXX
     * @return 
     */
    public HttpConnection getConnection()
    {
        return _connection;
    }

    /* ------------------------------------------------------------ */
    /** XXX
     * @return 
     */
    public ChunkableInputStream getInputStream()
    {
        if (_connection==null)
            return null;
        return _connection.getInputStream();
    }
    
    /* ------------------------------------------------------------ */
    /** XXX
     * @return 
     */
    public ChunkableOutputStream getOutputStream()
    {
        if (_connection==null)
            return null;
        return _connection.getOutputStream();
    }
    

    
    /* ------------------------------------------------------------ */
    /** Get the message state.
     * <PRE>
     * __MSG_EDITABLE = 0 - Created locally, all set methods enabled
     * __MSG_BAD      = 1 - Bad message or send failure.
     * __MSG_RECEIVED = 2 - Received from connection.
     * __MSG_SENDING  = 3 - Headers sent.
     * __MSG_SENT     = 4 - Entity and trailers sent.
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
    /** Get field names.
     * @return 
     */
    public Collection getFieldNames()
    {
        if (_header!=null && _trailer==null)
            return _header.getFieldNames();
        HashSet fns=new HashSet(_header.getFieldNames());
        fns.addAll(_trailer.getFieldNames());
        return fns;
    }

    /* ------------------------------------------------------------ */
    /** Does the header or trailer contain a field?
     * @param name Name of the field
     * @return True if contained in header or trailer.
     */
    public boolean containsField(String name)
    {
        boolean contains = _header.containsKey(name);
        if (!contains && _trailer!=null)
            contains = _trailer.containsKey(name);
        return contains;
    }
    
    /* ------------------------------------------------------------ */
    /** Get a message field.
     * Get a field from a message header. If no header field is found,
     * trailer fields are searched.
     * @param name The field name
     * @return field value or null
     */
    public String getField(String name)
    {
        String field = _header.get(name);
        if (field==null && _trailer!=null)
            field=_trailer.get(name);
        return field;
    }
    
    /* ------------------------------------------------------------ */
    /** Get a multi valued message field.
     * Get a field from a message header. If no header field is found,
     * trailer fields are searched.
     * @param name The field name
     * @return field value or null
     */
    public List getFieldValues(String name)
    {
        List field = _header.getValues(name);
        if (field==null && _trailer!=null)
            field=_trailer.getValues(name);
        return field;
    }

    /* ------------------------------------------------------------ */
    /* Which fields to set?
     * If the message is editable, then a header fields are returned.
     * Otherwise if the message is sending a HTTP/1.1 message,
     * then a trailer field is returned if it has been set.
     * @return Header or Trailer fields
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    protected HttpFields setFields()
        throws IllegalStateException
    {
        if (_state==__MSG_EDITABLE)
            return _header;

        if (_acceptTrailer &&
                 _state==__MSG_SENDING &&
                 _version.equals(__HTTP_1_1))
        {
            if (_trailer==null)
                _trailer=new HttpFields();
            return _trailer;
        }
        
        throw new IllegalStateException("Can't set fields in "+
                                        __state[_state]+
                                        " for "+_version);
    }
    

    /* ------------------------------------------------------------ */
    /** Set a field value.
     * If the message is editable, then a header field is set. Otherwise
     * if the message is sending and a HTTP/1.1 version, then a trailer
     * field is set.
     * @param name Name of field 
     * @param value New value of field
     * @return Old value of field
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
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
     * if the meesage is sending and a HTTP/1.1 version, then a trailer
     * field is set.
     * @param name Name of field 
     * @param value New values of field
     * @return Old values of field
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
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
     * if the meesage is sending and a HTTP/1.1 version, then a trailer
     * field is set.
     * @param name Name of field 
     * @param value New value to add to the field
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    public void addField(String name, String value)
        throws IllegalStateException
    {
        HttpFields fields=setFields();
        fields.add(name,value);
    }
    
    /* -------------------------------------------------------------- */
    /** Get a field as an integer value.
     * Look in header and trailer fields.
     * Returns the value of an integer field, or -1 if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     */
    public int getIntField(String name)
    {
        int v=_header.getIntField(name);
        if (v<0 && _trailer!=null)
            v=_trailer.getIntField(name);
        return v;
    }
    
    /* -------------------------------------------------------------- */
    /** Sets the value of an integer field.
     * Header or Trailer fields are set depending on message state.
     * @param name the field name
     * @param value the field integer value
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    public void setIntField(String name, int value)
        throws IllegalStateException
    {
        setFields().put(name, Integer.toString(value));
    }
    
    /* -------------------------------------------------------------- */
    /** Adds the value of an integer field.
     * Header or Trailer fields are set depending on message state.
     * @param name the field name
     * @param value the field integer value
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    public void addIntField(String name, int value)
        throws IllegalStateException
    {
        setFields().add(name, Integer.toString(value));
    }
    
    /* -------------------------------------------------------------- */
    /** Get a header as a date value.
     * Look in header and trailer fields.
     * Returns the value of a date field, or -1 if not found.
     * The case of the field name is ignored.
     * @param name the case-insensitive field name
     */
    public long getDateField(String name)
    {
        long d=_header.getDateField(name);
        if (d<0 && _trailer!=null)
            d=_trailer.getDateField(name);
        return d;
    }
    

    /* -------------------------------------------------------------- */
    /** Sets the value of a date field.
     * Header or Trailer fields are set depending on message state.
     * @param name the field name
     * @param value the field date value
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    public void setDateField(String name, Date date)
    {
        setFields().put(name,date);
    }
    
    /* -------------------------------------------------------------- */
    /** Adds the value of a date field.
     * Header or Trailer fields are set depending on message state.
     * @param name the field name
     * @param value the field date value
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    public void addDateField(String name, Date date)
    {
        setFields().addDateField(name,date);
    }
    
    /* -------------------------------------------------------------- */
    /** Sets the value of a date field.
     * Header or Trailer fields are set depending on message state.
     * @param name the field name
     * @param value the field date value
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    public void setDateField(String name, long date)
    {
        setFields().putDateField(name,date);
    }
    
    /* -------------------------------------------------------------- */
    /** Sets the value of a date field to the current time.
     * Header or Trailer fields are set depending on message state.
     * Uses efficient DateCache mechanism.
     * @param name the field name
     * @param value the field date value
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    public void setCurrentTime(String name)
    {
        setFields().putCurrentTime(name);
    }

    /* ------------------------------------------------------------ */
    /** Remove a field.
     * If the message is editable, then a header field is removed. Otherwise
     * if the message is sending and a HTTP/1.1 version, then a trailer
     * field is removed.
     * @param name Name of field 
     * @return Old value of field
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    public String removeField(String name)
        throws IllegalStateException
    {
        HttpFields fields=setFields();
        return (String) fields.remove(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Set the request version 
     * @param version the  HTTP version string (eg HTTP/1.1)
     * @exception IllegalStateException message is not EDITABLE
     */
    public void setVersion(String version)
    {
        if (_state!=__MSG_EDITABLE)
            throw new IllegalStateException(__state[_state]+
                                            "is not EDITABLE");
        version=version.toUpperCase();
        if (version.equals(__HTTP_1_1))
            _version=__HTTP_1_1;
        else if (version.equals(__HTTP_1_0))
            _version=__HTTP_1_0;
        else
            throw new IllegalArgumentException("Unknown version");
    }
    
    /* ------------------------------------------------------------ */
    /** Get the HTTP header fields.
     * @return Header or null
     */
    public HttpFields getHeader()
    {
        return _header;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the HTTP chunked trailer (also called trailer).
     * @return Trailer or null
     */
    public HttpFields getTrailer()
    {
        if (_acceptTrailer && _trailer==null)
            _trailer=new HttpFields();
        return _trailer;
    }
    
    /* ------------------------------------------------------------ */
    /** Set if trailers are accepted.
     * @param acceptTrailer  If true, setField() may use trailers.
     */
    public void setAcceptTrailer(boolean acceptTrailer)
    {
        _acceptTrailer=acceptTrailer;
    }
    
    /* ------------------------------------------------------------ */
    /** Set if trailers are accepted.
     * @param acceptTrailer  If true, setField() may use trailers.
     */
    public boolean acceptTrailer()
    {
        return _acceptTrailer;
    }
    
    /* -------------------------------------------------------------- */
    /** Character Encoding.
     * Checks the Content-Type header for a charset parameter and return its
     * value if found or ISO-8859-1 otherwise.
     * @return Character Encoding.
     */
    public String getCharacterEncoding()
    {
        String encoding="ISO-8859-1";
        String s = _header.get(HttpFields.__ContentType);
        if (s!=null)
        {
            int i0=s.indexOf(';');
            if (i0>=0)
            {
                int i1 = s.indexOf("charset=",i0);
                if (i1>=0)
                {
                    i1+=8;
                    int i2 = s.indexOf(' ',i1);
                    encoding = (0 < i2) ? s.substring(i1,i2) : s.substring(i1);
                }
            }
        }
        return encoding;
    }
    
    /* ------------------------------------------------------------ */
    /** Destroy the header.
     * Help the garbage collector by nulling everything that we can.
     */
    public void destroy()
    {
        if (Code.verbose(999))
            Code.debug("Destroy");
        if (_header!=null)
            _header.destroy();
        if (_trailer!=null)
            _trailer.destroy();
        _header=null;
        _trailer=null;
    }

    /* ------------------------------------------------------------ */
    /** Write the message header
     * @param writer
     */
    abstract void writeHeader(Writer writer)
        throws IOException;
    
    /* ------------------------------------------------------------ */
    /** Convert to String.
     * The message header is converted to a String.
     * @return String
     */
    public synchronized String toString()
    {
        StringWriter writer = new StringWriter();

        int save_state=_state;
        try{
            _state=__MSG_EDITABLE;
            writeHeader(writer);
        }
        catch(IOException e)
        {
            Code.warning(e);
        }
        finally
        {
            _state=save_state;
        }
        return writer.toString();
    }

    /* ------------------------------------------------------------ */
    /** Commit the message.
     * Take whatever actions possible to move the message to the SENDING state.
     */
    public synchronized void commit()
        throws IOException, IllegalStateException
    {
        if (Code.verbose(99))
            Code.debug("Commit from "+__state[_state]);
        
        int lastState=_state;
        _state=__MSG_SENDING;
        
        ChunkableOutputStream out = getOutputStream();
        if (out==null)
            throw new IllegalStateException("No output stream");
        
        switch(lastState)
        {
          case __MSG_EDITABLE:
              Writer writer = out.getRawWriter();
              writeHeader(writer);
              out.writeRawWriter();
              out.flush();
              break;
          case __MSG_BAD:
              throw new IllegalStateException("BAD");
          case __MSG_RECEIVED:
              throw new IllegalStateException("RECEIVED");
          case __MSG_SENDING:
              break;
          case __MSG_SENT:
              break;
        }
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @return true if the message has been modified. 
     */
    public boolean isDirty()
    {
        ChunkableOutputStream out=getOutputStream();
        
        return _state!=__MSG_EDITABLE
            || ( out!=null &&
                 (out.isWritten() || out.isCommitted()));
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public boolean isCommitted()
    {
        ChunkableOutputStream out=getOutputStream();
        return out!=null && out.isCommitted() ||
            _state==__MSG_SENT || _state==__MSG_SENDING;
    }

    /* ------------------------------------------------------------ */
    public synchronized void complete()
        throws IOException
    {
        if (!isCommitted())
            commit();
        
        ChunkableOutputStream out=getOutputStream();
        if (out!=null)
        {
            if (_trailer!=null && _trailer.size()>0)
            {
                if (out!=null && out.isChunking())
                    out.setTrailer(_trailer);
            }
            out.flush();
        }
        _state=__MSG_SENT;
    }
}








