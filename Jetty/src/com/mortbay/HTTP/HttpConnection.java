// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
//import com.sun.java.util.collections.*; XXX-JDK1.1

import com.mortbay.Util.*;
import java.util.*;
import java.io.*;
import java.net.InetAddress;


/* ------------------------------------------------------------ */
/** A HTTP Connection
 * This class provides the generic HTTP handling for
 * a connection to a HTTP server. An instance of HttpConnection
 * is normally created by a HttpListener and then given control
 * in order to run the protocol handling before and after passing
 * a request to the HttpServer of the HttpListener.
 *
 * @see HttpListener
 * @see HttpServer
 * @version 1.0 Wed Oct  6 1999
 * @author Greg Wilkins (gregw)
 */
public class HttpConnection
    implements OutputObserver
{
    /* ------------------------------------------------------------ */
    private HttpListener _listener;
    private ChunkableInputStream _inputStream;
    private ChunkableOutputStream _outputStream;
    private boolean _persistent;
    private boolean _close;
    private boolean _keepAlive;
    private String _version;
    private boolean _http1_1;
    private boolean _http1_0;
    private HttpRequest _request;
    private HttpResponse _response;
    private Thread _handlingThread;
    private InetAddress _remoteAddr;

    /* ------------------------------------------------------------ */
    private static final DateCache __dateCache=
	new DateCache("dd/MMM/yyyy:HH:mm:ss");


    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param listener The listener that created this connection.
     * @param remoteAddr The address of the remote end or null.
     * @param in InputStream to read request(s) from.
     * @param out OutputputStream to write response(s) to.
     */
    protected HttpConnection(HttpListener listener,
                             InetAddress remoteAddr,
                             InputStream in,
                             OutputStream out)
    {
        _listener=listener;
        _remoteAddr=remoteAddr;
	if (in instanceof ChunkableInputStream)
	    throw new IllegalArgumentException("InputStream is already chunkable");
        _inputStream=new ChunkableInputStream(in);
	if (out instanceof ChunkableOutputStream)
	    throw new IllegalArgumentException("OutputStream is already chunkable");
        _outputStream=new ChunkableOutputStream(out);
        _outputStream.addObserver(this);
    }

    /* ------------------------------------------------------------ */
    /** Get the Remote address.
     * @return the remote address
     */
    public InetAddress getRemoteAddr()
    {
        return _remoteAddr;
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
    /** Get the request
     * @return the request
     */
    public HttpRequest getRequest()
    {
        return _request;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the response
     * @return the response
     */
    public HttpResponse getResponse()
    {
        return _response;
    }
    
    /* ------------------------------------------------------------ */
    /** Close the connection.
     * This method calls close on the input and output streams and
     * interrupts any thread in the handle method.
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
    /** Get the listeners Default scheme. 
     * Conveniance method equivalent to getListener().getDefaultProtocol().
     * @return HttpServer.
     */
    public String getDefaultScheme()
    {
        return _listener.getDefaultScheme();
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
     * The service(request,response) method is called by handle to
     * service each request received on the connection.
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
    /** Service a Request.
     * This implementation passes the request and response to the
     * service method of the HttpServer for this connections listener.
     * If no HttpServer has been associated, the 503 is returned.
     * This method may be specialized to implement other ways of
     * servicing a request.
     * @param request The request
     * @param response The response
     * @exception HttpException 
     * @exception IOException 
     */
    protected void service(HttpRequest request, HttpResponse response)
        throws HttpException, IOException
    {
        if (getServer()==null)
                throw new HttpException(response.__503_Service_Unavailable);
        getServer().service(request,response);
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
        _keepAlive=false;
        _http1_0=true;
        _http1_1=false;
	boolean logRequest=false;
	
        try
        {       
            Code.debug("Wait for request header...");

            try
            {
                _request.readHeader(getInputStream());
            }
            catch(HttpException e)
            {
                throw e;
            }
            catch(IOException e)
            {
                Code.ignore(e);
                return false;
            }
            logRequest=true;
	    
            if (_request.getState()!=HttpMessage.__MSG_RECEIVED)
                throw new HttpException(_response.__400_Bad_Request);

	    if (Code.debug())
	    {
		_response.setField("Jetty-Request",_request.getRequestLine());
		Code.debug("REQUEST:\n"+_request.toString());
	    }
	    
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
            _response.setField(HttpFields.__Server,Version.__Version);
            _response.setField(HttpFields.__MimeVersion,"1.0");
            _response.setCurrentTime(HttpFields.__Date);

            
            // Handle Connection header field
            List connectionValues = _request.getFieldValues(HttpFields.__Connection);
            if (connectionValues!=null)
            {
                Iterator iter = connectionValues.iterator();
                while (iter.hasNext())
                {
                    String token=StringUtil.asciiToLowerCase(iter.next().toString());
                
                    // handle close token
                    if (token.equals(HttpFields.__Close))
                    {
                        _close=true;
                        _response.setField(HttpFields.__Connection,
                                           HttpFields.__Close);
                    }
                    else if (token.equals(HttpFields.__KeepAlive) && _http1_0)
                    {
                        _keepAlive=true;
                    }
                    
                    // Remove headers for HTTP/1.0 requests
                    if (HttpMessage.__HTTP_1_0.equals(_version))
                        _request.forceRemoveField(token);
                }
            }
            
            // Handle version specifics
            if (_http1_1)
                verifyHTTP_1_1();
            else if (_http1_0)
                verifyHTTP_1_0();
            else
                throw new HttpException(_response.__505_HTTP_Version_Not_Supported);
            if (Code.verbose(99))
                Code.debug("IN is "+
                           (_inputStream.isChunking()?"chunked":"not chunked")+
                           " Content-Length="+
                           _inputStream.getContentLength());
            
            // service the request
            service(_request,_response);

            // Complete the request
            if (_persistent)
            {
                try{
                    // Read remaining input
                    while(_inputStream.skip(4096)>0 || _inputStream.read()>=0);
                }
                catch(IOException e)
                {
                    if (_inputStream.getContentLength()>0)
                        _inputStream.setContentLength(0);
                    throw new HttpException(_response.__400_Bad_Request,
                                            "Missing Content");
                }

                // Check for no more content
                if (_inputStream.getContentLength()>0)
                {
                    _inputStream.setContentLength(0);
                    throw new HttpException(_response.__400_Bad_Request,
                                            "Missing Content");
                }

                // Commit the response
		if (!_response.isCommitted())
		    _response.commit();
                _inputStream.resetStream();
                if (_outputStream.isChunking())
                    _outputStream.endChunking(); 
                else
		    _outputStream.resetStream();
            }
            else
            {
                _response.commit();
                _outputStream.close();
            }
	    
	    Code.debug("RESPONSE:\n"+_response.toString());
	    
        }
        catch (HttpException e)
        {
            // Handle HTTP Exception by sending error code (if output not
            // committed) and closing connection.
            _persistent=false;
            if (_outputStream.isCommitted())
            {
                Code.warning(e.toString());
            }
            else
            {
                _outputStream.resetBuffer();
                _response.removeField(HttpFields.__TransferEncoding);
                _response.setField(HttpFields.__Connection,
                                   HttpFields.__Close);
                _response.sendError(e);
            }
        }
        catch (Exception e)
        {
            // Handle Exception by sending 500 error code (if output not
            // committed) and closing connection.
	    Code.warning(_request.toString()+":\n"+_request.toString(),e);
	    
            _persistent=false;
            if (!_response.isCommitted())
            {
                _response.reset();
                _response.removeField(HttpFields.__TransferEncoding);
                _response.setField(HttpFields.__Connection,
                                  HttpFields.__Close);
                _response.sendError(HttpResponse.__500_Internal_Server_Error);
            }
        }
        catch (Error e)
        {
	    Code.warning(e);
            
            _persistent=false;
            if (!_outputStream.isCommitted())
            {
                _outputStream.resetBuffer();
                _response.removeField(HttpFields.__TransferEncoding);
                _response.setField(HttpFields.__Connection,
                                  HttpFields.__Close);
                _response.sendError(HttpResponse.__500_Internal_Server_Error);
            }
	}
        finally
        {
	    // Log request - XXX should be in HttpHandler
	    if (logRequest && _request!=null && _response!=null)
	    {
		int length =
		    _response.getIntField(HttpFields.__ContentLength);
		String bytes = ((length>=0)?Long.toString(length):"-");
		String user = (String)_request.getAttribute(HttpRequest.__AuthUser);
		if (user==null)
		    user = "-";

		String referer = _request.getField(HttpFields.__Referer);
		if (referer==null)
		    referer="-";
		else
		    referer="\""+referer+"\"";
		
		String agent = _request.getField(HttpFields.__UserAgent);
		if (agent==null)
		agent="-";
		else
		    agent="\""+agent+"\"";

		String addr="127.0.0.1";
		if (_remoteAddr!=null)
		    addr=_remoteAddr.getHostAddress();
		
		String log= addr +
		    " - "+
		    user +
		    " [" +
		    __dateCache.format(System.currentTimeMillis())+
		    "] \""+
		    _request.getRequestLine()+
		    "\" "+
		    _response.getStatus()+
		    " " +
		    bytes +
		    " " +
		    referer +
		    " " +
		    agent;
		System.out.println(log);
	    }
	    

	    
            if (_request!=null)
                _request.destroy();
	    _request=null;
            if (_response!=null)
                _response.destroy();
	    _response=null;
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
        _persistent=_keepAlive;
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
                    getServer().getHttpEncoding()
			.enableEncoding(_inputStream,coding,coding_params);
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

    }
    

    /* ------------------------------------------------------------ */
    /** Output Notifications.
     * Trigger header and/or filters from output stream observations.
     * Also finalizes method of indicating response content length.
     * Called as a result of the connection subscribing for notifications
     * to the ChunkableOutputStream.
     * @see ChunkableOutputStream
     * @param out The output stream observed.
     * @param action The action.
     */
    public void outputNotify(ChunkableOutputStream out, int action)
        throws IOException
    {
	if (_response==null)
	    return;
	
        switch(action)
        {
          case OutputObserver.__FIRST_WRITE:
              if(Code.verbose()) Code.debug("notify FIRST_WRITE ");
	      
              // Determine how to limit content length and
              // enable output transfer encodings 
              List transfer_coding=_response.getFieldValues(HttpFields.__TransferEncoding);
              if (transfer_coding==null || transfer_coding.size()==0)
              {
                  // Default to chunking for HTTP/1.1
                  if (_http1_1)
                  {
                      _response.removeField(HttpFields.__ContentLength);
                      _response.setField(HttpFields.__TransferEncoding,
                                         HttpFields.__Chunked);
                      _outputStream.setChunking();
                  }
                  else if (_http1_0)
                  {
                      // If we dont have a content length, we can't be persistent
                      if (!_keepAlive || !_persistent ||
                          _response.getIntField(HttpFields.__ContentLength)<0)
                      {
                          _persistent=false;
                          _response.setField(HttpFields.__Connection,
                                             HttpFields.__Close);
                      }
		      else if (_keepAlive)
			  _response.setField(HttpFields.__Connection,
					     HttpFields.__KeepAlive);
                  }
              }
              else if (_http1_0)
              {
                  // Error for transfer encoding to be set in HTTP/1.0
                  _response.removeField(HttpFields.__TransferEncoding);
                  throw new HttpException(_response.__501_Not_Implemented,
                                          "Transfer-Encoding not supported in HTTP/1.0");
              }
              else
              {
                  // Examine and apply transfer encodings
                  
                  HashMap coding_params = new HashMap(7);
                  for (int i=transfer_coding.size();i-->0;)
                  {
                      coding_params.clear();
                      String coding =
                          HttpFields.valueParameters(transfer_coding.get(i).toString(),
                                                     coding_params);
                      coding=StringUtil.asciiToLowerCase(coding);

                      // Ignore identity coding
                      if (HttpFields.__Identity.equals(coding))
                          continue;
                
                      // Handle Chunking
                      if (HttpFields.__Chunked.equals(coding))
                      {
                          // chunking must be last and have no parameters
                          if (i+1!=transfer_coding.size() ||
                              coding_params.size()>0)
                              throw new HttpException(_response.__400_Bad_Request,
                                                      "Missing or incorrect chunked transfer-encoding");
                          out.setChunking();
                      }
                      else
                      {
                          // Check against any TE field
                          List te = _request.getAcceptableTransferCodings();
                          if (te==null || !te.contains(coding))
                              throw new HttpException(_response.__501_Not_Implemented,
                                                      "User agent does not accept "+
                                                      coding+
                                                      " transfer-encoding");

                          // Set coding
                          getServer().getHttpEncoding()
			      .enableEncoding(out,coding,coding_params);
                      }
                  }
              }

              // Nobble the OutputStream for HEAD requests
              if (_request.__HEAD.equals(_request.getMethod()))
                  _outputStream.nullOutput();
              break;
              
          case OutputObserver.__RESET_BUFFER:
              if(Code.verbose()) Code.debug("notify RESET_BUFFER");
              break;
              
          case OutputObserver.__COMMITING:
              if(Code.verbose()) Code.debug("notify COMMITING");
	      _response.commit();
              break;
              
          case OutputObserver.__COMMITED:
              if(Code.verbose()) Code.debug("notify COMMITED");
              break;
              
          case OutputObserver.__CLOSING:
              if(Code.verbose()) Code.debug("notify CLOSING");
	      _response.complete();
              break;
              
          case OutputObserver.__CLOSED:
              if(Code.verbose()) Code.debug("notify CLOSED");
              break;
        }
    }

    /* ------------------------------------------------------------ */
    /** 
     */
    void commitResponse()
    {
	
	// if we have no content or encoding,
	// and no content length
	// need to set content length (XXX or may just close connection?)
	if (!_outputStream.isWritten() &&
	    !_response.containsField(HttpFields.__TransferEncoding) &&
	    !_response.containsField(HttpFields.__ContentLength))
	{
	    if(_persistent)
	    {
		_response.setIntField(HttpFields.__ContentLength,0);
		if (_http1_0)
		    _response.setField(HttpFields.__Connection,
				       HttpFields.__KeepAlive);
	    }
	    else
	    {
		_close=true;
		_response.setField(HttpFields.__Connection,
				   HttpFields.__Close);
	    }
	}
    }
}

