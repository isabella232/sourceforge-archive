// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.*;
import java.io.IOException;

abstract public class Connection
{
    private Object _server;
    private ChunkableInputStream _inputStream;
    private ChunkableOutputStream _outputStream;
    
    public abstract ChunkableInputStream getInputStream();
    public abstract ChunkableOutputStream getOutputStream();
    public abstract String getDefaultProtocol();
    public abstract String getHost();
    public abstract int getPort();
    public abstract void close() throws IOException;

    /* ------------------------------------------------------------ */
    /** 
     */
    public void handle()
    {
        try
        {
            while(true)
            {
                HttpRequest request = new HttpRequest(this);
                HttpResponse response = new HttpResponse(this);
        
                try
                {       
                    Code.debug("Wait for request header...");
                    request.readHeader(getInputStream());
                    if (request.getState()!=HttpMessage.__MSG_RECEIVED)
                        throw new HttpException(response.__400_Bad_Request);
                    if (Code.debug())
                        Code.debug("HTTPRequest:",request.getRequestLine());
                    

                    // Pick response version
                    String version=request.getVersion();
                    if (HttpMessage.__HTTP_0_9.equals(version))
                    {
                        Code.warning("Handling HTTP/0.9 as HTTP/1.0");
                        version=HttpMessage.__HTTP_1_0;
                    }
                    else if (!HttpMessage.__HTTP_1_1.equals(version) &&
                             !HttpMessage.__HTTP_1_0.equals(version) &&
                             version.startsWith(HttpMessage.__HTTP_1_X))
                    {
                        Code.debug("Respond to HTTP/1.X with HTTP/1.1");
                        version=HttpMessage.__HTTP_1_1;
                    }
                    response.setVersion(version);
                    response.setField(HttpFields.__Server,"Jetty3_XXX");
                    response.setField(HttpFields.__MimeVersion,"1.0");
                    response.setCurrentTime(HttpFields.__Date);

                    
                    // Handle version
                    if (HttpMessage.__HTTP_1_1.equals(version))
                    {
                        // HTTP/1.1 Handling

                        // Check Host Field exists
                        String host=request.getField(HttpFields.__Host);
                        if (host==null || host.length()==0)
                            throw new HttpException(response.__400_Bad_Request);
                        if (!host.equals(request.getHost()))
                        {
                            Code.warning("XXX Host field does not match URI");
                            Code.debug(request);
                        }
                        
                        // Check transfer encodings
                        request.checkEncodings(false);
                        
                        // Handle Continue Expectations
                        String expect=request.getField(HttpFields.__Expect);
                        if (expect!=null && expect.length()>0)
                        {
                            if (StringUtil.asciiToLowerCase(expect)
                                .equals(HttpFields.__ExpectContinue))
                            {
                                // Send continue if no body available yet.
                                if (_inputStream.available()<=0)
                                {
                                    _outputStream.write(response.__Continue);
                                    _outputStream.flush();
                                }
                            }
                            else
                                throw new HttpException(response.__417_Expectation_Failed);
                        }
                    
                        // Enable transfer encodings
                        request.enableEncodings(false);
                    }
                    else
                    {
                        // HTTP 1.0 Handling

                        // Set content length
                        int content_length=
                            request.getIntField(HttpFields.__ContentLength);
                        if (content_length>=0)
                            _inputStream.setContentLength(content_length);
                    }
                    

                    // server.handle(request,response);
                    // response.complete();

                
                    if (HttpMessage.__HTTP_1_0.equals(request.getProtocol()))
                        break;
                    if (HttpFields.__Close
                        .equals(null/*XXX response.getHeader(HttpHeader.Connection)*/))
                    {
                        Code.debug("Closing persistent connection");
                        break;
                    }
                }
                catch (HttpException e)
                {
                    Code.debug(e);
                    
                    //response.reset();
                    //response.put(HttpFields.__Connection,
                    //             HttpFields.__Close);
                    response.sendError(e.getCode());
                    break;
                }
                catch (Exception e)
                {
                    Code.debug(e);
                    //response.reset();
                    //response.put(HttpFields.__Connection,
                    //             HttpFields.__Close);
                    response.sendError(HttpResponse.__400_Bad_Request);
                    break;
                }
                finally
                {
                    if (request!=null)
                        request.destroy();
                    if (response!=null)
                        response.destroy();
                }
            }
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
        finally
        {
            try{
                close();
            }
            catch (IOException e)
            {
                Code.ignore(e);
            }
            catch (Exception e)
            {
                Code.warning(e);
            }
        }
    }
};
