/*
 * =============================================================
 *  Copyright 2003 Mort Bay Consulting Pty Ltd. All
 * rights reserved. Distributed under the artistic license. Created on 17-Apr-2003 $Id:
 * HttpConnection.java,v 1.5 2004/01/10 00:09:16 gregwilkins Exp $
 * =============================================================
 */

package org.mortbay.http.server;

import java.io.IOException;
import java.net.Socket;
import org.mortbay.http.HttpHeader;
import org.mortbay.http.HttpHeaderValues;
import org.mortbay.http.HttpHeaders;
import org.mortbay.http.HttpInputStream;
import org.mortbay.http.HttpMethods;
import org.mortbay.http.HttpOutputStream;
import org.mortbay.http.HttpStatus;
import org.mortbay.http.HttpVersions;
import org.mortbay.http.handler.DumpHandler;
import org.mortbay.io.Buffer;
import org.mortbay.io.Portable;
import org.mortbay.io.stream.InputStreamBuffer;
import org.mortbay.io.stream.OutputStreamBuffer;

/* ------------------------------------------------------------------------------- */
/**
 * Temporary connection class to get things running.
 */
public class HttpConnection implements Runnable
{
    protected Socket _socket;
    protected HttpInputStream _in;
    protected HttpOutputStream _out;
    private HttpListener _listener;
    private HttpRequest _request;
    private HttpResponse _response;
    private HttpHandler _handler;
    private boolean _throttled;

    public HttpConnection(HttpListener listener,Socket socket) throws IOException
    {
        _listener=listener;
        _socket=socket;
        InputStreamBuffer in=new InputStreamBuffer(socket,4096);
        _request=new HttpRequest(this);
        _in=new HttpInputStream(in,_request);
        _request.setHttpInputStream(_in);
        OutputStreamBuffer out=new OutputStreamBuffer(socket,4096);
        _response=new HttpResponse(_request);
        _out=new HttpOutputStream(out,_response,1024);
        _response.setHttpOutputStream(_out);
    }

    public HttpConnection(HttpListener listener,Buffer in,Buffer out)
    {
        _listener=listener;
        _request=new HttpRequest(this);
        _in=new HttpInputStream(in,_request);
        _request.setHttpInputStream(_in);
        _response=new HttpResponse(_request);
        _out=new HttpOutputStream(out,_response,out.capacity()/4);
        _response.setHttpOutputStream(_out);
    }

    public void setHandler(HttpHandler handler)
    {
        _handler=handler;
    }

    public HttpHandler getHandler()
    {
        return _handler;
    }

    public Socket getSocket()
    {
        return _socket;
    }

    public void run()
    {
        try
        {
            while(_out!=null&&_out.isPersistent())
            {
                if(!handleRequest())
                    break;
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean handleRequest() throws IOException
    {
        if(_handler==null)
            _handler=new DumpHandler();
        try
        {
            HttpHeader request=_in.readHeader();
            if(request==null)
                return false;
            if(request!=_request)
                Portable.throwIllegalState("Not request for this connection");
            int version=request.getVersionOrdinal();
            _out.setVersionOrdinal(version);
            _response.setVersion(HttpVersions.HTTP_1_1_BUFFER);
            switch(version)
            {
                case HttpVersions.HTTP_0_9_ORDINAL:
                    break;
                case HttpVersions.HTTP_1_0_ORDINAL:
                    break;
                case HttpVersions.HTTP_1_1_ORDINAL:
                    if(request.getField(HttpHeaders.HOST_BUFFER)==null)
                    {
                        _response.setStatus(HttpStatus.ORDINAL_400_Bad_Request);
                        _out.close();
                        return true;
                    }
                    Buffer expect=request.get(HttpHeaders.EXPECT_BUFFER);
                    if(expect!=null)
                    {
                        if(expect.equals(HttpHeaderValues.CONTINUE_BUFFER))
                        {
                            _out.sendContinue();
                        }
                        else
                        {
                            _response.setStatus(HttpStatus.ORDINAL_417_Expectation_Failed);
                            _out.close();
                            return true;
                        }
                    }
                    break;
                default:
            }
            System.out.println(request.getMethod()+" "+request.getUri());
            _out.setHeadResponse(HttpMethods.HEAD_BUFFER.equals(request.getMethod()));
            if(_listener!=null)
                _listener.customizeRequest(this,_request);
            _handler.handle(_request,_response);
            _in.close();
            _out.close();
            return true;
        }
        finally
        {
            if(_response.getHttpOutputStream().isPersistent())
            {
                _in.resetStream();
                _out.resetStream();
                if(_listener!=null)
                    _listener.persistConnection(this);
            }
            else
                destroy();
        }
    }


    /* ------------------------------------------------------------------------------- */
    /**
     * isThrottled.
     * 
     * @return
     */
    public boolean isThrottled()
    {
        return _throttled;
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * setThrottled.
     * 
     * @param b
     */
    public void setThrottled(boolean throttled)
    {
        this._throttled=throttled;
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * destroy.
     */
    public void destroy()
    {
        if(_request!=null)
            _request.destroy();
        _request=null;
        if(_response!=null)
            _response.destroy();
        _in=null;
        _out=null;
        _response=null;
        _listener=null;
        _handler=null;
        _socket=null;
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * isOpen.
     * 
     * @return
     */
    public boolean isOpen()
    {
        return _out!=null;
    }
}
