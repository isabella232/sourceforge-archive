package org.mortbay.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import junit.framework.TestCase;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.stream.InputStreamBuffer;

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
	throws Exception
    {
        String http= "POST /foo HTTP/1.0\015\012" + "\015\012";
        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        HttpParser parser= new Parser(buffer);
        parser.parse();
        assertEquals("POST", f0);
        assertEquals("/foo", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(-1, h);
    }

    public void testLineParse1()
	throws Exception
    {
        String http= "GET /999\015\012";
        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        f2= null;
        HttpParser parser= new Parser(buffer);
        parser.parse();
        assertEquals("GET", f0);
        assertEquals("/999", f1);
        assertEquals(null, f2);
        assertEquals(-1, h);
    }

    public void testLineParse2()
	throws Exception
    {
        String http= "POST /222  \015\012";
        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        f2= null;
        HttpParser parser= new Parser(buffer);
        parser.parse();
        assertEquals("POST", f0);
        assertEquals("/222", f1);
        assertEquals(null, f2);
        assertEquals(-1, h);
    }

    public void testHeaderParse()
	throws Exception
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

        HttpParser parser= new Parser(buffer);
        parser.parse();
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
    	throws Exception
    {
        String http=
            "GET /chunk HTTP/1.0\015\012"
                + "Header1: value1\015\012"
				+ "Transfer-Encoding: chunked\015\012"
                + "\015\012"
                + "a;\015\012"
                + "0123456789\015\012"
                + "1a\015\012"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                + "0\015\012";
        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        HttpParser parser= new Parser(buffer);
        parser.parse();
        assertEquals("GET", f0);
        assertEquals("/chunk", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(1, h);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);
    }

    public void testMultiParse()
		throws Exception
    {
        String http=
            "GET /mp HTTP/1.0\015\012"
                + "Header1: value1\015\012"
				+ "Transfer-Encoding: chunked\015\012"
                + "\015\012"
                + "a;\015\012"
                + "0123456789\015\012"
                + "1a\015\012"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                + "0\015\012"
                + "POST /foo HTTP/1.0\015\012"
                + "Header2: value2\015\012"
				+ "Content-Length: 0\015\012"
                + "\015\012"
                + "PUT /doodle HTTP/1.0\015\012"
                + "Header3: value3\015\012"
				+ "Content-Length: 10\015\012"
                + "\015\012"
                + "0123456789\015\012";

        ByteArrayBuffer buffer= new ByteArrayBuffer(http.getBytes());

        HttpParser parser= new Parser(buffer);
        parser.parse();
        assertEquals("GET", f0);
        assertEquals("/mp", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(1, h);
        assertEquals("Header1", hdr[0]);
        assertEquals("value1", val[0]);
        assertEquals("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);

        parser.parse();
        assertEquals("POST", f0);
        assertEquals("/foo", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(1, h);
        assertEquals("Header2", hdr[0]);
        assertEquals("value2", val[0]);
        assertEquals(null, content);

        parser.parse();
        assertEquals("PUT", f0);
        assertEquals("/doodle", f1);
        assertEquals("HTTP/1.0", f2);
        assertEquals(1, h);
        assertEquals("Header3", hdr[0]);
        assertEquals("value3", val[0]);
        assertEquals("0123456789", content);
    }

    public void testInputStreamParse() throws Exception
    {
        String http=
            "GET / HTTP/1.0\015\012"
                + "Header1: value1\015\012"
				+ "Transfer-Encoding: chunked\015\012"
                + "\015\012"
                + "a;\015\012"
                + "0123456789\015\012"
                + "1a\015\012"
                + "ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"
                + "0\015\012"
                + "POST /foo HTTP/1.0\015\012"
                + "Header2: value2\015\012"
                + "Content-Length: 0\015\012"
                + "\015\012"
                + "PUT /doodle HTTP/1.0\015\012"
                + "Header3: value3\015\012"
				+ "Content-Length: 10\015\012"
                + "\015\012"
                + "0123456789\015\012";

        File file= File.createTempFile("test", ".txt");
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
                24
            };

        for (int t= 0; t < tests.length; t++)
        {
            String tst="t"+tests[t];
            InputStreamBuffer buffer= new InputStreamBuffer(new FileInputStream(file), tests[t]);

            HttpParser parser= new Parser(buffer);
            parser.parse();
            assertEquals(tst,"GET", f0);
            assertEquals(tst,"/", f1);
            assertEquals(tst,"HTTP/1.0", f2);
            assertEquals(tst,1, h);
            assertEquals(tst,"Header1", hdr[0]);
            assertEquals(tst,"value1", val[0]);
            assertEquals(tst,"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", content);

            parser.parse();
            assertEquals(tst,"POST", f0);
            assertEquals(tst,"/foo", f1);
            assertEquals(tst,"HTTP/1.0", f2);
            assertEquals(tst,1, h);
            assertEquals(tst,"Header2", hdr[0]);
            assertEquals(tst,"value2", val[0]);
            assertEquals(tst,null, content);

            parser.parse();
            assertEquals(tst,"PUT", f0);
            assertEquals(tst,"/doodle", f1);
            assertEquals(tst,"HTTP/1.0", f2);
            assertEquals(tst,1, h);
            assertEquals(tst,"Header3", hdr[0]);
            assertEquals(tst,"value3", val[0]);
            assertEquals(tst,"0123456789", content);
        }
    }

    String content;
    String f0;
    String f1;
    String f2;
    String[] hdr;
    String[] val;
    int h;
    
    class Parser extends HttpParser
    {
        Parser(Buffer source)
        {
            super(source);
        }
        
        public void foundContent(int index, Buffer ref)
        {
            if (index == 0)
                content= "";
            content= content.substring(0, index) + ref;
        }

        public void foundField0(Buffer ref)
        {
            h= -1;
            hdr= new String[9];
            val= new String[9];
            f0= ref.toString();
        }

        public void foundField1(Buffer ref)
        {
            f1= ref.toString();
        }

        public void foundField2(Buffer ref)
        {
            f2= ref.toString();
        }

        public void foundHttpHeader(Buffer ref)
        {
            hdr[++h]= ref.toString();
        }

        public void foundHttpValue(Buffer ref)
        {
            if (val[h] == null)
                val[h]= ref.toString();
            else
                val[h] += ref.toString();
        }

        public void headerComplete()
        {
            content= null;
        }

        public void messageComplete(int contentLength)
        {
        }
    }
}
