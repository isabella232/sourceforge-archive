// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;


/* ------------------------------------------------------------ */
/** HTTP Request.
 * This class manages the headers, trailers and content streams
 * of a HTTP response. It can be used for receiving or generating
 * requests.
 *
 * @see HttpRequest
 * @version 1.0 Tue Oct  5 1999
 * @author Greg Wilkins (gregw)
 */
public class HttpResponse extends HttpMessage 
{ 
      public final static int
          __100_Continue = 100,
          __101_Switching_Protocols = 101,
          __200_OK = 200,
          __201_Created = 201,
          __202_Accepted = 202,
          __203_Non_Authoritative_Information = 203,
          __204_No_Content = 204,
          __205_Reset_Content = 205,
          __206_Partial_Content = 206,
          __300_Multiple_Choices = 300,
          __301_Moved_Permanently = 301,
          __302_Found = 302,
          __303_See_Other = 303,
          __304_Not_Modified = 304,
          __305_Use_Proxy = 305,
          __307_Temporary_Redirect = 307,
          __400_Bad_Request = 400,
          __401_Unauthorized = 401, 
          __402_Payment_Required = 402,
          __403_Forbidden = 403,
          __404_Not_Found = 404,
          __405_Method_Not_Allowed = 405,
          __406_Not_Acceptable = 406,
          __407_Proxy_Authentication_Required = 407,
          __408_Request_Timeout = 408,
          __409_Conflict = 409,
          __410_Gone = 410,
          __411_Length_Required = 411,
          __412_Precondition_Failed = 412,
          __413_Request_Entity_Too_Large = 413,
          __414_Request_URI_Too_Large = 414,
          __415_Unsupported_Media_Type = 415,
          __416_Requested_Range_Not_Satisfiable = 416,
          __417_Expectation_Failed = 417,
          __500_Internal_Server_Error = 500,
          __501_Not_Implemented = 501,
          __502_Bad_Gateway = 502,
          __503_Service_Unavailable = 503,
          __504_Gateway_Timeout = 504,
          __505_HTTP_Version_Not_Supported = 505;

          
    /* -------------------------------------------------------------- */
    public final static HashMap __statusMsg = new HashMap();
    static
    {
        // Build error code map using reflection
        try
        {
            Field[] fields = com.mortbay.HTTP.HttpResponse.class
                .getDeclaredFields();
            for (int f=fields.length; f-->0 ;)
            {
                int m = fields[f].getModifiers();
                String name=fields[f].getName();
                if (Modifier.isFinal(m) &&
                    Modifier.isStatic(m) &&
                    fields[f].getType().equals(Integer.TYPE) &&
                    name.startsWith("__") &&
                    Character.isDigit(name.charAt(2)))
                {
                    String message = name.substring(6);
                    message = message.replace('_',' ');
                    __statusMsg.put(fields[f].get(null),message);
                }
            }
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    public static final byte[] __Continue=
        "HTTP/1.1 100 Continue\015\012\015\012".getBytes();
    
    /* -------------------------------------------------------------- */
    private int _status= __200_OK;
    private String _reason;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public HttpResponse()
    {
        _version=__HTTP_1_0;
        _state=__MSG_EDITABLE;
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param connection 
     */
    public HttpResponse(HttpConnection connection)
    {
        super(connection);
        _version=__HTTP_1_0;
        _state=__MSG_EDITABLE;
    }
    

    /* ------------------------------------------------------------ */
    /** Get the HTTP Request.
     * Get the HTTP Request associated with this response.
     * @return associated request
     */
    public HttpRequest getRequest()
    {
        if (_connection==null)
            return null;
        return _connection.getRequest();
    }
    
    /* ------------------------------------------------------------ */
    /** XXX Not Implemented
     * @param in 
     * @exception IOException 
     */
    public synchronized void readHeader(ChunkableInputStream in)
        throws IOException
    {
        _state=__MSG_BAD;
        Code.notImplemented();
    }
    
    
    /* -------------------------------------------------------------- */
    public synchronized  void writeHeader(Writer writer) 
        throws IOException
    {
        if (_state!=__MSG_EDITABLE)
            throw new IllegalStateException(__state[_state]+
                                            " is not EDITABLE");

        String status=_status+" ";
        if (Code.verbose())
            Code.debug("writeHeaders: ",status);
        _state=__MSG_BAD;
        synchronized(writer)
        {
            writer.write(_version);
            writer.write(' ');
            writer.write(status);
            writer.write(getReason());
            writer.write(HttpFields.__CRLF);
            _header.write(writer);
        }
        _state=__MSG_SENDING;
    }
    
    /* -------------------------------------------------------------- */
    public int getStatus()
    {
        return _status;
    }
    
    /* -------------------------------------------------------------- */
    public void setStatus(int status)
    {
        _status=status;
    }
    
    /* -------------------------------------------------------------- */
    public String getReason()
    {
        if (_reason!=null)
            return _reason;
        return (String)__statusMsg.get(new Integer(_status));
    }
    
    /* -------------------------------------------------------------- */
    public void setReason(String reason)
    {
        _reason=reason;
    }
      
    /* ------------------------------------------------------------ */
    /* Which fields to set?
     * Specialized HttpMessage.setFields to consult request TE field
     * for a "trailer" token if state is SENDING.
     * @return Header or Trailer fields
     * @exception IllegalStateException Not editable or sending 1.1
     *                                  with trailers
     */
    protected HttpFields setFields()
        throws IllegalStateException
    {
        if (!_acceptTrailer &&
            _state==__MSG_SENDING &&
            _version.equals(__HTTP_1_1))
        {
            HttpRequest request=_connection.getRequest();
            if (request!=null)
                request.getAcceptableTransferCodings();
        }

        return super.setFields();
    }
    
    /* ------------------------------------------------------------- */
    /** Send Error Response.
     * Sends an error response to the client using the specified status
     * code and detail message.
     * @param exception 
     * @exception IOException If an I/O error has occurred.
     */
    public void sendError(HttpException exception) 
        throws IOException
    {
        _header.put(HttpFields.__ContentType,HttpFields.__TextHtml);
        String message=exception.getMessage();
        String reason=exception.getReason();

        int code=exception.getCode();
        setStatus(code);
        setReason(reason);

        if (code!=204 && code!=304 && code>=200)
        {
            ChunkableOutputStream out=getOutputStream();
            Writer writer= new OutputStreamWriter(out,"UTF8");
            synchronized(writer)
            {
                writer.write("<HTML>\n<HEAD>\n<TITLE>Error ");
                writer.write(exception.getCode()+" "+reason+"</TITLE>\n");
                writer.write("<BODY>\n<H2>HTTP ERROR: ");
                writer.write(exception.getCode() +" "+reason + "</H2>\n");
                if (message!=null)
                    writer.write(message);
                writer.write("\n</BODY>\n</HTML>\n");
                writer.flush();
            }
        }
        commit();
    }
    
    /* ------------------------------------------------------------- */
    /** Send Error Response.
     * Sends an error response to the client using the specified status
     * code and detail message.
     * @param code the status code
     * @param message the detail message
     * @exception IOException If an I/O error has occurred.
     */
    public void sendError(int code,String message) 
        throws IOException
    {
        _header.put(HttpFields.__ContentType,"text/html");
        setStatus(code);
        String reason = (String)__statusMsg.get(new Integer(code));
        setReason(reason);

        if (code!=204 && code!=304 && code>=200)
        {
            ChunkableOutputStream out=getOutputStream();
            Writer writer=new OutputStreamWriter(out,"UTF8");
            synchronized(writer)
            {
                writer.write("<HTML>\n<HEAD>\n<TITLE>Error ");
                writer.write(code+" "+reason+"</TITLE>\n");
                writer.write("<BODY>\n<H2>HTTP ERROR: ");
                writer.write(code +" "+reason + "</H2>");
                if (message!=null)
                    writer.write(message);
                writer.write("</BODY>\n</HTML>\n");
                writer.flush();
            }
        }
        commit();
    }
      
    /* ------------------------------------------------------------- */
    /**
     * Sends an error response to the client using the specified status
     * code and no default message.
     * @param code the status code
     * @exception IOException If an I/O error has occurred.
     */
    public void sendError(int code) 
        throws IOException
    {
        sendError(code,null);
    }
    
    /* ------------------------------------------------------------- */
    /**
     * Sends a redirect response to the client using the specified redirect
     * location URL.
     * @param location the redirect location URL
     * @exception IOException If an I/O error has occurred.
     */
    public void sendRedirect(String location)
        throws IOException
    {
        _header.put(HttpFields.__Location,location);
        setStatus(__307_Temporary_Redirect);
        commit();
    }


    /* -------------------------------------------------------------- */
    /** Add a Set-Cookie field.
     */
    public void addSetCookie(String name,
                             String value)
    {
        addSetCookie(name,value,null,"/",null,false);
    }
    
    /* -------------------------------------------------------------- */
    /** Add a Set-Cookie field. 
     * @param name The cookie name
     * @param value The cookie value which must not contain ';'.
     * @param domain The domain, which must be a subdomain of the server.
     *        If null is passed the default of this domain is used.
     * @param path The path the cookie applies to.
     *        If null is passed the default of "/" is used.
     * @param expires the Date the cookie expires on.
     *        If null is passed the cookie expires with the browser session.
     * @param secure if true the cookie is flagged secure.
     */
    public void addSetCookie(String name,
                             String value,
                             String domain,
                             String path,
                             Date expires,
                             boolean secure)
    {
        // Check arguments
        if (name==null || name.length()==0)
            throw new IllegalArgumentException("Bad cookie name");
        if (value==null || value.length()==0)
            throw new IllegalArgumentException("Bad cookie value");
        for(int i = 0; i < name.length(); i++)
        {
	    char c = name.charAt(i);
            if (Character.isWhitespace(c) || c==',' || c==';')
                throw new IllegalArgumentException("Bad cookie name:'"+name+"'");
	}
        if(name.equalsIgnoreCase ("Comment") ||
           name.equalsIgnoreCase ("Discard") ||
           name.equalsIgnoreCase ("Domain")  ||
           name.equalsIgnoreCase ("Expires") ||
           name.equalsIgnoreCase ("Max-Age") ||
           name.equalsIgnoreCase ("Path")    ||
           name.equalsIgnoreCase ("Secure")  ||
           name.equalsIgnoreCase ("Version"))
            throw new IllegalArgumentException("Bad cookie name");
        if (secure)
            Code.notImplemented();

        
        // Format value and params
        StringBuffer buf = new StringBuffer(128);
        String name_value_params=null;
        synchronized(buf)
        {
            buf.append(name);
            buf.append('=');
            buf.append(UrlEncoded.encodeString(value));
            if (path!=null && path.length()>0)
            {
                buf.append(";path=");
                buf.append(path);
            }
            if (domain!=null && domain.length()>0)
            {
                buf.append(";domain=");
                buf.append(domain);
            }
            if (expires!=null)
            {
                buf.append(";expires=");
                buf.append(HttpFields.__dateSend.format(expires));
            }
        
            name_value_params=buf.toString();
        }
        
        _header.put(HttpFields.__SetCookie,name_value_params);
    }
    
    /* ------------------------------------------------------------ */
    /** Destroy the header.
     * Help the garbage collector by null everything that we can.
     */
    public void destroy()
    {
        _reason=null;
        super.destroy();
    }
};







