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
    
    /* -------------------------------------------------------------- */
    private int _status;
    private String _reason;
    
    /* -------------------------------------------------------------- */
    /** Construct a response
     * @param out The output stream that the response will be written to.
     * @param request The HttpRequest that this response is to.
     */
    public HttpResponse()
    {
        _version = HttpMessage.__HTTP_1_0;
        _status = __200_OK;
        _header.put(HttpFields.__ContentType,"text/html");
        _header.put(HttpFields.__MimeVersion,"1.0");
        _header.put(HttpFields.__Server,"Jetty3_XXX");
        _header.putCurrentTime(HttpFields.__Date);
        _header.put(HttpFields.__Connection,HttpFields.__Close);
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
    
    /* -------------------------------------------------------------- */
    public synchronized  void writeHeader(OutputStream out) 
        throws IOException
    {
        if (_state!=__MSG_EDITABLE)
            throw new IllegalStateException("Not MSG_EDITABLE");
        
        _state=__MSG_BAD;
        synchronized(out)
        {
            out.write(_version.getBytes());
            out.write(' ');
            out.write((_status+" ").getBytes());
            out.write(getReason().getBytes());
            out.write(HttpFields.__CRLF_B);
            if (_cookies!=null)
                _header.write(out,(HttpFields)_cookies); //XXX
            else
                _header.write(out);
        }
        _state=__MSG_SENDING;
    }

    
    
      
    /* ------------------------------------------------------------- */
    /**
     * Sends an error response to the client using the specified status
     * code and detail message.
     * @param code the status code
     * @param reason the detail message
     * @exception IOException If an I/O error has occurred.
     */
    public void sendError(int code,String reason) 
        throws IOException
    {
        _header.put(HttpFields.__ContentType,"text/html");
        setStatus(code);
        setReason(reason);

// XXX         out=getOutputStream();
//          synchronized(out)
//          {
//              writeHeaders(out);
//              out.write("<HTML>\n<HEAD>\n<TITLE>Error ".getBytes());
//              out.write((code+"</TITLE>\n").getBytes());
//              out.write("<BODY>\n<H2>HTTP ERROR: ".getBytes());
//              out.write((code +" ").getbytes());
//              out.write((reason + "</H2>\n").getbytes());       
//              out.write("</BODY>\n</HTML>\n".getbytes());
//              out.flush();
//          }
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
        String msg = (String)__statusMsg.get(new Integer(code));
        if (msg==null)
            sendError(code,"UNKNOWN ERROR CODE");
        else
            sendError(code,msg);
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
        // XXX writeHeaders(out);
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







