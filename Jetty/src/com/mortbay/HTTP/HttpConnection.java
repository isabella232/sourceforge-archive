// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.io.*;


/* ------------------------------------------------------------ */
/** A HTTP Connection
 * This abstract class provides the generic HTTP handling for
 * a connection to a HTTP server. A concrete instance of HttpConnection
 * is normally created by a concrete HttpListener and then given control
 * in order to run the protocol handling before and after passing
 * a request to the HttpServer of the HttpListener.
 *
 * @see HttpListener
 * @see HttpServer
 * @version 1.0 Wed Oct  6 1999
 * @author Greg Wilkins (gregw)
 */
abstract public class HttpConnection
    implements OutputObserver
{
    private HttpListener _listener;
    private ChunkableInputStream _inputStream;
    private ChunkableOutputStream _outputStream;
    private boolean _persistent;
    private boolean _close;
    private ByteArrayOutputStream _headerBuffer=new ByteArrayOutputStream(512);
    private String _version;
    private boolean _http1_1;
    private boolean _http1_0;
    private HttpRequest _request;
    private HttpResponse _response;
    private Thread _handlingThread;
    
    /* ------------------------------------------------------------ */
    /** Constructor. XXX
     * @param listener 
     * @param in 
     * @param out 
     */
    protected HttpConnection(HttpListener listener,
                             InputStream in,
                             OutputStream out)
    {
        _listener=listener;
        _inputStream=new ChunkableInputStream(in);
        _outputStream=new ChunkableOutputStream(out);
        _outputStream.addObserver(this);
    }

    /* ------------------------------------------------------------ */
    /** Get the connections InputStream.
     * @return the connections InputStream
     */
    public ChunkableInputStream getInputStream()
    {
        return _inputStream;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the connections OutputStream.
     * @return the connections OutputStream
     */
    public ChunkableOutputStream getOutputStream()
    {
        return _outputStream;
    }
    
    /* ------------------------------------------------------------ */
    /** Close the connection.
     * This method calls close on the input and output streams and
     * interrupts ny thread in the handle method.
     * may be specialized to close sockets etc.
     * @exception IOException 
     */
    public void close()
        throws IOException
    {
        _inputStream.close();
        _outputStream.close();
        if (_handlingThread!=null)
            _handlingThread.interrupt();
    }
    
    /* ------------------------------------------------------------ */
    /** Get the connections listener. 
     * @return HttpListener that created this Connection.
     */
    public HttpListener getListener()
    {
        return _listener;
    }

    /* ------------------------------------------------------------ */
    /** Get the listeners HttpServer .
     * Conveniance method equivalent to getListener().getServer().
     * @return HttpServer.
     */
    public HttpServer getServer()
    {
        return _listener.getServer();
    }

    /* ------------------------------------------------------------ */
    /** Get the listeners Default protocol.
     * Conveniance method equivalent to getListener().getDefaultProtocol().
     * @return HttpServer.
     */
    public String getDefaultProtocol()
    {
        return _listener.getDefaultProtocol();
    }
    
    /* ------------------------------------------------------------ */
    /** Get the listeners HttpServer .
     * Conveniance method equivalent to getListener().getHost().
     * @return HttpServer.
     */
    public String getHost()
    {
        return _listener.getHost();
    }
    
    /* ------------------------------------------------------------ */
    /** Get the listeners Port .
     * Conveniance method equivalent to getListener().getPort().
     * @return HttpServer.
     */
    public int getPort()
    {
        return _listener.getPort();
    }
    
    /* ------------------------------------------------------------ */
    /** Handle the connection.
     * Once the connection has been created, this method is called
     * to handle one or more requests that may be received on the
     * connection.  The method only returns once all requests have been
     * handled, an error has been returned to the requestor or the
     * connection has been closed.
     */
    public synchronized void handle()
    {
        _handlingThread=Thread.currentThread();
        try
        {
            while(handleRequest());
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
        finally
        {
            _handlingThread=null;
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
    
    /* ------------------------------------------------------------ */
    /* Handle a single request off a connection.
     * @return True if the connection can handle more requests.
     * @exception IOException Problem with the connection.
     */
    private boolean handleRequest()
        throws IOException
    {
        _request = new HttpRequest(this);
        _response = new HttpResponse(this);
        
        // Assume the connection is not persistent, unless told otherwise.
        _persistent=false;
        _close=false;
        _http1_0=true;
        _http1_1=false;
        
        try
        {       
            Code.debug("Wait for request header...");
            _request.readHeader(getInputStream());
            if (_request.getState()!=HttpMessage.__MSG_RECEIVED)
                throw new HttpException(_response.__400_Bad_Request);
            if (Code.debug())
                Code.debug("Request:",_request.getRequestLine());        

            // Pick response version
            _version=_request.getVersion();
            if (HttpMessage.__HTTP_0_9.equals(_version))
            {
                Code.warning(HttpMessage.__HTTP_0_9);
                throw new HttpException(_response.__505_HTTP_Version_Not_Supported);
            }
            else if (HttpMessage.__HTTP_1_0.equals(_version))
            {
                _http1_0=true;
                _http1_1=false;
            }
            else if (HttpMessage.__HTTP_1_1.equals(_version))
            {
                _http1_0=false;
                _http1_1=true;
            }
            else if (_version.startsWith(HttpMessage.__HTTP_1_X))
            {
                Code.debug("Respond to HTTP/1.X with HTTP/1.1");
                _version=HttpMessage.__HTTP_1_1;
                _http1_0=false;
                _http1_1=true;
            }
            _response.setVersion(_version);
            _response.setField(HttpFields.__Server,"Jetty3_XXX");
            _response.setField(HttpFields.__MimeVersion,"1.0");
            _response.setCurrentTime(HttpFields.__Date);

            
            // Handle Connection header field
            List connectionValues = _request.getFieldValues(HttpFields.__Connection);
            if (connectionValues!=null)
            {
                Iterator iter = connectionValues.iterator();
                while (iter.hasNext())
                {
                    String token=iter.next().toString();
                
                    // handle close token
                    if (token.equals(HttpFields.__Close))
                    {
                        _close=true;
                        _response.setField(HttpFields.__Connection,
                                           HttpFields.__Close);
                    }
                    
                    // Remove headers for HTTP/1.0 requests
                    if (HttpMessage.__HTTP_1_0.equals(_version))
                        _request.removeField(token);
                }
            }
            
            // Handle version specifics
            if (_http1_1)
                verifyHTTP_1_1();
            else if (_http1_0)
                verifyHTTP_1_0();
            else
                throw new HttpException(_response.__505_HTTP_Version_Not_Supported);
            if (Code.debug())
                Code.debug("IN is "+
                           (_inputStream.isChunking()?"chunked":"not chunked")+
                           " Content-Length="+
                           _inputStream.getContentLength());
            
            // Pass the request to the server
            if (getServer()==null)
                throw new HttpException(_response.__503_Service_Unavailable);
            getServer().service(_request,_response);

            // Complete the request
            if (_persistent)
            {
                // Read any remaining content
                while(_inputStream.skip(4096)>0 || _inputStream.read()>=0);
                if (_inputStream.getContentLength()>0)
                    throw new HttpException(_response.__400_Bad_Request);
                _inputStream.resetStream();
            }
            
            // Complete the response
            _response.completeSend();
            
            if (_persistent)
            {
                if (_outputStream.isChunking())
                    _outputStream.endChunking(); 
                _outputStream.resetStream();
            }
            else
                _outputStream.close();
                
        }
        catch (HttpException e)
        {
            // Handle HTTP Exception by sending error code (if output not
            // committed) and closing connection.
            Code.debug(e);
            _persistent=false;
            if (_outputStream.isCommitted())
                Code.warning(e.toString());
            else
            {
                _outputStream.resetBuffer();
                _response.setField(HttpFields.__Connection,
                                   HttpFields.__Close);
                _response.sendError(e.getCode());
            }
        }
        catch (Exception e)
        {
            // Handle Exception by sending 500 error code (if output not
            // committed) and closing connection.
            Code.debug(e);
            _persistent=false;
            if (_outputStream.isCommitted())
                Code.warning(e.toString());
            else
            {
                _outputStream.resetBuffer();
                _response.setField(HttpFields.__Connection,
                                  HttpFields.__Close);
                _response.sendError(HttpResponse.__500_Internal_Server_Error);
            }
        }
        finally
        {
            if (_request!=null)
                _request.destroy();
            if (_response!=null)
                _response.destroy();
        }
        return _persistent;
    }

    /* ------------------------------------------------------------ */
    /* Verify HTTP/1.0 request
     * @exception HttpException problem with the request. 
     * @exception IOException problem with the connection.
     */
    private void verifyHTTP_1_0()
        throws HttpException, IOException
    {     
        // Set content length
        int content_length=
            _request.getIntField(HttpFields.__ContentLength);
        if (content_length>=0)
            _inputStream.setContentLength(content_length);
        else if (content_length<0)
        {
            // Can't have content without a content length
            String content_type=_request.getField(HttpFields.__ContentType);
            if (content_type!=null && content_type.length()>0)
                throw new HttpException(_response.__411_Length_Required);
            _inputStream.setContentLength(0);
        }

        // dont support persistent connections in HTTP/1.0
        _response.setField(HttpFields.__Connection,HttpFields.__Close);
        _persistent=false;
    }
    
    /* ------------------------------------------------------------ */
    /* Verify HTTP/1.1 request
     * @exception HttpException problem with the request. 
     * @exception IOException problem with the connection.
     */
    private void verifyHTTP_1_1()
        throws HttpException, IOException
    {        
        // Check Host Field exists
        String host=_request.getField(HttpFields.__Host);
        if (host==null || host.length()==0)
            throw new HttpException(_response.__400_Bad_Request);
        if (!host.equals(_request.getHost()))
        {
            Code.warning("XXX Host field does not match URI");
            Code.debug(_request);
        }
        
        // check and enable requests transfer encodings.
        boolean _inputEncodings=false;
        List transfer_coding=_request.getFieldValues(HttpFields.__TransferEncoding);
        if (transfer_coding!=null)
        {
            HashMap coding_params = new HashMap(7);
            for (int i=transfer_coding.size(); i-->0;)
            {
                coding_params.clear();
                String coding =
                    HttpFields.valueParameters(transfer_coding.get(i).toString(),
                                               coding_params);
                coding=StringUtil.asciiToLowerCase(coding);

                // Ignore identity coding
                if (HttpFields.__Identity.equals(coding))
                    continue;

                // We have none identity encodings
                _inputEncodings=true;
                
                // Handle Chunking
                if (HttpFields.__Chunked.equals(coding))
                {
                    // chunking must be last and have no parameters
                    if (i+1!=transfer_coding.size() ||
                        coding_params.size()>0)
                        throw new HttpException(_response.__400_Bad_Request);
                    _inputStream.setChunking();
                }
                else
                    getServer().enableEncoding(_inputStream,coding,coding_params);
            }
        }
        
        // Check input content length can be determined
        int content_length=_request.getIntField(HttpFields.__ContentLength);
        String content_type=_request.getField(HttpFields.__ContentType);
        if (_inputEncodings)
        {
            // Must include chunked
            if (!_inputStream.isChunking())
                throw new HttpException(_response.__400_Bad_Request);
        }
        else
        {
            // If we have a content length, use it
            if (content_length>=0)
                _inputStream.setContentLength(content_length);
            // else if we have no content
            else if (content_type==null || content_type.length()==0)
                _inputStream.setContentLength(0);
            // else we need a content length
            else
                throw new HttpException(_response.__411_Length_Required);
        }

        // Handle Continue Expectations
        String expect=_request.getField(HttpFields.__Expect);
        if (expect!=null && expect.length()>0)
        {
            if (StringUtil.asciiToLowerCase(expect)
                .equals(HttpFields.__ExpectContinue))
            {
                // Send continue if no body available yet.
                if (_inputStream.available()<=0)
                {
                    _outputStream.getRawStream()
                        .write(_response.__Continue);
                    _outputStream.getRawStream()
                        .flush();
                }
            }
            else
                throw new HttpException(_response.__417_Expectation_Failed);
        }
        else if (_inputStream.available()<=0 &&
                 (_request.__PUT.equals(_request.getMethod()) ||
                  _request.__POST.equals(_request.getMethod())))
        {
            // Send continue for RFC 2068 exception
            _outputStream.getRawStream()
                .write(_response.__Continue);
            _outputStream.getRawStream()
                .flush();
        }  
            
             
        // Persistent unless requested otherwise
        _persistent=!_close;
         
        // XXX Should check TE header, but lets always chunk persistent connections
        if (_persistent)
            _outputStream.setChunking();  
    }
    

    /* ------------------------------------------------------------ */
    /** Output Notifications.
     * Trigger header and/or filters from output stream observations.
     * Called as a result of the connection subscribing for notifications
     * to the ChunkableOutputStream.
     * @see ChunkableOutputStream
     * @param out The output stream observed.
     * @param action The action.
     */
    public void outputNotify(ChunkableOutputStream out, int action)
        throws IOException
    {
        switch(action)
        {
          case OutputObserver.__FIRST_WRITE:
              Code.debug("notify FIRST_WRITE");
              // XXX Output encodings
              // XXX Install user filters
              break;
              
          case OutputObserver.__RESET_BUFFER:
              Code.debug("notify RESET_BUFFER");
              break;
              
          case OutputObserver.__COMMITING:
              Code.debug("notify COMMITING");
              // XXX Unchunked 1.1 requests with content length may
              // may be made persistent here.
              _headerBuffer.reset();
              _response.writeHeader(_headerBuffer);
              _headerBuffer.writeTo(_outputStream.getRawStream());
              break;
              
          case OutputObserver.__COMMITED:
              Code.debug("notify COMMITED");
              break;
              
          case OutputObserver.__CLOSING:
              Code.debug("notify CLOSING");
              break;
              
          case OutputObserver.__CLOSED:
              Code.debug("notify CLOSED");
              break;
        }
    }
};





