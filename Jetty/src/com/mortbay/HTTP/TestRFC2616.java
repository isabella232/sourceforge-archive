// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.HTTP.Handler.DumpHandler;
import com.mortbay.HTTP.Handler.NotFoundHandler;
import com.mortbay.HTTP.Handler.NullHandler;
import com.mortbay.HTTP.Handler.ResourceHandler;
import com.mortbay.HTTP.Handler.TestTEHandler;
import com.mortbay.Util.Code;
import com.mortbay.Util.IO;
import com.mortbay.Util.Resource;
import com.mortbay.Util.LineInput;
import com.mortbay.Util.Test;
import com.mortbay.Util.ThreadPool;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.ServerSocket;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/* ------------------------------------------------------------ */
/** Test against RFC 2616.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class TestRFC2616
    extends ThreadPool
    implements HttpListener
{
    private HttpServer _server;
    private static File docRoot = null;
    private static TestFileData [] testFiles = null;


    /* -------------------------------------------------------------- */

    //
    // this inner class creates some files needed for testing of code 
    // involving the ResourceHandler.  instead, it may be cleaner to 
    // subclass the handler or resource context to fetch data from 
    // this code instead of going to disk, but that would involve 
    // hacking up the production code to a much larger degree than
    // is necessary to implement certain desired features (Ranges)
    //
    // so, this class instead creates a "docroot/" directory with 
    // some small files in it.  That way, you still dont have to
    // worry about what your work dir is when you run the tests.
    // drawback: it will overwrite these files if they exist. hopefully
    // nobody will use the same bizarre file names.
    //
    // @author Helmut Hissen (hzh)
    //

    public final static String defaultTestRoot = "testdocs";
    public final static String testFilePrefix = "alphabet";
    public final static String testFileSuffix = ".txt";

    class TestFileData {

          File file;
          String data;
          String name;
          String modDate;
          Resource resource;



          public TestFileData(File file, String data) throws IOException {
               File docRoot = new File(file.getParent());

               this.file = file;
               this.data = data;
               this.name = file.getName();

               if ( !docRoot.exists() ) {
                      docRoot.mkdir();
               }

               FileOutputStream fos = new FileOutputStream(file);
               fos.write(data.getBytes());
               fos.close();
               Code.debug("created " + file.getPath());

               try {
                  this.resource = Resource.newResource(
                              new URL("file", "localhost", file.getAbsolutePath())
                  );
                  this.modDate = HttpFields.__dateSend.format(new Date(resource.lastModified()));
               }
               catch (MalformedURLException mue) {  
                  Code.warning(mue);
               }

          }
    }

    public final static String [] testFileChars = {
        "abcdefghijklmnopqrstuvwxyz",        // PLAIN TEXT ONLY PLEASE
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ"         // OTHERWISE: DEAL WITH TEXT ENCODING
    };

    public TestFileData[] initTestFileData(File docRoot) throws IOException {

        TestFileData[] testFiles = new TestFileData[testFileChars.length];

        for (int i = 0; i < testFileChars.length; i++) {
             testFiles[i] = new TestFileData(
                      new File(docRoot, testFilePrefix + i + testFileSuffix), 
                      testFileChars[i]
             );
        }

	return testFiles;
    }
    
    /* --------------------------------------------------------------- */
    public TestRFC2616()
        throws Exception
    {
	if (testFiles == null) {
		docRoot = new File(defaultTestRoot);
		testFiles = initTestFileData(docRoot);
	}
	
        setName("Test");
        setMinThreads(1);
        setMaxThreads(10);
        setMaxIdleTimeMs(30000);
        _server=new HttpServer();
        HandlerContext context = _server.getContext(null,"/");
	context.setResourceBase(docRoot.getName());
        context.addHandler(new TestTEHandler());
        context.addHandler(new RedirectHandler());
        ResourceHandler rh = new ResourceHandler();
        context.addHandler(rh);   // for testdocs
        rh.setHandleGeneralOptionsQuery(false); // dont handle OPTIONS *
        context.addHandler(new DumpHandler());
        context.addHandler(new NotFoundHandler());
        _server.addListener(this);
        _server.start();
        
    }

    /* --------------------------------------------------------------- */
    public void setHttpServer(HttpServer s)
    {
    }
    
    /* ------------------------------------------------------------ */
    public HttpServer getHttpServer()
    {
        return _server;
    }
    
    /* ------------------------------------------------------------ */
    public ServerSocket getServerSocket()
    {
        return null;
    }
    
    /* --------------------------------------------------------------- */
    public String getDefaultScheme()
    {
        return "jettytest";
    }

    /* --------------------------------------------------------------- */
    public void setHost(String h)
    {
    }
    
    /* --------------------------------------------------------------- */
    public String getHost()
    {
        return "localhost";
    }
    
    /* --------------------------------------------------------------- */
    public void setPort(int p)
    {
    }
    
    /* --------------------------------------------------------------- */
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
        HttpConnection connection = new HttpConnection(this,null,in,out,null);
        connection.handle();
        connection.close();
        return out.toByteArray();
    }
    
    /* --------------------------------------------------------------- */
    public static void test()
    {   
        test3_3();      /* Date/Time Formats                           */
        test3_6();      /* Transfer Encodings                          */
        test3_9();      /* Quality Values                              */
        test4_4();      /* Message Length                              */
        test5_2();
        test8_1();
        test8_2();
        test9_2();
        test9_4();
        test9_8();
	test10_2_7();	/* 206 Partial Content                         */
        test10_3();     /* Redirection 3XX                             */
        test14_16();    /* Content-Range                               */
        test14_35();    /* Byte Ranges                                 */
        test14_39();    /* TE                                          */
        test19_6();     /* Compatibility with Previous Versions        */
    }

    
    /* --------------------------------------------------------------- */
    public static void test3_3()
    {        
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
        Test t = new Test("RFC2616 3.6 Transfer Coding");
        String response=null;
        try
        {
            TestRFC2616 listener = new TestRFC2616();
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

            // Chunked and keep alive
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Transfer-Encoding: chunked\n"+
                                           "Content-Type: text/plain\n"+
                                           "Connection: keep-alive\n"+
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
            offset = t.checkContains(response,offset,"HTTP/1.1 200","gzip in")+10;
            offset = t.checkContains(response,offset,"1234567890","gzip in");

            
            // output gzip
            offset=0;
            byte[] rbytes=listener.getResponses(("GET /R1?gzip HTTP/1.1\n"+
                                                 "Host: localhost\n"+
                                                 "TE: gzip\n" +
                                                 "Connection: close\n"+
                                                 "\n").getBytes());
            Code.debug("RESPONSE: ",new String(rbytes));
            ByteArrayInputStream bin = new ByteArrayInputStream(rbytes);
            ChunkableInputStream cin = new ChunkableInputStream(bin);
            HttpFields header = new HttpFields();
            header.read((LineInput)cin.getRawStream());
            Code.debug("HEADER:\n",header);
            cin.setChunking();
            GZIPInputStream gin = new GZIPInputStream(cin);
            ByteArrayOutputStream bout3 = new ByteArrayOutputStream();
            IO.copy(gin,bout3);
            response=new String(bout3.toByteArray());
            t.checkContains(response,"<H3>","gzip out");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
            if (response!=null)
                Code.warning(response);
        }
    }
   
    
    /* --------------------------------------------------------------- */
    public static void test3_9()
    {        
        Test t = new Test("RFC2616 3.9 Quality");
        try
        {
            HttpFields fields = new HttpFields();

            fields.put("Q","bbb;q=0.5,aaa,ccc;q=0.001,d;q=0,e;q=0.0001");
            List list = fields.getValues("Q",", \t");
            list=HttpFields.qualityList(list);
            t.checkEquals(HttpFields.valueParameters(list.get(0).toString(),null),
                          "aaa","Quality parameters");
            t.checkEquals(HttpFields.valueParameters(list.get(1).toString(),null),
                          "bbb","Quality parameters");
            t.checkEquals(HttpFields.valueParameters(list.get(2).toString(),null),
                          "ccc","Quality parameters");
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
                                   "HTTP/1.1 200 OK","3. ignore c-l")+1;
            offset=t.checkContains(response,offset,
                                   "/R1","3. ignore c-l")+1;
            offset=t.checkContains(response,offset,
                                   "123456","3. ignore c-l")+1;
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200 OK","3. ignore c-l")+1;
            offset=t.checkContains(response,offset,
                                   "/R2","3. content-length")+1;
            offset=t.checkContains(response,offset,
                                   "123456","3. content-length")+1;
            
            // No content length
            t.check(true,"Skip 411 checks as IE breaks this rule");
//              offset=0;
//              response=listener.getResponses("GET /R2 HTTP/1.1\n"+
//                                             "Host: localhost\n"+
//                                             "Content-Type: text/plain\n"+
//                                             "Connection: close\n"+
//                                             "\n"+
//                                             "123456");
//              offset=t.checkContains(response,offset,
//                                     "HTTP/1.1 411 ","411 length required")+10;
//              offset=0;
//              response=listener.getResponses("GET /R2 HTTP/1.0\n"+
//                                             "Content-Type: text/plain\n"+
//                                             "\n"+
//                                             "123456");
//              offset=t.checkContains(response,offset,
//                                     "HTTP/1.0 411 ","411 length required")+10;
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void test5_2()
    {        
        Test t = new Test("RFC2616 5.2 Virtual Hosts");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            listener.getHttpServer().getContext("VirtualHost",
                                                "/path/*")
                .addHandler(new DumpHandler());
            listener.getHttpServer().start();
            String response;
            int offset=0;

            // Default Host
            offset=0;
            response=listener.getResponses("GET /path/R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "\n");
            
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200",
                                   "Default host")+1;
            offset=t.checkContains(response,offset,
                                   "contextPath=",
                                   "Default host")+1;
            offset=t.checkContains(response,offset,
                                   "pathInContext=/path/R1",
                                   "Default host")+1;
            
            // Virtual Host
            offset=0;
            response=listener.getResponses("GET http://VirtualHost/path/R1 HTTP/1.1\n"+
                                           "Host: ignored\n"+
                                           "\n");
            
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200",
                                   "1. virtual host uri")+1;
            offset=t.checkContains(response,offset,
                                   "contextPath=/path",
                                   "1. virtual host uri")+1;
            offset=t.checkContains(response,offset,
                                   "pathInContext=/R1",
                                   "1. virtual host uri")+1;

            // Virtual Host
            offset=0;
            response=listener.getResponses("GET /path/R1 HTTP/1.1\n"+
                                           "Host: VirtualHost\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200",
                                   "2. virtual host field")+1;
            offset=t.checkContains(response,offset,
                                   "contextPath=/path",
                                   "2. virtual host field")+1;
            offset=t.checkContains(response,offset,
                                   "pathInContext=/R1",
                                   "2. virtual host field")+1;

            // Virtual Host
            offset=0;
            response=listener.getResponses("GET /path/R1 HTTP/1.1\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 400","3. no host")+1;            
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
        Test t = new Test("RFC2616 8.1 Persistent");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;

            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200 OK\015\012","8.1.2 default")+10;
            
            t.checkContains(response,offset,
                            "Content-Length: ","8.1.2 default");

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
        Test t = new Test("RFC2616 8.2 Transmission");
        try
        {
            // Suppress EOF warnings. Premature EOF used to trigger
            // sending of 100-Continue.
            // This is not a very good way of doingit, but ...
            Code.setSuppressWarnings(!Code.debug());
            
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
            t.checkEquals(response.indexOf("HTTP/1.1 100"),-1,
                          "8.2.3 no expect no 100");
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200","8.2.3 no expect no 100")+1;

            
            // Expect with body
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Expect: 100-continue\n"+
                                           "Content-Type: text/plain\n"+
                                           "Content-Length: 8\n"+
                                           "Connection: close\n"+
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
                                   "HTTP/1.1 200","8.2.3 expect 100")+1;
            
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
                                   "HTTP/1.1 404","8.2.3 RFC2068")+1;
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
                                   "HTTP/1.1 200","8.2.3 RFC2068")+1;
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
        finally{
            Code.setSuppressWarnings(Code.debug());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void test9_2()
    {        
        Test t = new Test("RFC2616 9.2 OPTIONS");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;

            // Default Host
            offset=0;
            response=listener.getResponses("OPTIONS * HTTP/1.1\n"+
                                           "Connection: close\n"+
                                           "Host: localhost\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200","200")+1;
            offset=t.checkContains(response,offset,
                                   "Allow: GET, HEAD, POST, PUT, DELETE, MOVE, OPTIONS, TRACE","Allow")+1;
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void test9_4()
    {        
        Test t = new Test("RFC2616 9.4 HEAD");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String get;
            String head;

            // Default Host
            get=listener.getResponses("GET /R1 HTTP/1.0\n"+
                                      "Host: localhost\n"+
                                      "\n");
            head=listener.getResponses("HEAD /R1 HTTP/1.0\n"+
                                       "Host: localhost\n"+
                                       "\n");
            
            Code.debug("GET: ",get);
            Code.debug("HEAD: ",head);
            t.checkContains(get,0,"HTTP/1.0 200","GET");
            t.checkContains(get,0,"Content-Type: text/html","GET content");
            t.checkContains(get,0,"Content-Length: ","GET length");
            t.checkContains(head,0,"HTTP/1.0 200","HEAD");
            t.checkContains(head,0,"Content-Type: text/html","HEAD content");
            t.checkContains(head,0,"Content-Length: ","HEAD length");
            t.checkContains(get,0,"<HTML>","GET body");
            t.checkEquals(head.indexOf("<HTML>"),-1,"HEAD no body");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void test9_8()
    {        
        Test t = new Test("RFC2616 9.8 TRACE");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;

            // Default Host
            offset=0;
            response=listener.getResponses("TRACE /path HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200","200")+1;
            offset=t.checkContains(response,offset,
                                   "Content-Type: message/http",
                                   "message/http")+1;
            offset=t.checkContains(response,offset,
                                   "TRACE /path HTTP/1.1\r\n"+
                                   "Host: localhost\r\n",
                                   "Request");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void test10_2_7()
    {        
        Test t = new Test("RFC2616 10.2.7 206 Partial Content");

        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;

            // check to see if corresponging GET w/o range would return 
            //   a) ETag
            //   b) Content-Location
            // these same headers will be required for corresponding 
            // sub range requests 

            response=listener.getResponses("GET /" + listener.testFiles[0].name + " HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);

            boolean noRangeHasContentLocation = (response.indexOf("\r\nContent-Location: ") != -1);


            // now try again for the same resource but this time WITH range header

            response=listener.getResponses("GET /" + listener.testFiles[0].name + " HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "Range: bytes=1-3\n"+
                                           "\n");

            Code.debug("RESPONSE: ",response);
            offset=0;
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 206 Partial Content\r\n",
                                   "1. proper 206 status code");
            offset=t.checkContains(response,offset, 
                                   "Content-Type: text/plain",
                                   "2. content type") + 2;
            offset=t.checkContains(response,offset,
                                   "Last-Modified: " + listener.testFiles[0].modDate + "\r\n", 
                                   "3. correct resource mod date");

            // if GET w/o range had Content-Location, then the corresponding 
            // response for the a GET w/ range must also have that same header

            offset=t.checkContains(response,offset, 
                                   "Content-Range: bytes 1-3/26",
                                   "4. content range") + 2;

            if (noRangeHasContentLocation) {
                    offset=t.checkContains(response,offset, 
                                  "Content-Location: ", 
                                  "5. Content-Location header as with 200");
            } 
            else {
                    Code.debug("no need to check for Conten-Location header in 206 response");
                    // spec does not require existence or absence if these want any
                    // header for the get w/o range
            }

            String expectedData = listener.testFiles[0].data.substring(1, 3+1);
            offset=t.checkContains(response,offset, 
                                   expectedData, 
                                   "6. subrange data: \"" + expectedData + "\"");
	}
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    } 

    
    /* --------------------------------------------------------------- */
    public static void test10_3()
    {        
        Test t = new Test("RFC2616 10.3 redirection");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;

            // HTTP/1.0
            offset=0;
            response=listener.getResponses("GET /redirect HTTP/1.0\n"+
                                           "Connection: Keep-Alive\n"+
                                           "\n"+
                                           "GET /redirect HTTP/1.0\n"+
                                           "\n"
                                           );
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.0 302","302")+1;
            t.checkContains(response,offset,
                            "Location: /dump",
                            "redirected");
            t.checkContains(response,offset,
                            "Content-Length: 0",
                            "content length");
            t.checkContains(response,offset,
                            "Connection: keep-alive",
                            "keep-alive");
            
            offset=t.checkContains(response,offset,
                                   "HTTP/1.0 302","302")+1;
            t.checkContains(response,offset,
                            "Location: /dump",
                            "redirected");
            t.checkContains(response,offset,
                            "Connection: close",
                            "closed");

            
            // HTTP/1.1
            offset=0;
            response=listener.getResponses("GET /redirect HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "\n"+
                                           "GET /redirect HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n"
                                           );
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 302","302")+1;
            t.checkContains(response,offset,
                            "Location: /dump",
                            "redirected");
            t.checkContains(response,offset,
                            "Content-Length: 0",
                            "content length");
            
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 302","302")+1;
            t.checkContains(response,offset,
                            "Location: /dump",
                            "redirected");
            t.checkContains(response,offset,
                            "Connection: close",
                            "closed");
            
            // HTTP/1.0 content
            offset=0;
            response=listener.getResponses("GET /redirect/content HTTP/1.0\n"+
                                           "Connection: Keep-Alive\n"+
                                           "\n"+
                                           "GET /redirect/content HTTP/1.0\n"+
                                           "\n"
                                           );
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.0 302","302")+1;
            t.checkContains(response,offset,
                            "Location: /dump",
                            "redirected");
            t.checkContains(response,offset,
                            "Connection: close",
                            "close no content length");
            
            // HTTP/1.1 content
            offset=0;
            response=listener.getResponses("GET /redirect/content HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "\n"+
                                           "GET /redirect/content HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n"
                                           );
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 302","302")+1;
            t.checkContains(response,offset,
                            "Location: /dump",
                            "redirected");
            t.checkContains(response,offset,
                            "Transfer-Encoding: chunked",
                            "chunked content length");
            
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 302","302")+1;
            t.checkContains(response,offset,
                            "Location: /dump",
                            "redirected");
            t.checkContains(response,offset,
                            "Connection: close",
                            "closed");
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    

    /* --------------------------------------------------------------- */

    public void checkContentRange( Test t, 
                     String tname,
                     String path, 
                     String reqRanges,
                     int expectedStatus,
                     String expectedRange,
                     String expectedData) {

        try {
            String response;
            int offset=0;

            String byteRangeHeader = "";
            if (reqRanges != null) {
                 byteRangeHeader = "Range: " + reqRanges + "\n";
            }

            response=getResponses("GET /" + path + " HTTP/1.1\n"+
                                    "Host: localhost\n"+
                                    byteRangeHeader +
                                    "Connection: close\n"+
                                    "\n");

            switch (expectedStatus) {
                case 200 : {
                       offset=t.checkContains(response,offset,
                                  "HTTP/1.1 200 OK\r\n",
                                  tname + ".1. proper 200 OK status code");
                       break;
                }
                case 206 : {
                       offset=t.checkContains(response,offset,
                                  "HTTP/1.1 206 Partial Content\r\n",
                                  tname + ".1. proper 206 Partial Content status code");
                       break;
                }
                case 416 : {
                       offset=t.checkContains(response,offset,
                                              "HTTP/1.1 416 Requested Range Not Satisfiable\r\n",
                                              tname + ".1. proper 416 Requested Range not Satisfiable status code");
                       break;
                }
            }

            if (expectedRange != null)
            {
                String expectedContentRange = "Content-Range: bytes " + expectedRange + "\r\n"; 
                offset=t.checkContains(response,offset, 
                                  expectedContentRange,
                                  tname + ".2. content range " + expectedRange);
            }

            if (expectedStatus == 200 || expectedStatus == 206)
            {
                offset=t.checkContains(response,offset,expectedData,tname +
                                       ".3. subrange data: \"" + expectedData + "\"");
            }
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }


    public static void test14_16()
    {        
        Test t = new Test("RFC2616 14.16 Conent-Range");
        try {
          TestRFC2616 listener = new TestRFC2616();

          int id = 0;


          //
          // calibrate with normal request (no ranges); if this doesnt
          // work, dont expect ranges to work either
          //
          listener.checkContentRange( t, 
                     Integer.toString(id++),
                     listener.testFiles[0].name,
                     null,
                     200,
                     null,
                     listener.testFiles[0].data 
          );


          //
          // server should ignore all range headers which include
          // at least one syntactically invalid range 
          //
          String [] totallyBadRanges = {
                     "bytes=a-b",
                     "bytes=-1-2",
                     "bytes=-1-2,2-3",
                     "bytes=-",
                     "bytes=-1-",
                     "bytes=a-b,-1-1-1",
                     "bytes=",
                     "doublehalfwords=1-2",
          };

          for (int i = 0; i < totallyBadRanges.length; i++) {
             listener.checkContentRange( t, 
                     "BadRange"+i,
                     listener.testFiles[0].name,
                     totallyBadRanges[i],
                     416,
                     null,
                     listener.testFiles[0].data 
             );
          }


          //
          // should test for combinations of good and syntactically
          // invalid ranges here, but I am not certain what the right
          // behavior is abymore
          //
          // a) Range: bytes=a-b,5-8
          //
          // b) Range: bytes=a-b,bytes=5-8
          //
          // c) Range: bytes=a-b
          //    Range: bytes=5-8
          //


          //
          // return data for valid ranges while ignoring unsatisfiable
          // ranges
          //

          listener.checkContentRange( t, 
                     "bytes=5-8",
                     listener.testFiles[0].name,
                     "bytes=5-8",
                     206,
                     "5-8/26",
                     listener.testFiles[0].data.substring(5,8+1) 
                                      );
          
          // 
          // server should not return a 416 if at least syntactically valid ranges
          // are is satisfiable
          //
          listener.checkContentRange( t, 
                                      "bytes=5-8,50-60",
                                      listener.testFiles[0].name,
                                      "bytes=5-8,50-60",
                                      206,
                                      "5-8/26",
                                      listener.testFiles[0].data.substring(5,8+1) 
          );
          listener.checkContentRange( t, 
                     "bytes=50-60,5-8",
                     listener.testFiles[0].name,
                     "bytes=50-60,5-8",
                     206,
                     "5-8/26",
                     listener.testFiles[0].data.substring(5,8+1) 
          );

          // 416 as none are satisfiable
          listener.checkContentRange( t, 
                     "bytes=50-60",
                     listener.testFiles[0].name,
                     "bytes=50-60",
                     416,
                     "*/26",
                     null
                                      );
          

        }

        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
      } 


    /* --------------------------------------------------------------- */
    public static void test14_35()
    {        
        Test t = new Test("RFC2616 14.35 Byte Ranges");
        try {
          TestRFC2616 listener = new TestRFC2616();

          int id = 0;


          //
          // test various valid range specs that have not been 
          // tested yet
          //

          listener.checkContentRange( t, 
                     "bytes=0-2",
                     listener.testFiles[0].name,
                     "bytes=0-2",
                     206,
                     "0-2/26",
                     listener.testFiles[0].data.substring(0,2+1) 
          );

          listener.checkContentRange( t, 
                     "bytes=23-",
                     listener.testFiles[0].name,
                     "bytes=23-",
                     206,
                     "23-25/26",
                     listener.testFiles[0].data.substring(23,25+1) 
          );

          listener.checkContentRange( t, 
                     "bytes=23-42",
                     listener.testFiles[0].name,
                     "bytes=23-42",
                     206,
                     "23-25/26",
                     listener.testFiles[0].data.substring(23,25+1) 
          );

          listener.checkContentRange( t, 
                     "bytes=-3",
                     listener.testFiles[0].name,
                     "bytes=-3",
                     206,
                     "23-25/26",
                     listener.testFiles[0].data.substring(23,25+1) 
          );

          listener.checkContentRange( t, 
                     "bytes=23-23,-2",
                     listener.testFiles[0].name,
                     "bytes=23-23,-2",
                     206,
                     "23-23/26",
                     listener.testFiles[0].data.substring(23,23+1) 
          );

          listener.checkContentRange( t, 
                     "bytes=-1,-2,-3",
                     listener.testFiles[0].name,
                     "bytes=-1,-2,-3",
                     206,
                     "25-25/26",
                     listener.testFiles[0].data.substring(25,25+1) 
          );

        }

        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    } 

    
    /* --------------------------------------------------------------- */
    public static void test14_39()
    {        
        Test t = new Test("RFC2616 14.39 TE");
        try
        {
            TestRFC2616 listener = new TestRFC2616();
            String response;
            int offset=0;

            // Gzip accepted
            offset=0;
            response=listener.getResponses("GET /R1?gzip HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "TE: gzip;q=0.5\n"+
                                           "Connection: close\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200","TE: coding")+1;
            offset=t.checkContains(response,offset,
                                   "Transfer-Encoding: gzip,chunked","TE: coding")+1;

            // Gzip not accepted
            offset=0;
            response=listener.getResponses("GET /R1?gzip HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "TE: deflate\n"+
                                           "Connection: close\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 501","TE: coding not accepted")+1;

            // trailer field
            offset=0;
            listener._server.setChunkingForced(true);
            response=listener.getResponses("GET /R1 HTTP/1.1\n"+
                                           "Host: localhost\n"+
                                           "TE: trailer\n"+
                                           "Connection: close\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.1 200","TE: trailer")+1;
            offset=t.checkContains(response,offset,
                                   "TestTrailer: Value","TE: trailer")+1;
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void test19_6()
    {        
        Test t = new Test("RFC2616 19.6 Keep-Alive");
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
                                   "HTTP/1.0 200 OK\015\012","19.6.2 default close")+10;
            t.checkNotContained(response,offset,
                                "Connection: close","19.6.2 not assumed");
            
            offset=0;
            response=listener.getResponses("GET /R1 HTTP/1.0\n"+
                                           "Host: localhost\n"+
                                           "Connection: keep-alive\n"+
                                           "\n"+
                                           
                                           "GET /R2 HTTP/1.0\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n"+

                                           "GET /R3 HTTP/1.0\n"+
                                           "Host: localhost\n"+
                                           "Connection: close\n"+
                                           "\n");
            Code.debug("RESPONSE: ",response);
            offset=t.checkContains(response,offset,
                                   "HTTP/1.0 200 OK\015\012","19.6.2 Keep-alive 1")+1;
            offset=t.checkContains(response,offset,
                                   "Connection: keep-alive",
                                   "19.6.2 Keep-alive 1")+1;
            
            offset=t.checkContains(response,offset,
                                   "<HTML>",
                                   "19.6.2 Keep-alive 1")+1;
            
            offset=t.checkContains(response,offset,
                                   "/R1","19.6.2 Keep-alive 1")+1;
            
            offset=t.checkContains(response,offset,
                                   "HTTP/1.0 200 OK\015\012","19.6.2 Keep-alive 2")+11;
            offset=t.checkContains(response,offset,
                                   "Connection: close","19.6.2 Keep-alive close")+1;
            offset=t.checkContains(response,offset,
                                   "/R2","19.6.2 Keep-alive close")+3;
            
            t.checkEquals(response.indexOf("/R3"),-1,"19.6.2 closed");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    public void customizeRequest(HttpConnection connection,
                                       HttpRequest request)
    {
    }
    
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
        try{
            TestRFC2616.test();
        }
        catch(Throwable e)
        {
            Code.warning(e);
            new Test("com.mortbay.HTTP.TestRFC2616").check(false,e.toString());
        }
        finally
        {
            Test.report();
        }
    }


    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public class RedirectHandler extends NullHandler
    {
        /* ------------------------------------------------------------ */
        public void handle(String pathInContext,
                           HttpRequest request,
                           HttpResponse response)
            throws HttpException, IOException
        {
            if (!super.isStarted())
                return;        
            
            // For testing set transfer encodings
            if (request.getPath().startsWith("/redirect"))
            {
                if (request.getPath().startsWith("/redirect/content"))
                    response.getOutputStream().write("Content".getBytes());
                response.sendRedirect("/dump");
                request.setHandled(true);
            }
        }
    }
}
