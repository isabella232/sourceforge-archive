// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import org.mortbay.util.Code;
import org.mortbay.util.Test;
import org.mortbay.util.URI;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/* ------------------------------------------------------------ */
/** Test HTTP Request.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class TestRequest
{    
    /* --------------------------------------------------------------- */
    public static HttpRequest getRequest(String data)
        throws IOException
    {
        return getRequest(data.getBytes());
    }
    
    /* --------------------------------------------------------------- */
    public static HttpRequest getRequest(byte[] data)
        throws IOException
    {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpConnection connection = new HttpConnection(null,null,in,out,null);
        
        HttpRequest request = new HttpRequest(connection);
        request.readHeader(connection.getInputStream());
        return request;
    }
    
    /* --------------------------------------------------------------- */
    public static void test()
    {   
        testRequestLine();
        testParameters();
        testMimeTypes();
    }

    /* --------------------------------------------------------------- */
    public static void testRequestLine()
    {
        Test test = new Test("org.mortbay.http.HttpRequest.getRequestLine");

        String[] rl =
        {
            "GET /xxx HTTP/1.0",          "GET", "/xxx",    "HTTP/1.0",
            " GET /xxx HTTP/1.0 ",        "GET", "/xxx",    "HTTP/1.0",
            "  PUT  /xxx  HTTP/1.1  ",    "PUT", "/xxx",    "HTTP/1.1",
            "  GET  /xxx   ",             "GET", "/xxx",    "HTTP/0.9",
            "GET  /xxx",                  "GET", "/xxx",    "HTTP/0.9",
            "  GET  /xxx   ",             "GET", "/xxx",    "HTTP/0.9",
            "GET / ",                     "GET", "/",       "HTTP/0.9",
            "GET /",                      "GET", "/",       "HTTP/0.9",
            "GET http://h:1/ HTTP/1.0",   "GET", "/",       "HTTP/1.0",
            "GET http://h:1/xx HTTP/1.0", "GET", "/xx",     "HTTP/1.0",
            "GET http HTTP/1.0",          "GET", "/http",   "HTTP/1.0",
            "GET http://h:1/",            "GET", "/",       "HTTP/0.9",
            "GET http://h:1/xxx",         "GET", "/xxx",    "HTTP/0.9",
            "  GET     ",                 null,  null,      null,
            "GET",                        null,  null,      null,
            "",                           null,  null,      null,
            "Option * http/1.1  ",        "OPTION", "*",    "HTTP/1.1",
        };

        HttpRequest r = new HttpRequest();
        
        try{
            for (int i=0; i<rl.length ; i+=4)
            {
                try{
                    r.decodeRequestLine(rl[i].toCharArray(),rl[i].length());
                    test.checkEquals(r.getMethod(),rl[i+1],rl[i]);
                    URI uri=r.getURI();
                    test.checkEquals(uri!=null?uri.getPath():null,
                                     rl[i+2],rl[i]);
                    test.checkEquals(r.getVersion(),rl[i+3],rl[i]);
                }
                catch(IOException e)
                {
                    if (rl[i+1]!=null)
                        Code.warning(e);
                    test.check(rl[i+1]==null,rl[i]);
                }
                catch(IllegalArgumentException e)
                {
                    if (rl[i+1]!=null)
                        Code.warning(e);
                    test.check(rl[i+1]==null,rl[i]);
                }
            }
        }
        catch(Exception e)
        {
            test.check(false,e.toString());
            Code.warning("failed",e);
        }
    }
    
    
    /* --------------------------------------------------------------- */
    public static void testParameters()
    {        
        Test t = new Test("org.mortbay.http.HttpRequest.getParameter");
        try
        {
            HttpRequest request=null;


            // No params
            request=getRequest("GET /R1 HTTP/1.0\n"+
                               "Content-Type: text/plain\n"+
                               "Content-Length: 5\n"+
                               "\n"+
                               "123\015\012");
            Code.debug("Request: ",request);
            t.checkEquals(request.getParameterNames().size(),0,"No parameters");
            

            // Query params
            request=getRequest("GET /R1 HTTP/1.0\n"+
                               "Content-Type: text/plain\n"+
                               "Content-Length: 5\n"+
                               "\n"+
                               "123\015\012");
            Code.debug("Request: ",request);
            t.checkEquals(request.getQuery(),null,"No query");
            
            request=getRequest("GET /R1?A=1,2,3&B=4&B=5&B=6 HTTP/1.0\n"+
                               "Content-Type: text/plain\n"+
                               "Content-Length: 5\n"+
                               "\n"+
                               "123\015\012");
            Code.debug("Request: ",request);
            t.checkEquals(request.getParameterNames().size(),2,"Query parameters");
            t.checkEquals(request.getParameter("A"),"1,2,3","Single Query");
            t.checkEquals(request.getParameter("B"),"4","Multi as Single");
            t.checkEquals(request.getParameterValues("A").size(),1,"Single as Multi");
            t.checkEquals(request.getParameterValues("A").get(0),"1,2,3",
                          "Single as Multi");
            t.checkEquals(request.getParameterValues("B").get(0),"4",
                          "Multi query");
            t.checkEquals(request.getParameterValues("B").get(1),"5",
                          "Multi query");
            t.checkEquals(request.getParameterValues("B").get(2),"6",
                          "Multi query");


            // Form params
            request=getRequest("GET /R1 HTTP/1.0\n"+
                               "Content-Type: text/plain\n"+
                               "Content-Length: 15\n"+
                               "\n"+
                               "B=7&C=8&D=9&D=A");
            t.checkEquals(request.getParameterNames().size(),0,"No form, wrong type");

            request=getRequest("GET /R1 HTTP/1.0\n"+
                               "Content-Type: application/x-www-form-urlencoded\n"+
                               "Content-Length: 15\n"+
                               "\n"+
                               "B=7&C=8&D=9&D=A");
            t.checkEquals(request.getParameterNames().size(),0,"No form, GET");
            
            request=getRequest("POST /R1 HTTP/1.0\n"+
                               "Content-Type: application/x-www-form-urlencoded\n"+
                               "Content-Length: 15\n"+
                               "\n"+
                               "B=7&C=8&D=9&D=A");
            t.checkEquals(request.getInputStream().available(),15,"Form not read yet");
            t.checkEquals(request.getParameterNames().size(),3,"Form parameters");
            t.checkEquals(request.getInputStream().available(),0,"Form read");
            t.checkEquals(request.getParameter("B"),"7","Form single param");
            t.checkEquals(request.getParameter("C"),"8","Form single param");
            t.checkEquals(request.getParameterValues("D").size(),2,"Form Multi");
            t.checkEquals(request.getParameterValues("D").get(0),"9",
                          "Form Multi");
            t.checkEquals(request.getParameterValues("D").get(1),"A",
                          "Form Multi");

            // Query and form params
            
            request=getRequest("POST /R1?A=1,2,3&B=4&B=5&B=6 HTTP/1.0\n"+
                               "Content-Type: application/x-www-form-urlencoded\n"+
                               "Content-Length: 15\n"+
                               "\n"+
                               "B=7&C=8&D=9&D=A");
            t.checkEquals(request.getInputStream().available(),15,"Form not read yet");
            t.checkEquals(request.getParameterNames().size(),4,"Form and query params");
            t.checkEquals(request.getInputStream().available(),0,"Form read");

            t.checkEquals(request.getParameter("A"),"1,2,3","Single Query");
            t.checkEquals(request.getParameter("B"),"4","Merge as Single");
            t.checkEquals(request.getParameterValues("B").get(0),"4",
                          "Merged multi");
            t.checkEquals(request.getParameterValues("B").get(1),"5",
                          "Merged multi");
            t.checkEquals(request.getParameterValues("B").get(2),"6",
                          "Merged multi");
            t.checkEquals(request.getParameterValues("B").get(3),"7",
                          "Merged multi");
            t.checkEquals(request.getParameter("C"),"8","Form single param");
            t.checkEquals(request.getParameterValues("D").size(),2,"Form Multi");
            t.checkEquals(request.getParameterValues("D").get(0),"9",
                          "Form Multi");
            t.checkEquals(request.getParameterValues("D").get(1),"A",
                          "Form Multi");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }

    /* --------------------------------------------------------------- */
    public static void testMimeTypes()
    {        
        Test t = new Test("org.mortbay.http.HandlerContext.getMimeByExtension");
        try
        {
            HandlerContext c = new HandlerContext(null,"/");
            c.getMimeMap();
            
            t.checkEquals(c.getMimeByExtension("index.html"),"text/html","index.html");
            t.checkEquals(c.getMimeByExtension("style.css"),"text/css","index.html");
            t.checkEquals(c.getMimeByExtension("doc.pdf"),"application/pdf","index.html");
            t.checkEquals(c.getMimeByExtension("blah/index.html"),"text/html","index.html");
            t.checkEquals(c.getMimeByExtension("blah/style.css"),"text/css","index.html");
            t.checkEquals(c.getMimeByExtension("blah/doc.pdf"),"application/pdf","index.html");
            t.checkEquals(c.getMimeByExtension("blah/my.index.html"),"text/html","index.html");
            t.checkEquals(c.getMimeByExtension("blah/my.style.css"),"text/css","index.html");
            t.checkEquals(c.getMimeByExtension("blah/my.doc.pdf"),"application/pdf","index.html");
            
            t.checkEquals(c.getMimeByExtension("index.HTML"),"text/html","index.html");
            t.checkEquals(c.getMimeByExtension("style.CSS"),"text/css","index.html");
            t.checkEquals(c.getMimeByExtension("doc.PDF"),"application/pdf","index.html");
            t.checkEquals(c.getMimeByExtension("blah/index.htMl"),"text/html","index.html");
            t.checkEquals(c.getMimeByExtension("blah/style.cSs"),"text/css","index.html");
            t.checkEquals(c.getMimeByExtension("blah/doc.pDf"),"application/pdf","index.html");
            t.checkEquals(c.getMimeByExtension("blah/my.index.Html"),"text/html","index.html");
            t.checkEquals(c.getMimeByExtension("blah/my.style.Css"),"text/css","index.html");
            t.checkEquals(c.getMimeByExtension("blah/my.doc.Pdf"),"application/pdf","index.html");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }

    
}
