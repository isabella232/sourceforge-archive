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
public class HttpConnection
{
    protected HttpInputStream _in;
    protected HttpOutputStream _out;
    private HttpHeader _outHeader;
    
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
    
    public void run()
        throws IOException
    {
        while (true) 
        {
            System.out.println();
            HttpHeader request = _in.readHeader();
            if (request==null)
                break;
            System.out.println(request.getMethod()+" "+request.getUri());
            _out.setHeadResponse(HttpMethods.HEAD_BUFFER.equals(request.getMethod()));
            byte data[] = new byte[4096];
            int length=0;
            int len;
            while((len=_in.read(data,0,4096))>0)
            {
                System.out.println("read "+len+" bytes");
                length+=len;
            }
                
            System.out.println("total "+length+" bytes");
                 
            _outHeader.setVersion(HttpVersions.HTTP_1_1_BUFFER);
            _outHeader.setStatus(200);
            _outHeader.put(HttpHeaders.CONTENT_TYPE_BUFFER,HttpHeaderValues.TEXT_HTML_BUFFER);
            _outHeader.put(HttpHeaders.CONNECTION_BUFFER,request.get(HttpHeaders.CONNECTION_BUFFER));
                
            _out.write("<h1>Test Server</h1>".getBytes());
            _out.write(("<p>Request content read = "+length+"</p>").getBytes());
            _out.write("<h3>Request:</h3><pre>".getBytes());
            _out.write(request.toString().getBytes());
            _out.write("</pre>".getBytes());
            _out.write(("<form method=\"POST\" action=\""+request.getUri()+"\">").getBytes());
            _out.write("<textarea name=\"text\">Test input</textarea>".getBytes());
            _out.write("<br/><input type=\"Submit\"></form>".getBytes());
                
            _out.close();   
                
            _in.resetStream();
            _out.resetStream();
                
            System.out.println("EOR");
        } 
    }
}
