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
    public final static String __HTTP_1_X ="HTTP/1.X";

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
    /** 
     */
    public void writeNotify()
    {}

    
    /* -------------------------------------------------------------- */
    /** Character Encoding.
     * Checks the Content-Type header for a charset parameter and return its
     * value if found or ISO-8859-1 otherwise.
     * @return Character Encoding.
     */
    public String getCharacterEncoding ()
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














