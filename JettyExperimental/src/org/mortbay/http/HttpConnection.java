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
import org.mortbay.util.StringUtil;


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
    
    public void run() throws IOException
    {
        while (_out.isPersistent())
        {
            if (!runNext())
                break;
        }
    }

    public boolean runNext() throws IOException
    {
        try
        {
            System.out.println();
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

            StringBuffer content=new StringBuffer();
            byte data[]= new byte[4096];
            int length= 0;
            int len;
            while ((len= _in.read(data, 0, 4096)) > 0)
            {
                System.out.println("read " + len + " bytes");
                length += len;
                if (len>0)
                    content.append(new String(data,0,len));
            }

            System.out.println("total " + length + " bytes");

            _outHeader.setStatus(200);
            _outHeader.put(HttpHeaders.CONTENT_TYPE_BUFFER, HttpHeaderValues.TEXT_HTML_BUFFER);
            _outHeader.put(
                HttpHeaders.CONNECTION_BUFFER,
                request.get(HttpHeaders.CONNECTION_BUFFER));

            _out.write("<html><h1>Test Server</h1>".getBytes());
            _out.write(("<p>Request content read = " + length + "</p>").getBytes());
            _out.write("<h3>Request:</h3><pre>".getBytes());
            _out.write(request.toString().getBytes());
            _out.write("</pre>".getBytes());
            _out.write(("<form method=\"POST\" action=\"" + request.getUri() + "\">").getBytes());
            _out.write("<textarea name=\"text\">Test input</textarea>".getBytes());
            _out.write("<br/><input type=\"Submit\"></form>\n".getBytes());

            _out.write("<h3>Content:</h3><pre>".getBytes());
            _out.write(content.toString().getBytes());
            _out.write("</pre></html>\n".getBytes());
            
            System.out.println("EOR");
            _out.close();
            return true;
        }
        finally
        {
            _in.resetStream();
            _out.resetStream();
        }
    }
}
