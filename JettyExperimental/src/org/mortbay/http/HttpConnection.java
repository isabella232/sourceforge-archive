/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import java.io.IOException;
import java.net.Socket;

import org.mortbay.io.Buffer;
import org.mortbay.io.stream.InputStreamBuffer;
import org.mortbay.io.stream.OutputStreamBuffer;


/* ------------------------------------------------------------------------------- */
/**
 * Temporary connection class to get things running.
 * 
 */
public class HttpConnection implements Runnable
{
    protected HttpInputStream _in;
    protected HttpOutputStream _out;
    private HttpHeader _outHeader;
    private Handler _handler;
    
    public HttpConnection(Socket socket)
        throws IOException
    {
        InputStreamBuffer in = new InputStreamBuffer(socket,4096);
        _in = new HttpInputStream(in);
        OutputStreamBuffer out = new OutputStreamBuffer(socket,4096);
        _out = new HttpOutputStream(out,1024);
        _outHeader=_out.getHttpHeader();
    }
    
    public HttpConnection(Buffer in, Buffer out)
    {
        _in = new HttpInputStream(in);
        _out=new HttpOutputStream(out,out.capacity()/4);
        _outHeader=_out.getHttpHeader();
    }
    
    public void setHandler(Handler handler)
    {
        _handler=handler;
    }
    
    public Handler getHandler()
    {
        return _handler;
    }
    
    public void run()
    {
        try
        {
            while (_out.isPersistent())
            {
                if (!runNext())
                    break;
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean runNext() throws IOException
    {
        if (_handler==null)
            _handler=new DumpHandler();
            
        try
        {
            HttpHeader request= _in.readHeader();
            if (request == null)
                return false;

            int version= request.getVersionOrdinal();
            _out.setVersionOrdinal(version);
            _outHeader.setVersion(HttpVersions.HTTP_1_1_BUFFER);
            
            switch (version)
            {
                case HttpVersions.HTTP_0_9_ORDINAL :
                    break;
                case HttpVersions.HTTP_1_0_ORDINAL :
                    break;
                case HttpVersions.HTTP_1_1_ORDINAL :
                    if (request.getField(HttpHeaders.HOST_BUFFER) == null)
                    {
                        _outHeader.setStatus(HttpStatus.ORDINAL_400_Bad_Request);
                        _out.close();
                        return true;
                    }
                    
                    Buffer expect = request.get(HttpHeaders.EXPECT_BUFFER);
                    if (expect!=null)
                    {
                        if (expect.equals(HttpHeaderValues.CONTINUE_BUFFER))
                        {
                            _out.sendContinue();
                        }
                        else
                        {
                            _outHeader.setStatus(HttpStatus.ORDINAL_417_Expectation_Failed);
                            _out.close();
                            return true;
                        }
                    }
                    
                    break;
                default :
            }

            System.out.println(request.getMethod() + " " + request.getUri());
            _out.setHeadResponse(HttpMethods.HEAD_BUFFER.equals(request.getMethod()));

            _handler.handle(request,_outHeader,_in,_out);
            _in.close();
            _out.close();
            return true;
        }
        finally
        {
            _in.resetStream();
            _out.resetStream();
        }
    }
    
    /** A temporary interface for extending the HttpConnection handling */
    public static interface Handler
    {
        public void handle(HttpHeader request, 
                           HttpHeader response,
                           HttpInputStream in,
                           HttpOutputStream out)
            throws IOException;
    }
    
    public static class DumpHandler implements Handler
    {
        public void handle(HttpHeader request, 
                           HttpHeader response,
                           HttpInputStream in, 
                           HttpOutputStream out)
            throws IOException
        {
            StringBuffer content=new StringBuffer();
            byte data[]= new byte[4096];
            int length= 0;
            int len;
            while ((len= in.read(data, 0, 4096)) > 0)
            {
                length += len;
                if (len>0)
                    content.append(new String(data,0,len));
            }
            
            response.setStatus(200);
            response.put(HttpHeaders.CONTENT_TYPE_BUFFER, HttpHeaderValues.TEXT_HTML_BUFFER);
            response.put(
                HttpHeaders.CONNECTION_BUFFER,
                request.get(HttpHeaders.CONNECTION_BUFFER));

            out.write("<html><h1>Test Server</h1>".getBytes());
            out.write(("<p>Request content read = " + length + "</p>").getBytes());
            out.write("<h3>Request:</h3><pre>".getBytes());
            out.write(request.toString().getBytes());
            out.write("</pre>".getBytes());
            out.write(("<form method=\"POST\" action=\"" + request.getUri() + "\">").getBytes());
            out.write("<textarea name=\"text\">Test input</textarea>".getBytes());
            out.write("<br/><input type=\"Submit\"></form>\n".getBytes());

            out.write("<h3>Content:</h3><pre>".getBytes());
            out.write(content.toString().getBytes());
            out.write("</pre></html>\n".getBytes());
            
        }
    }
}
