/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.mortbay.io.stream.InputStreamBuffer;
import org.mortbay.io.stream.OutputStreamBuffer;


/* ------------------------------------------------------------------------------- */
/**
 * 
 */
public class HttpServerConnection
{

    public static void main(String[] args)
        throws Exception
    {   
        ServerSocket ss = new ServerSocket(8080);
        System.out.println("listening on "+ss);
        while(true)
        {
            Socket socket=ss.accept();
            InputStreamBuffer in = new InputStreamBuffer(socket,4096);
            HttpInputStream input = new HttpInputStream(in);
            OutputStreamBuffer out = new OutputStreamBuffer(socket,4096);
            HttpOutputStream output = new HttpOutputStream(out,1024);
            HttpHeader response=output.getHttpHeader();
            
            while (true) 
            {
                HttpHeader request = input.readHeader();
                if (request==null)
                    break;
                System.err.println(request);
                
                byte data[] = new byte[4096];
                int length=0;
                int len;
                while((len=input.read(data,0,4096))>0)
                    length+=len;
                 
                response.setVersion(HttpVersions.HTTP_1_1_BUFFER);
                response.setStatus(200);
                response.put(HttpHeaders.CONTENT_TYPE_BUFFER,HttpHeaderValues.TEXT_HTML_BUFFER);
                response.put(HttpHeaders.CONNECTION_BUFFER,request.get(HttpHeaders.CONNECTION_BUFFER));
                
                output.write("<h1>Test Server</h1>".getBytes());
                output.write(("<p>Request content read = "+length+"</p>").getBytes());
                output.write("<h3>Request:</h3><pre>".getBytes());
                output.write(request.toString().getBytes());
                output.write("</pre>".getBytes());
                output.close();   
                
                output.reset();
            } 
        }
    }   
    
}
