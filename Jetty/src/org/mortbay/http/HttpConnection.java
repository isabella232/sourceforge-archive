// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import org.mortbay.util.Code;
import org.mortbay.util.LifeCycle;
import org.mortbay.util.StringUtil;
import org.mortbay.util.ThreadPool;
import org.mortbay.util.ThreadPool.PoolThread;


/* ------------------------------------------------------------ */
/** A HTTP Connection.
 * This class provides the generic HTTP handling for
 * a connection to a HTTP server. An instance of HttpConnection
 * is normally created by a HttpListener and then given control
 * in order to run the protocol handling before and after passing
 * a request to the HttpServer of the HttpListener.
 *
 * This class is not synchronized as it should only ever be known
 * to a single thread.
 *
 * @see HttpListener
 * @see HttpServer
 * @version $Id$
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
    private int _dotVersion;
    private boolean _outputSetup;
    private HttpRequest _request;
    private HttpResponse _response;
    private Thread _handlingThread;
    private InetAddress _remoteAddr;
    private String _remoteHost;
    private HttpServer _httpServer;
    private Object _connection;
    private HashMap _codingParams;
    private ArrayList _codings;

    private boolean _statsOn;
    private long _tmpTime;
    private long _openTime;
    private long _reqTime;
    private int _requests;
    private Object _object;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param listener The listener that created this connection.
     * @param remoteAddr The address of the remote end or null.
     * @param in InputStream to read request(s) from.
     * @param out OutputputStream to write response(s) to.
     * @param connection The underlying connection object, most likely
     * a socket. This is not used by HttpConnection other than to make
     * it available via getConnection().
     */
    public HttpConnection(HttpListener listener,
                          InetAddress remoteAddr,
                          InputStream in,
                          OutputStream out,
                          Object connection)
    {
        Code.debug("new HttpConnection: ",connection);
        _listener=listener;
        _remoteAddr=remoteAddr;
        _inputStream=new ChunkableInputStream(in);
        _outputStream=new ChunkableOutputStream(out);
        _outputStream.addObserver(this);
        _outputSetup=false;
        if (_listener!=null)
            _httpServer=_listener.getHttpServer();
        _connection=connection;
        
        _statsOn=_httpServer!=null && _httpServer.getStatsOn();
        if (_statsOn)
        {
            _openTime=System.currentTimeMillis();
            _httpServer.statsOpenConnection();
        }
        _reqTime=0;
        _requests=0;
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
    /** Get the Remote hostname as a String.
     * @return the remote address
     */
    public String getRemoteHost()
    {
        if (_remoteHost==null)
        {
            if (_remoteAddr==null)
                return "localhost";
            _remoteHost=_remoteAddr.getHostName();
        }
        return _remoteHost;
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
    /** Get the underlying connection object.
     * This opaque object, most likely a socket. This is not used by
     * HttpConnection other than to make it available via getConnection().
     * @return Connection abject
     */
    public Object getConnection()
    {
        return _connection;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the request.
     * @return the request
     */
    public HttpRequest getRequest()
    {
        return _request;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the response.
     * @return the response
     */
    public HttpResponse getResponse()
    {
        return _response;
    }

    /* ------------------------------------------------------------ */
    /** Force the connection to not be persistent.
     */
    public void forceClose()
    {
        _persistent=false;
        _close=true;
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
        try{
            _outputStream.close();
            _inputStream.close();
        }
        finally
        {
            if (_handlingThread!=null)
                _handlingThread.interrupt();
        }
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
     * Conveniance method equivalent to getListener().getHttpServer().
     * @return HttpServer.
     */
    public HttpServer getHttpServer()
    {
        return _httpServer;
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
    /** Get associated object.
     * Used by a particular HttpListener implementation to associate
     * private datastructures with the connection.
     * @return An object associated with the connecton by setObject.
     */
    public Object getObject()
    {
        return _object;
    }
    
    /* ------------------------------------------------------------ */
    /** Set associated object.
     * Used by a particular HttpListener implementation to associate
     * private datastructures with the connection.
     * @param o An object associated with the connecton.
     */
    public void setObject(Object o)
    {
        _object=o;
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
            // XXX - can't do this check because IE does this after
            // a redirect.
            // Can't have content without a content length
            // String content_type=_request.getField(HttpFields.__ContentType);
            // if (content_type!=null && content_type.length()>0)
            //     throw new HttpException(_response.__411_Length_Required);
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
        Enumeration transfer_coding=_request.getFieldValues(HttpFields.__TransferEncoding,
                                                            HttpFields.__separators);
        boolean input_encodings=false;
        if (transfer_coding!=null)
        {
            if (_codings==null)
                _codings=new ArrayList(2);
            else
            {
                _codings.clear();
                if (_codingParams!=null)
                    _codingParams.clear();
            }
            
            while(transfer_coding.hasMoreElements())
                _codings.add(transfer_coding.nextElement());

            for(int i=_codings.size();i-->0;)
            {        
                String value=_codings.get(i).toString();
                if (_codingParams==null && value.indexOf(';')>0)
                    _codingParams=new HashMap(7);
                String coding=HttpFields.valueParameters(value,_codingParams);
                
                if (HttpFields.__Identity.equalsIgnoreCase(coding))
                    continue;
                
                input_encodings=true;
                
                if (HttpFields.__Chunked.equalsIgnoreCase(coding))
                {
                    // chunking must be last and have no parameters
                    if (i+1<_codings.size() || _codingParams!=null&&_codingParams.size()>0)
                        throw new HttpException(HttpResponse.__400_Bad_Request);
                    _inputStream.setChunking();
                }
                else
                {
                    getHttpServer().getHttpEncoding()
                        .enableEncoding(_inputStream,coding,_codingParams);
                }
            }
        }
        
        // Check input content length can be determined
        int content_length=_request.getIntField(HttpFields.__ContentLength);
        String content_type=_request.getField(HttpFields.__ContentType);
        if (input_encodings)
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
            {
                // XXX - can't do this check as IE stuff up on
                // a redirect.
                // throw new HttpException(_response.__411_Length_Required);
                _inputStream.setContentLength(0);
            }
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
    public void outputNotify(ChunkableOutputStream out, int action, Object ignoredData)
        throws IOException
    {
        if (_response==null)
            return;

        switch(action)
        {
          case OutputObserver.__FIRST_WRITE:
              if (!_outputSetup)
                  setupOutputStream();
              break;
              
          case OutputObserver.__RESET_BUFFER:
              _outputSetup=false;
              break;
              
          case OutputObserver.__COMMITING:
              if (_response.getState()==HttpMessage.__MSG_EDITABLE)
                  _response.commitHeader();
              break;
              
          case OutputObserver.__CLOSING:
              break;
              
          case OutputObserver.__CLOSED:
              break;
        }
    }

    /* ------------------------------------------------------------ */
    /** Setup the reponse output stream.
     * Use the current state of the request and response, to set tranfer
     * parameters such as chunking and content length.
     */
    public void setupOutputStream()
        throws IOException
    {
        if (_outputSetup)
            return;
        _outputSetup=true;
        
        // Determine how to limit content length and
        // enable output transfer encodings 
        Enumeration transfer_coding=_response.getFieldValues(HttpFields.__TransferEncoding,
                                                             HttpFields.__separators);
        if (transfer_coding==null || !transfer_coding.hasMoreElements())
        {
            switch(_dotVersion)
            {
              case 1:
                  {
                      // if forced or (not closed and no length)
                      if (_listener.getHttpServer().isChunkingForced() ||
                          (!HttpFields.__Close.equals(_response.getField(HttpFields.__Connection)))&&
                          (_response.getField(HttpFields.__ContentLength)==null))
                      {
                          // Chunk it!
                          _response.removeField(HttpFields.__ContentLength);
                          _response.setField(HttpFields.__TransferEncoding,
                                         HttpFields.__Chunked);
                          _outputStream.setChunking();
                      }
                      break;
                  }
              case 0:
                  {
                      // If we dont have a content length (except 304 replies), 
		      // or we have been requested to close
		      // then we can't be persistent 
                      if (!_keepAlive || !_persistent ||
                          HttpResponse.__304_Not_Modified!=_response.getStatus() &&
                          _response.getField(HttpFields.__ContentLength)==null ||
                          HttpFields.__Close.equals(_response.getField(HttpFields.__Connection)))
                      {
                          _persistent=false;
                          if (_keepAlive)
                              _response.setField(HttpFields.__Connection,
                                                 HttpFields.__Close);
                          _keepAlive=false;
                      }
                      else if (_keepAlive)
                          _response.setField(HttpFields.__Connection,
                                             HttpFields.__KeepAlive);
                      break;
                  }
              default:
                  _keepAlive=false;
                  _persistent=false;
            }
        }
        else if (_dotVersion<1)
        {
            // Error for transfer encoding to be set in HTTP/1.0
            _response.removeField(HttpFields.__TransferEncoding);
            throw new HttpException(_response.__501_Not_Implemented,
                                    "Transfer-Encoding not supported in HTTP/1.0");
        }
        else
        {
            // Use transfer encodings to determine length
            _response.removeField(HttpFields.__ContentLength);
            
            if (_codings==null)
                _codings=new ArrayList(2);
            else
            {
                _codings.clear();
                if (_codingParams!=null)
                    _codingParams.clear();
            }

            // Handle transfer encoding
            while(transfer_coding.hasMoreElements())
                _codings.add(transfer_coding.nextElement());
            for(int i=_codings.size();i-->0;)
            {        
                String value=_codings.get(i).toString();
                if (_codingParams==null && value.indexOf(';')>0)
                    _codingParams=new HashMap(7);
                String coding=HttpFields.valueParameters(value,_codingParams);
                
                if (HttpFields.__Identity.equalsIgnoreCase(coding))
                    continue;                

                // Ignore identity coding
                if (HttpFields.__Identity.equalsIgnoreCase(coding))
                    continue;
                
                // Handle Chunking
                if (HttpFields.__Chunked.equalsIgnoreCase(coding))
                {
                    // chunking must be last and have no parameters
                    if (i+1<_codings.size() || _codingParams!=null&&_codingParams.size()>0)
                        throw new HttpException(_response.__400_Bad_Request,
                                                "Missing or incorrect chunked transfer-encoding");
                    _outputStream.setChunking();
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
                    getHttpServer().getHttpEncoding()
                        .enableEncoding(_outputStream,coding,_codingParams);
                }
            }
        }

        // Nobble the OutputStream for HEAD requests
        if (_request.__HEAD.equals(_request.getMethod()))
            _outputStream.nullOutput();
    }

    
    /* ------------------------------------------------------------ */
    void commitResponse()
        throws IOException
    {            
        _outputSetup=true;
        
        // Handler forced close, listener stopped or no idle threads left.
        _close=HttpFields.__Close.equals(_response.getField(HttpFields.__Connection));
        if (!_close && (!_listener.isStarted()||_listener.isOutOfResources()))
        {
            _close=true;
            _response.setField(HttpFields.__Connection,
                               HttpFields.__Close);
        }
        if (_close)
            _persistent=false;
        
        // if we have no content or encoding,
        // and no content length
        int status = _response.getStatus();
        if (status!=HttpResponse.__304_Not_Modified &&
            status!=HttpResponse.__204_No_Content &&
            !_outputStream.isWritten() &&
            !_response.containsField(HttpFields.__ContentLength) &&
            !_response.containsField(HttpFields.__TransferEncoding))
        {
            if(_persistent)
            {
                if (status>=300 && status<400)
                {
                    _response.setField(HttpFields.__ContentLength,"0");
                }
                else
                {    
                    switch (_dotVersion)
                    {
                      case 0:
                          {
                              _close=true;
                              _persistent=false;
                              _response.setField(HttpFields.__Connection,
                                                 HttpFields.__Close);
                          }
                          break;
                      case 1:
                          {
                              // force chunking on.
                              _response.setField(HttpFields.__TransferEncoding,
                                                 HttpFields.__Chunked);
                              _outputStream.setChunking();
                          }
                          break;
                          
                      default:
                          _close=true;
                          _response.setField(HttpFields.__Connection,
                                             HttpFields.__Close);
                          break;
                    }
                }
            }
            else
            {
                _close=true;
                _response.setField(HttpFields.__Connection,
                                   HttpFields.__Close);
            }
        }
    }

    
    /* ------------------------------------------------------------ */
    /* Exception reporting policy method.
     * @param e the Throwable to report.
     */
    private void exception(Throwable e)
    {
	try{
	    boolean gotIOException = false;
            if (e instanceof HttpException)
            {
                if (_request==null)
                    Code.warning(e.toString());
                else
                    Code.warning(_request.getRequestLine()+" "+e.toString());
                Code.debug(e);
            }
            else if (e instanceof IOException)
	    {
                // Assume browser closed connection
                gotIOException = true;
                if (Code.verbose())
                    Code.debug(e);
                else if (Code.debug())
                    Code.debug(e.toString());
	    }
	    else 
            {
                if (_request==null)
                    Code.warning(e);
                else
                    Code.warning(_request.getRequestLine(),e);
            }
            
	    _persistent=false;
	    if (_response != null && !_response.isCommitted())
	    {
		_response.reset();
		_response.removeField(HttpFields.__TransferEncoding);
		_response.setField(HttpFields.__Connection,
				   HttpFields.__Close);
		
		_response.sendError(HttpResponse.__500_Internal_Server_Error,e);

                // probabluy not browser so be more verbose
		if (gotIOException)
                {
                    if (_request==null)
                        Code.warning(e);
                    else
                        Code.warning(_request.getRequestLine(),e);
                }
	    }
	}
        catch(Exception ex)
        {
            Code.ignore(ex);
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
     * @return The HttpContext that completed handling of the request or null.
     * @exception HttpException 
     * @exception IOException 
     */
    protected HttpContext service(HttpRequest request, HttpResponse response)
        throws HttpException, IOException
    {
        if (_httpServer==null)
                throw new HttpException(response.__503_Service_Unavailable);
        return _httpServer.service(request,response);
    }
    
    /* ------------------------------------------------------------ */
    /** Handle the connection.
     * Once the connection has been created, this method is called
     * to handle one or more requests that may be received on the
     * connection.  The method only returns once all requests have been
     * handled, an error has been returned to the requestor or the
     * connection has been closed.
     * The handleNext() is called in a loop until it returns false,
     */
    public void handle()
    {
        while(_listener.isStarted())
            if (!handleNext())
                break;
    }
    
    /* ------------------------------------------------------------ */
    /** Handle next request off the connection.
     * The service(request,response) method is called by handle to
     * service each request received on the connection.
     * If the thread is a PoolThread, the thread is set as inactive
     * when waiting for a request. 
     * @return true if the connection is still open and may provide
     * more requests.
     */
    public boolean handleNext()
    {
        _handlingThread=Thread.currentThread();
        PoolThread poolThread=(_handlingThread instanceof PoolThread)
            ? ((PoolThread)_handlingThread):null;
        
        HttpContext context=null;
        try
        {
            // Create or recycle connection
            if (_request!=null)
            {
                _request.recycle(this);
                if (_response!=null)
                    _response.recycle(this);
                else
                    _response = new HttpResponse(this);
            }
            else
            {
                _request = new HttpRequest(this);
                _response = new HttpResponse(this);
            }
            
            // Assume the connection is not persistent,
            // unless told otherwise.
            _persistent=false;
            _close=false;
            _keepAlive=false;
            _dotVersion=0;
            Code.debug("Wait for request...");
            
            try
            {
                _outputSetup=false;
                if (poolThread!=null)
                    poolThread.setActive(false);
                _request.readHeader(getInputStream());
                if (poolThread!=null)
                    poolThread.setActive(true);
                _listener.customizeRequest(this,_request);
            }
            catch(HttpException e){throw e;}
            catch(IOException e)
            {
                if (_request.getState()!=HttpMessage.__MSG_RECEIVED)
                {
                    if (Code.debug())
                    {
                        if (Code.verbose())
                            Code.debug(e);
                        else
                            Code.debug(e.toString());
                    }
                    _persistent=false;
                    _response.destroy();
                    _response=null;
                    return _persistent;
                }
                
                exception(e);
                _persistent=false;
                _response.destroy();
                _response=null;
                return _persistent;
            }
            
            if (_request.getState()!=HttpMessage.__MSG_RECEIVED)
                throw new HttpException(_response.__400_Bad_Request);
            
            // We have a valid request!
            if (_statsOn)
            {
                _requests++;
                _tmpTime=_request.getTimeStamp();
                _reqTime=_tmpTime;
                _httpServer.statsGotRequest();
            }
            if (Code.debug())
            {
                _response.setField("Jetty-Request",
                                   _request.getRequestLine());
                Code.debug("REQUEST:\n",_request);
            }
            
            // Pick response version
            _version=_request.getVersion();
            _dotVersion=_request.getDotVersion();
            
            if (_dotVersion>1)
            {
                Code.debug("Respond to HTTP/1.X with HTTP/1.1");
                _version=HttpMessage.__HTTP_1_1;
                _dotVersion=1;
            }
            
            // Common fields on the response
            // XXX could be done faster?
            _response.setVersion(HttpMessage.__HTTP_1_1);
            _response.setDateField(HttpFields.__Date,_request.getTimeStamp());
            _response.setField(HttpFields.__Server,Version.__VersionDetail);
            _response.setField(HttpFields.__ServletEngine,Version.__ServletEngine);
            
            // Handle Connection header field
            Enumeration connectionValues =
                _request.getFieldValues(HttpFields.__Connection,
                                        HttpFields.__separators);
            if (connectionValues!=null)
            {
                while (connectionValues.hasMoreElements())
                {
                    String token=connectionValues.nextElement().toString();
                    // handle close token
                    if (token.equalsIgnoreCase(HttpFields.__Close))
                    {
                        _close=true;
                        _response.setField(HttpFields.__Connection,
                                           HttpFields.__Close);
                    }
                    else if (token.equalsIgnoreCase(HttpFields.__KeepAlive) &&
                             _dotVersion==0)
                        _keepAlive=true;
                    
                    // Remove headers for HTTP/1.0 requests
                    if (_dotVersion==0)
                        _request.forceRemoveField(token);
                }
            }
            
            // Handle version specifics
            if (_dotVersion==1)
                verifyHTTP_1_1();
            else if (_dotVersion==0)
                verifyHTTP_1_0();
            else if (_dotVersion!=-1)
                throw new HttpException(_response.__505_HTTP_Version_Not_Supported);
            
            if (Code.verbose(99))
                Code.debug("IN is "+
                           (_inputStream.isChunking()
                            ?"chunked":"not chunked")+
                           " Content-Length="+
                           _inputStream.getContentLength());
            
            // service the request
            context=service(_request,_response);
        } 
        catch (InterruptedIOException e)
        {
            exception(e);
            _persistent=false;
            try
            {
                _response.commit();
                _outputStream.flush();
            }
            catch (IOException e2){exception(e2);}
        }
        catch (Exception e)     {exception(e);}
        catch (Error e)         {exception(e);}
        finally
        {
            int bytes_written=0;
            try
            {
                int content_length = _response==null
                    ?-1:_response.getIntField(HttpFields.__ContentLength);
                
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
                        _persistent=false;
                        Code.ignore(e);
                        exception(new HttpException(_response.__400_Bad_Request,
                                                    "Missing Content"));
                    }
                    
                    // Check for no more content
                    if (_inputStream.getContentLength()>0)
                    {
                        _inputStream.setContentLength(0);
                        _persistent=false;
                        exception (new HttpException(_response.__400_Bad_Request,
                                                     "Missing Content"));
                    }
                    
                    // Commit the response
                    try{
                        _outputStream.flush(true);
                        bytes_written=_outputStream.getBytesWritten();
                        _outputStream.resetStream();
                        _outputStream.addObserver(this);
                        _inputStream.resetStream();
                    }
                    catch(IOException e) {exception(e);}
                }
                else if (_response!=null) // There was a request
                {
                    // half hearted attempt to eat any remaining input
                    try{
                        if (_inputStream.getContentLength()>0)
                        while(_inputStream.skip(4096)>0 ||_inputStream.read()>=0);
                        _inputStream.resetStream();
                    }
                    catch(IOException e){Code.ignore(e);}
                
                    // commit non persistent
                    try{
                        _response.commit();
                        _outputStream.flush();
                        bytes_written=_outputStream.getBytesWritten();
                        _outputStream.close();
                        _outputStream.resetStream();
                    }
                    catch(IOException e) {exception(e);}
                }
                
                // Check response length
                if (_response!=null)
                {
                    Code.debug("RESPONSE:\n",_response);
                    if (_persistent &&
                        content_length>=0 && bytes_written>0 && content_length!=bytes_written)
                    {
                    Code.warning("Invalid length: Content-Length="+content_length+
                                 " bytes written="+bytes_written+
                                 " for "+_request.getRequestURL());
                    _persistent=false;
                    try{_outputStream.close();}
                    catch(IOException e) {Code.warning(e);}
                    }    
                }
            }
            finally
            {
                // stats & logging
                if (_statsOn && _reqTime>0)
                {
                    _httpServer.statsEndRequest(System.currentTimeMillis()-_reqTime,
                                                (_response!=null));
                    _reqTime=0;
                }
                if (context!=null)
                    context.log(_request,_response,bytes_written);
                
                if (_persistent)
                    _listener.persistConnection(this);
                else
                    destroy();
            }
        }

        return _persistent;
    }

    /* ------------------------------------------------------------ */
    /** Destroy the connection.
     * called by handleNext when handleNext returns false.
     */
    private void destroy()
    {
        // Destroy request and response
        if (_request!=null)
            _request.destroy();
        if (_response!=null)
            _response.destroy();
        _request=null;
        _response=null;
        _handlingThread=null;
        
        try{close();}
        catch (IOException e){Code.ignore(e);}
        catch (Exception e){Code.warning(e);}
        if (_statsOn)
        {
            _tmpTime=System.currentTimeMillis();
            if (_reqTime>0)
                _httpServer.statsEndRequest(_tmpTime-_reqTime,false);
            _httpServer.statsCloseConnection(_tmpTime-_openTime,_requests);
        }
    }
}
