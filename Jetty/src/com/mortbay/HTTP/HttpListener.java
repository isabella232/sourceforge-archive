// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import com.mortbay.Util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/* ------------------------------------------------------------ */
/** 
 *
 * @see
 * @version 1.0 Sat Oct  2 1999
 * @author Greg Wilkins (gregw)
 */
public class HttpListener extends ThreadPool
{
    
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
    public HttpListener(int minThreads,
                        int maxThreads,
                        int maxIdleTimeMs)
        throws IOException
    {
        super("Listener",minThreads,maxThreads,maxIdleTimeMs);
    }
    
    /* ------------------------------------------------------------ */
    /** Handle a connection to the server by trying to read a HttpRequest
     *  and finding the right type of handler for that request, which
     *  provides the HttpResponse
     */
    public void handleConnection(Socket connection)
    {
        try
        {
            while(true)
            {
                HttpRequest request = null;
                // XXX HttpResponse response = null;
                
                try
                {       
                    Code.debug("Waiting for request...");
                    request = new HttpRequest(/* XXX server,connection,address*/);

                    if (Code.debug())
                        Code.debug("Received HTTP request:",
                                   request.getMethod(),
                                   " "//,request.getRequestURI()
                                   );
            
//                      response=new HttpResponse(connection.getOutputStream(),
//                                                request);

//                      server.handle(request,response);

//                      response.complete();
                
                    if (HttpMessage.__HTTP_1_0.equals(request.getProtocol()))
                        break;
                    if (HttpFields.__Close
                        .equals(null/*XXX response.getHeader(HttpHeader.Connection)*/))
                    {
                        Code.debug("Closing persistent connection");
                        break;
                    }
                }
//                  catch (HeadException e)
//                  {
//                      Code.ignore(e);
//                  }
                catch (Exception e)
                {
                    Code.debug(e);
                    
//                      // If no respones - must have a request error
//                      if (response==null)
//                      {
//                          // try to write BAD_REQUEST
//                          response=new HttpResponse(connection.getOutputStream(),
//                                                    null);
//                          response.setHeader(HttpHeader.Connection,
//                                             HttpHeader.Close);
//                          response.sendError(HttpResponse.SC_BAD_REQUEST);
//                          break;
//                      }
                }
                finally
                {
                    if (request!=null)
                        request.destroy();
//                      if (response!=null)
//                          response.destroy();
                }
            }
        }
        catch (Exception e)
        {
            Code.debug("Request problem:",e);
        }
        finally
        {
            try{
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










