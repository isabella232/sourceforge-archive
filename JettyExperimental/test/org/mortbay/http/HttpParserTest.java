package org.mortbay.http;

import org.mortbay.util.ByteArrayBuffer;
import org.mortbay.util.Buffer;

import junit.framework.TestCase;

/**
 * @author gregw
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class HttpParserTest extends TestCase implements HttpParser.Handler
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

    public void testParse()
    {
        String http= "GET / HTTP/1.0\015\012" + 
        "Header1: value1\015\012" + 
        "Header2  :   value 2a  \015\012" + 
		"                    value 2b  \015\012" + 
		"Header3: \015\012" + 
		"Header4 \015\012" + 
		"  value4\015\012" + 
		"\015\012"+
		"a;\015\012"+
		"0123456789\015\012"+
		"1a\015\012"+
		"ABCDEFGHIJKLMNOPQRSTUVWXYZ\015\012"+
		"0\015\012";
		ByteArrayBuffer buffer = new ByteArrayBuffer(http.getBytes());
		HttpParser.parse(this,buffer);
    }

    /**
     * @see org.mortbay.http.HttpParser.Handler#foundContent(int, Buffer)
     */
    public void foundContent(int offset, Buffer ref)
    {
    	System.out.println("Content["+offset+"]="+ref);
    }

    /**
     * @see org.mortbay.http.HttpParser.Handler#foundField0(Buffer)
     */
    public void foundField0(Buffer ref)
    {
    	System.out.println("Field0="+ref);
    }

    /**
     * @see org.mortbay.http.HttpParser.Handler#foundField1(Buffer)
     */
    public void foundField1(Buffer ref)
    {
		System.out.println("Field1="+ref);
    }

    /**
     * @see org.mortbay.http.HttpParser.Handler#foundField2(Buffer)
     */
    public void foundField2(Buffer ref)
    {
		System.out.println("Field2="+ref);
    }

    /**
     * @see org.mortbay.http.HttpParser.Handler#foundHttpHeader(Buffer)
     */
    public void foundHttpHeader(Buffer ref)
    {
		System.out.println("Header="+ref);
    }

    /**
     * @see org.mortbay.http.HttpParser.Handler#foundHttpValue(Buffer)
     */
    public void foundHttpValue(Buffer ref)
    {
		System.out.println("Value='"+ref+"'");
    }

    /**
     * @see org.mortbay.http.HttpParser.Handler#getContentLength()
     */
    public int getContentLength()
    {
        return HttpParser.CHUNKED_CONTENT;
    }

    /**
     * @see org.mortbay.http.HttpParser.Handler#headerComplete()
     */
    public void headerComplete()
    {
		System.out.println("Header Complete");
    }

    /**
     * @see org.mortbay.http.HttpParser.Handler#messageComplete(int)
     */
    public void messageComplete(int contentLength)
    {
		System.out.println("Message Complete: "+contentLength);
    }
}
