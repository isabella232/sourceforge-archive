package org.mortbay.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.Buffer;
import org.mortbay.io.stream.InputBuffer;

import junit.framework.TestCase;

/**
 * @author gregw
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class HttpParserTest extends TestCase
{

    /**
     * Constructor for HttpParserTest.
     * @param arg0
     */
    public HttpParserTest(String arg0)
    {
        super(arg0);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HttpParserTest.class);
    }

    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /**
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testLineParse0()
    {
        String http= "POST /foo HTTP/1.0\015\012" + "\015\012";
        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        contentLength= HttpParser.NO_CONTENT;
        HttpParser parser= new Parser();
        parser.parse(buffer);
        assertEquals("POST", f0);
        assertEquals("/foo", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(-1, h);
    }

    public void testLineParse1()
    {
        String http= "GET /999\015\012";
        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        f2= null;
        contentLength= HttpParser.NO_CONTENT;
        HttpParser parser= new Parser();
        parser.parse(buffer);
        assertEquals("GET", f0);
        assertEquals("/999", f1);
        assertEquals(null, f2);
        assertEquals(-1, h);
    }

    public void testLineParse2()
    {
        String http= "POST /222  \015\012";
        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        f2= null;
        contentLength= HttpParser.NO_CONTENT;
        HttpParser parser= new Parser();
        parser.parse(buffer);
        assertEquals("POST", f0);
        assertEquals("/222", f1);
        assertEquals(null, f2);
        assertEquals(-1, h);
    }

    public void testHeaderParse()
    {
        String http=
            "GET / HTTP/1.0\015\012"
                + "Header1: value1\015\012"
                + "Header2  :   value 2a  \015\012"
                + "                    value 2b  \015\012"
                + "Header3: \015\012"
                + "Header4 \015\012"
                + "  value4\015\012"
                + "\015\012";
        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        contentLength= HttpParser.NO_CONTENT;
        HttpParser parser= new Parser();
        parser.parse(buffer);
        assertEquals("GET", f0);
        assertEquals("/", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("Header2", hdr[1]);
        assertEquals("value 2avalue 2b", val[1]);
        assertEquals("Header3", hdr[2]);
        assertEquals(null, val[2]);
        assertEquals("Header4", hdr[3]);
        assertEquals("value4", val[3]);
        assertEquals(3, h);
    }

    public void testChunkParse()
    {
        String http=
            "GET /chunk HTTP/1.0\015\012"
                + "Header1: value1\015\012"
                + "\015\012"
                + "a;\015\012"
                + "0123456789\015\012"
                + "1a\015\012"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                + "0\015\012";
        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        contentLength= HttpParser.CHUNKED_CONTENT;
        HttpParser parser= new Parser();
        parser.parse(buffer);
        assertEquals("GET", f0);
        assertEquals("/chunk", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(0, h);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);
    }

    public void testMultiParse()
    {
        String http=
            "GET /mp HTTP/1.0\015\012"
                + "Header1: value1\015\012"
                + "\015\012"
                + "a;\015\012"
                + "0123456789\015\012"
                + "1a\015\012"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                + "0\015\012"
                + "POST /foo HTTP/1.0\015\012"
                + "Header2: value2\015\012"
                + "\015\012"
                + "PUT /doodle HTTP/1.0\015\012"
                + "Header3: value3\015\012"
                + "\015\012"
                + "0123456789\015\012";

        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        contentLength= HttpParser.CHUNKED_CONTENT;
        HttpParser parser= new Parser();
        parser.parse(buffer);
        assertEquals("GET", f0);
        assertEquals("/mp", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(0, h);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);

        contentLength= HttpParser.NO_CONTENT;
        parser.parse(buffer);
        assertEquals("POST", f0);
        assertEquals("/foo", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(0, h);
        assertEquals("Header2", hdr[0]);
        assertEquals("value2", val[0]);
        assertEquals(null, content);

        contentLength= 10;
        parser.parse(buffer);
        assertEquals("PUT", f0);
        assertEquals("/doodle", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(0, h);
        assertEquals("Header3", hdr[0]);
        assertEquals("value3", val[0]);
        assertEquals("0123456789", content);

    }

    public void testInputStreamParse() throws Exception
    {
        String http=
            "GET / HTTP/1.0\015\012"
                + "Header1: value1\015\012"
                + "\015\012"
                + "a;\015\012"
                + "0123456789\015\012"
                + "1a\015\012"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                + "0\015\012"
                + "POST /foo HTTP/1.0\015\012"
                + "Header2: value2\015\012"
                + "\015\012"
                + "PUT /doodle HTTP/1.0\015\012"
                + "Header3: value3\015\012"
                + "\015\012"
                + "0123456789\015\012";

        File file= File.createTempFile("test", ".txt");
        System.out.println("test file = " + file);
        FileOutputStream out= new FileOutputStream(file);
        out.write(http.getBytes());
        out.flush();
        out.close();

        int[] tests=
            {
                1024,
                http.length() + 3,
                http.length() + 2,
                http.length() + 1,
                http.length() + 0,
                http.length() - 1,
                http.length() - 2,
                http.length() / 2,
                http.length() / 3,
                48,
                16 
            };

        for (int t= 0; t < tests.length; t++)
        {
            System.err.println("buffer size = " + tests[t]);
            InputBuffer buffer= new InputBuffer(new FileInputStream(file), tests[t]);

            contentLength= HttpParser.CHUNKED_CONTENT;
            HttpParser parser= new Parser();
            parser.parse(buffer);
            assertEquals("GET", f0);
            assertEquals("/", f1);
            assertEquals("HTTP/1.0", f2);
            assertEquals(0, h);
            assertEquals("Header1", hdr[0]);
            assertEquals("value1", val[0]);
            assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);

            contentLength= HttpParser.NO_CONTENT;
            parser.parse(buffer);
            assertEquals("POST", f0);
            assertEquals("/foo", f1);
            assertEquals("HTTP/1.0", f2);
            assertEquals(0, h);
            assertEquals("Header2", hdr[0]);
            assertEquals("value2", val[0]);
            assertEquals(null, content);

            contentLength= 10;
            parser.parse(buffer);
            assertEquals("PUT", f0);
            assertEquals("/doodle", f1);
            assertEquals("HTTP/1.0", f2);
            assertEquals(0, h);
            assertEquals("Header3", hdr[0]);
            assertEquals("value3", val[0]);
            assertEquals("0123456789", content);
        }
    }

    String content;
    String f0;
    String f1;
    String f2;
    String[] hdr;
    String[] val;
    int h;
    int contentLength;

    class Parser extends HttpParser
    {
        public void foundContent(int index, Buffer ref)
        {
            if (index == 0)
                content= "";
            content= content.substring(0, index) + ref;
            System.out.println("Content[" + index + "]=" + ref.toDetailString());
        }

        public void foundField0(Buffer ref)
        {
            h= -1;
            hdr= new String[9];
            val= new String[9];
            f0= ref.toString();
            System.out.println("Field0=" + ref);
        }

        public void foundField1(Buffer ref)
        {
            f1= ref.toString();
            System.out.println("Field1=" + ref);
        }

        public void foundField2(Buffer ref)
        {
            f2= ref.toString();
            System.out.println("Field2=" + ref);
        }

        public void foundHttpHeader(Buffer ref)
        {
            hdr[++h]= ref.toString();
            System.out.println("Header=" + ref);
        }

        public void foundHttpValue(Buffer ref)
        {
            if (val[h] == null)
                val[h]= ref.toString();
            else
                val[h] += ref.toString();
            System.out.println("Value='" + ref + "'");
        }

        public int getContentLength()
        {
            return HttpParserTest.this.contentLength;
        }

        public void headerComplete()
        {
            content= null;
            System.out.println("Header Complete");
        }

        public void messageComplete(int contentLength)
        {
            System.out.println("Message Complete: " + contentLength);
        }
    }
}
