// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class TestRFC2616 extends HttpListener
{
    public TestRFC2616()
        throws IOException
    {
        super(new HttpServer(),"Test",1,10,30000);
    }

    public String getDefaultProtocol()
    {
        return "jettytest";
    }

    public String getHost()
    {
        return "localhost";
    }
    
    public int getPort()
    {
        return 0;
    }

    /* --------------------------------------------------------------- */
    public String getResponses(String request)
        throws IOException
    {
        return new String(getResponses(request.getBytes()));
    }
    
    /* --------------------------------------------------------------- */
    public byte[] getResponses(byte[] request)
        throws IOException
    {
        ByteArrayInputStream in = new ByteArrayInputStream(request);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TestConnection connection = new TestConnection(in,out);
        connection.handle();
        connection.close();
        return out.toByteArray();
    }
    
    /* --------------------------------------------------------------- */
    class TestConnection extends HttpConnection
    {
        TestConnection(InputStream in,OutputStream out)
        {
            super(TestRFC2616.this,in,out);
        }    
    }

    
    /* --------------------------------------------------------------- */
    public static void test()
    {   
        test3_3();
        test3_6();
        test4_4();
        test8_1();
        test8_2();
    }

    
    /* --------------------------------------------------------------- */
    public static void test3_3()
    {        
        // RANDOM NONE RIGOROUS TESTS!!!
        Test t = new Test("RFC2616 3.3 Date/Time");
        try
        {
            HttpFields fields = new HttpFields();

            fields.put("D1","Sun, 06 Nov 1994 08:49:37 GMT");
            fields.put("D2","Sunday, 06-Nov-94 08:49:37 GMT");
            fields.put("D3","Sun Nov  6 08:49:37 1994");
            Date d1 = new Date(fields.getDateField("D1"));
            Date d2 = new Date(fields.getDateField("D2"));
            Date d3 = new Date(fields.getDateField("D3"));

            t.checkEquals(d1,d2,"3.3.1 RFC 822 RFC 850");
            t.checkEquals(d2,d3,"3.3.1 RFC 850 ANSI C");

            fields.putDateField("Date",d1);
            t.checkEquals(fields.get("Date"),
                          "Sun, 06 Nov 1994 08:49:37 GMT",
                          "3.3.1 RFC 822 preferred");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }    
    }

    /* --------------------------------------------------------------- */
    public static void test3_6()
    {        
        // RANDOM NONE RIGOROUS TESTS!!!
        Test t = new Test("RFC2616 3.6 Transfer Coding");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;

            // Chunk once
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked,chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "123\015\012\015\012"+
                                           "0;\015\012\015\012");
            Code.debug("RESPONSE: ",response);
            t.checkContains(response,"HTTP/1.1 400 Bad","Chunked once");

            // Chunk last
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked,identity\n"+
                                           "Content-Type: text/plain\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "123\015\012\015\012"+
                                           "0;\015\012\015\012");
            Code.debug("RESPONSE: ",response);
            t.checkContains(response,"HTTP/1.1 400 Bad","Chunked last");
            
            // Unknown encoding
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: xxx,chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "\015\012"+
                                           "5;\015\012"+
                                           "123\015\012\015\012"+
                                           "0;\015\012\015\012");
            Code.debug("RESPONSE: ",response);
            t.checkContains(response,"HTTP/1.1 501","Unknown encoding");

            // Chunked
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "\n"+
                                           "3;\n"+
                                           "123\n"+
                                           "3;\n"+
                                           "456\n"+
                                           "0;\n\n"+
                                           
                                           "GET /R2 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset = t.checkContains(response,offset,"HTTP/1.1 200","3.6.1 Chunking")+10;
            offset = t.checkContains(response,offset,"123456","3.6.1 Chunking");
            offset = t.checkContains(response,offset,"/R2","3.6.1 Chunking")+10;


            // gzip encoding
            offset=0;
            ByteArrayOutputStream bout1 = new ByteArrayOutputStream();
            bout1.write(("GET /R1 HTTP/1.1\n"+
                        "Host: localhost\n"+
                        "Transfer-Encoding: gzip,chunked\n"+
                        "Content-Type: text/plain\n"+
                        "\n").getBytes());
            ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
            GZIPOutputStream gout=new GZIPOutputStream(bout2);
            gout.write("1234567890".getBytes());
            gout.flush();
            gout.close();
            byte[] gzip_content=bout2.toByteArray();
            bout1.write("3;\n".getBytes());
            for (int i=0;i<3;i++)
                bout1.write(gzip_content[i]);
            bout1.write(("\n"+(gzip_content.length-3)+";\n").getBytes());
            for (int i=3;i<gzip_content.length;i++)
                bout1.write(gzip_content[i]);
            bout1.write(("\n"+
                         "0;\n\n"+
                         
                         "GET /R2 HTTP/1.1\n"+
                         "Host: localhost\n"+
                         "Connection: close\n"+
                         "\n").getBytes());
            
            response=new String(listener.getResponses(bout1.toByteArray()));
            Code.debug("RESPONSE: ",response);
            offset = t.checkContains(response,offset,"HTTP/1.1 200","gzip")+10;
            offset = t.checkContains(response,offset,"1234567890","gzip");
            
            t.check(true,"XXX TRAILERS & TE not tested here");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void test4_4()
    {        
        // RANDOM NONE RIGOROUS TESTS!!!
        Test t = new Test("RFC2616 4.4 Message Length");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;


            // 2
            // If content length not used, second request will not be read.
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: identity\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 5\n"+
                                           "\n"+
                                           "123\015\012"+
                                           
                                           "GET /R2 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200 OK","2. identity")+10;
            offset=t.checkContains(response,offset,
                                   "/R1","2. identity")+3;
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200 OK","2. identity")+10;
            offset=t.checkContains(response,offset,
                                   "/R2","2. identity")+3;

            // 3
            // content length is ignored, as chunking is used.  If it is
            // not ignored, the second request wont be seen.
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 100\n"+
                                           "\n"+
                                           "3;\n"+
                                           "123\n"+
                                           "3;\n"+
                                           "456\n"+
                                           "0;\n"+
                                           "\n"+
                                           
                                           "GET /R2 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 6\n"+
                                           "\n"+
                                           "123456");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200 OK","3. ignore c-l")+10;
            offset=t.checkContains(response,offset,
                                   "/R1","3. ignore c-l")+3;
            offset=t.checkContains(response,offset,
                                   "123456","3. ignore c-l")+6;
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200 OK","3. ignore c-l")+10;
            offset=t.checkContains(response,offset,
                                   "/R2","3. content-length")+3;
            offset=t.checkContains(response,offset,
                                   "123456","3. content-length")+6;

            // No content length
            offset=0;
            response=listener.getResponses("GET /R2 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Content-Type: text/plain\n"+
                                           "Connection: close\n"+
                                           "\n"+
                                           "123456");
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 411 ","411 length required")+10;
            offset=0;
            response=listener.getResponses("GET /R2 HTTP/1.0\n"+
                                           "Content-Type: text/plain\n"+
                                           "\n"+
                                           "123456");
            offset=t.checkContains(response,offset,
                                   "HTTP/1.0 411 ","411 length required")+10;
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void test8_1()
    {        
        // RANDOM NONE RIGOROUS TESTS!!!
        Test t = new Test("RFC2616 8.1 Persistent");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;

            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.0\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.0 200 OK\015\012","8.1.2 default")+10;
            offset=t.checkContains(response,offset,
                                   "Connection: close","8.1.2 default")+3;
            
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "\n"+
                                           
                                           "GET /R2 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n"+

                                           "GET /R3 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200 OK\015\012","8.1.2 default")+1;
            offset=t.checkContains(response,offset,
                                   "/R1","8.1.2 default")+1;
            
            t.checkEquals(response.indexOf("/R3"),-1,"8.1.2.1 close");
            
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200 OK\015\012","8.1.2.2 pipeline")+11;
            offset=t.checkContains(response,offset,
                                   "Connection: close","8.1.2.2 pipeline")+1;
            offset=t.checkContains(response,offset,
                                   "/R2","8.1.2.1 close")+3;
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void test8_2()
    {        
        // RANDOM NONE RIGOROUS TESTS!!!
        Test t = new Test("RFC2616 8.2 Transmission");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;

            // Expect Failure
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Expect: unknown\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 8\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 417","8.2.3 expect failure")+1;

            // No Expect
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 8\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 400 Bad","8.2.3 no expect no 100")+1;
            t.checkEquals(response.indexOf("HTTP/1.1 100"),-1,
                          "8.2.3 no expect no 100");

            // Expect with body
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Expect: 100-continue\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 8\n"+
                                           "\n"+
                                           "123456\015\012");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200 OK","8.2.3 expect with body")+1;
            t.checkEquals(response.indexOf("HTTP/1.1 100"),-1,
                          "8.2.3 expect with body");
            
            // Expect 100
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Expect: 100-continue\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 8\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 100 Continue","8.2.3 expect 100")+1;
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 400 Bad","8.2.3 expect 100")+1;
            
            // No Expect PUT
            offset=0;
            response=listener.getResponses("PUT /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 8\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 100 Continue","8.2.3 RFC2068")+1;
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 400 Bad","8.2.3 RFC2068")+1;
            // No Expect PUT
            offset=0;
            response=listener.getResponses("POST /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 8\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 100 Continue","8.2.3 RFC2068")+1;
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 400 Bad","8.2.3 RFC2068")+1;
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    /* --------------------------------------------------------------- */
    public static void testX_X()
    {        
        // RANDOM NONE RIGOROUS TESTS!!!
        Test t = new Test("RFC2616 X.X");
        try
        {
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
};




