// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import java.io.*;
import java.net.*;

/* ---------------------------------------------------------------- */
/** HTTP Listener
 * <p> Instances of HttpListener handle a single receiving HTTP
 * connection. They make calls into HttpServer to handle the
 * requests that they receive.
 *
 * <p><h4>Notes</h4>
 * <p> On JDK1.0.2 it is not possible to specify the listening InetAddress
 *
 * @see class HttpServer
 * @version $Id$
 * @author Greg Wilkins
*/
public class HttpListener extends ThreadedServer
{
    public static boolean frameDebug = 
	System.getProperties().get("FRAMEDEBUG")!=null;

    /* ------------------------------------------------------------ */
    public static Class[] ConstructArgs =
    {
        com.mortbay.Util.InetAddrPort.class,
        com.mortbay.HTTP.HttpServer.class,
        Integer.TYPE,Integer.TYPE,Integer.TYPE
    };
    
    /* ------------------------------------------------------------ */
    InetAddrPort address=null;
    HttpServer server=null;

    /* ------------------------------------------------------------ */
    public InetAddrPort getAddress()
    {
        return address;
    }
    
    /* ------------------------------------------------------------ */
    /** Construct a HttpListener
     * @param address The InetAddress and port on which to listen
     *                If address.inetAddress==null,
     *                InetAddrPort.getLocalHost() is used and set in address.
     *                If address.port==0, 80 is used and set in address.
     * @param server  The HttpServer to pass requests to.
     */     
    public HttpListener(InetAddrPort address,
                        HttpServer server)
        throws IOException
    {
        this(address,server,0,0,0);
    }
    
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param address The InetAddress and port on which to listen
     *                If address.inetAddress==null,
     *                InetAddrPort.getLocalHost() is used and set in address.
     *                If address.port==0, 80 is used and set in address.
     * @param server  The HttpServer to pass requests to.
     * @param minThreads 
     * @param maxThreads 
     * @param maxIdleTimeMs 
     * @exception IOException 
     */
    public HttpListener(InetAddrPort address,
                        HttpServer server,
                        int minThreads,
                        int maxThreads,
                        int maxIdleTimeMs)
        throws IOException
    {
        super(address,minThreads,maxThreads,maxIdleTimeMs);
        if (address.getPort()==0)
        {
            address.setPort(80);
            super.setAddress(address.getInetAddress(),address.getPort());
        }
        
        this.address=address;
        this.server=server;
    }
    
    /* ------------------------------------------------------------ */
    public void start()
	throws IOException
    {
	super.start();
	Log.event(this.getClass().getName()+
		  " started on " +getAddress() );
    }
    
    /* ------------------------------------------------------------ */
    /** Allow the Listener a chance to customise the request
     * before the server does its stuff.
     * <br> This allows extra attributes to be set for SSL connections.
     */
    protected void customiseRequest(Socket connection,
				    HttpRequest request)
    {}

    /* ------------------------------------------------------------ */
    /** Handle a connection to the server by trying to read a HttpRequest
     *  and finding the right type of handler for that request, which
     *  provides the HttpResponse.
     */
    public void handleConnection(Socket connection)
    {
        try
        {
	    if(frameDebug) Log.event("CONNECT: "+connection);
            while(true)
            {
                HttpRequest request = null;
                HttpResponse response = null;
                
                try
                {       
                    Code.debug("Waiting for request...");
                    request = new HttpRequest(server,connection,address);

                    if (Code.debug())
                        Code.debug("Received HTTP request:",
                                   request.getMethod(),
                                   " ",
                                   request.getRequestURI());
		    if(frameDebug) Log.event("REQUEST: "+request.getMethod()+
					     " "+request.getRequestURI());
            
                    response=new HttpResponse(connection.getOutputStream(),
                                              request);

		    customiseRequest(connection, request);

                    server.handle(request,response);

                    response.complete();
		    if(frameDebug) Log.event("RESPONSE: "+response.getStatus());
                
                    String connection_header =response.getHeader(response.Connection);
                    if (connection_header!=null)
                        connection_header=
                            StringUtil.asciiToLowerCase(connection_header);

                    // Break request loop if 1.0 and not keep-alive
                    if (HttpHeader.HTTP_1_0.equals(request.getProtocol())&&
                        !"keep-alive".equals(connection_header))
                        break;

                    // Break request loop of close requested
                    if (HttpHeader.Close.equals(connection_header))
                    {
                        Code.debug("Closing persistent connection");
                        break;
                    }

		    // Read any remaining input.
		    if (request.getContentLength()>0)
		    {
			HttpInputStream in=request.getHttpInputStream();
			try{
			    // Skip/read remaining input
			    while(in.getContentLength()>0 &&
				  (in.skip(4096)>0 || in.read()>=0));
			    
			}
			catch(IOException e)
			{
			    Code.ignore(e);
			}
		    }
                }
                catch (HeadException e)
                {
                    Code.ignore(e);
                }
                catch (Exception e)
                {
		    if(frameDebug) Code.warning(e);
		    else Code.debug(e);
                    
                    // If no respones - must have a request error
                    if (response==null)
                    {
                        // try to write BAD_REQUEST
                        response=new HttpResponse(connection.getOutputStream(),
                                                  null);
                        response.setHeader(HttpHeader.Connection,
                                           HttpHeader.Close);
                        response.sendError(HttpResponse.SC_BAD_REQUEST);
                        break;
                    }
                }
                finally
                {
                    if (request!=null)
                        request.destroy();
                    if (response!=null)
                        response.destroy();
                }

		if(frameDebug) Log.event("KEEPALIVE: "+connection);
            }
        }
        catch (Exception e)
        {
            Code.debug("Request problem:",e);
        }
        finally
        {
            try{
		if(frameDebug) Log.event("CLOSE: "+connection);
                connection.close();
            }
            catch (IOException e){
                Code.ignore(e);
            }
            catch (Exception e){
                Code.warning("Request problem:",e);
            }
        }
    }
};



