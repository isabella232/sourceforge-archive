// ========================================================================
// Copyright (c) 1998 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Base.Code;
import com.mortbay.Base.Test;
import java.util.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Misc HTTP Tests
 * <p>
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 * </pre>
 *
 * @see
 * @version $Revision$ $Date$
 * @author Greg Wilkins (gregw)
 */
public class HttpTests
{
    public static String CRLF = HttpHeader.CRLF;
    

    /* --------------------------------------------------------------- */
    public static void httpHeader()
    throws IOException
    {
	String h1 =
	    "Content-Type: xyz" + CRLF +
	    "  I1  : 42   " + CRLF +
	    "D1: Fri, 31 Dec 1999 23:59:59 GMT" + CRLF +
	    "D2: Friday, 31-Dec-99 23:59:59 GMT" + CRLF +
	    "D3: Fri Dec 31 23:59:59 1999" + CRLF +
	    CRLF +
	    "Other Stuff"+ CRLF;
	
	String h2 =
	    "Content-Type: pqy" + CRLF +
	    "I1: -33" + CRLF +
	    "D1: Fri, 31 Dec 1999 23:59:59 GMT" + CRLF +
	    "D2: Fri, 31 Dec 1999 23:59:59 GMT" + CRLF +
	    "D3: Fri Dec 31 23:59:59 1999" + CRLF +
	    CRLF;
	

	ByteArrayInputStream bais = new ByteArrayInputStream(h1.getBytes());
	HttpInputStream his = new HttpInputStream(bais);

	HttpHeader h = new HttpHeader();
	h.read(his);

	Test t = new Test("com.mortbay.HTTP.HttpHeader");
	
	t.checkEquals(his.readLine(),"Other Stuff","Read headers");
	
	t.checkEquals(h.getHeader(HttpHeader.ContentType),
		      "xyz","getHeader");
	h.setHeader(HttpHeader.ContentType,"pqy");
	t.checkEquals(h.getHeader(HttpHeader.ContentType),
		      "pqy","setHeader");
	
	t.checkEquals(h.getIntHeader("I1"),42,"getIntHeader");
	h.setIntHeader("I1",-33);
	t.checkEquals(h.getIntHeader("I1"),-33,"setIntHeader");
	
	long d1 = h.getDateHeader("D1");
	long d2 = h.getDateHeader("D2");
	long d3 = h.getDateHeader("D3");
	t.checkEquals(d1,d2,"getDateHeader");
	t.checkEquals(d2,d3,"getDateHeader");

	h.setDateHeader("D2",d1);
	t.checkEquals(h.getHeader("D1"),h.getHeader("D2"),
		      "setDateHeader");

	String h3 = h.toString();
	t.checkEquals(h2,h3,"toString");
	
    }
    
    /* --------------------------------------------------------------- */
    public static void pathMap()
    {
	Test test = new Test("com.mortbay.HTTP.PathMap");

	try{
	    PathMap pm = new PathMap();
	    test.checkEquals(pm.longestMatch("x"),null,"empty");
	    pm.put("aaa","a");
	    test.checkEquals(pm.longestMatch("x"),null,"miss");
	    test.checkEquals(pm.longestMatch("a"),null,"miss");
	    test.checkEquals(pm.longestMatch("aaabbb"),"aaa","match");
	    pm.put("aaabb","b");
	    test.checkEquals(pm.longestMatch("aaaaa"),"aaa","reject bb");
	    test.checkEquals(pm.longestMatch("aaabbb"),"aaabb","get longest");
	    pm.put("aaab","c");
	    test.checkEquals(pm.longestMatch("aaaaa"),"aaa","reject bb");
	    test.checkEquals(pm.longestMatch("aaabbb"),"aaabb","get longest");
	    pm.put("aa","d");
	    test.checkEquals(pm.longestMatch("aaaaa"),"aaa","reject bb");
	    test.checkEquals(pm.longestMatch("aaabbb"),"aaabb","get longest");
	    pm.put("aaabbb","e");
	    test.checkEquals(pm.longestMatch("aaaaa"),"aaa","reject bb");
	    test.checkEquals(pm.longestMatch("aaabbb"),"aaabbb","get longest");
	    pm.put("aa$","f");
	    test.checkEquals(pm.longestMatch("aaa"),"aaa","reject non exact");
	    pm.remove("aaa");
	    test.checkEquals(pm.longestMatch("aaa"),"aa","remove");
	    test.checkEquals(pm.longestMatch("aa"),"aa$","exact");
	    
	    pm.put("xxx%","g");
	    test.checkEquals(pm.longestMatch("xxxx"),null,"exact or path");
	    test.checkEquals(pm.longestMatch("xxx"),"xxx%","exact or path");
	    test.checkEquals(pm.longestMatch("xxx/"),"xxx%","exact or path");
	    
	    pm.put("/","h");
	    test.checkEquals(pm.longestMatch("/any"),"/","root match");

	    pm.put("yyy|","i");
	    test.checkEquals(pm.longestMatch("yyy"),"yyy|","no trail / match");
	    test.checkEquals(pm.longestMatch("yyy/"),"yyy|","trail / match");
	    test.checkEquals(pm.longestMatch("yyy/zzz"),null,"trail / mis");

	    System.err.println(pm);

	    test.checkEquals(PathMap.match("aaa","aaa/bbb"),"aaa","matching part 1");
	    test.checkEquals(PathMap.match("aaa/","aaa/bbb"),"aaa/","matching part 2");
	    test.checkEquals(PathMap.match("aaa%","aaa/bbb"),"aaa/","matching part 3");
	    test.checkEquals(PathMap.match("aaa$","aaa/bbb"),null,"matching part 4");
	    test.checkEquals(PathMap.match("aaa|","aaa/bbb"),null,"matching part 5");
	    test.checkEquals(PathMap.match("aaa/bbb$",
				   "aaa/bbb"),"aaa/bbb","matching part 6");
	    test.checkEquals(PathMap.match("aaa/bbb|",
				   "aaa/bbb"),"aaa/bbb","matching part 7");
	    
	    test.checkEquals(PathMap.match("aaa/bbb",
				   "aaa/bbb/"),"aaa/bbb","matching part 8");
	    test.checkEquals(PathMap.match("aaa/bbb/",
				   "aaa/bbb/"),"aaa/bbb/","matching part 9");
	    test.checkEquals(PathMap.match("aaa/bbb%",
				   "aaa/bbb/"),"aaa/bbb/","matching part 10");
	    test.checkEquals(PathMap.match("aaa/bbb$",
				   "aaa/bbb/"),null,"matching part 11");
	    test.checkEquals(PathMap.match("aaa/bbb|",
				   "aaa/bbb/"),"aaa/bbb/","matching part 12");

	}
	catch(Exception e)
	{
	    test.check(false,e.toString());
	    Code.debug("failed",e);
	}
    }
    
    
    /* --------------------------------------------------------------- */
    public static void httpRequest()
    {
	Test test = new Test("com.mortbay.HTTP.HttpHeader");

	String[] rl =
	{
	    "GET /xxx HTTP/1.0",          "GET", "/xxx",    "HTTP/1.0",
	    " GET /xxx HTTP/1.0 ",        "GET", "/xxx",    "HTTP/1.0",
	    "  PUT  /xxx  HTTP/1.1  ",    "PUT", "/xxx",    "HTTP/1.1",
	    "  GET  /xxx   ",             "GET", "/xxx",    "HTTP/1.0",
	    "GET  /xxx",                  "GET", "/xxx",    "HTTP/1.0",
	    "  GET  /xxx   ",             "GET", "/xxx",    "HTTP/1.0",
	    "GET / ",                     "GET", "/",       "HTTP/1.0",
	    "GET /",                      "GET", "/",       "HTTP/1.0",
	    "GET http://h:p/ HTTP/1.0",   "GET", "/",       "HTTP/1.0",
	    "GET http://h:p/xx HTTP/1.0", "GET", "/xx",     "HTTP/1.0",
	    "GET http HTTP/1.0",          "GET", "http",    "HTTP/1.0",
	    "GET http://h:p/",            "GET", "/",       "HTTP/1.0",
	    "GET http://h:p/xxx",         "GET", "/xxx",    "HTTP/1.0",
	    "  GET     ",                 null,  null,      null,
	    "GET",                        null,  null,      null,
	    "",                           null,  null,      null,
	};

	HttpRequest r = new HttpRequest("GET","/");
	
	try{
	    for (int i=0; i<rl.length ; i+=4)
	    {
		try{
		    r.decodeRequestLine(rl[i].toCharArray(),rl[i].length());
		    test.checkEquals(r.getMethod(),rl[i+1],rl[i]);
		    test.checkEquals(r.getRequestURI(),rl[i+2],rl[i]);
		    test.checkEquals(r.getVersion(),rl[i+3],rl[i]);
		}
		catch(IOException e)
		{
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
    
	    
    /* -------------------------------------------------------------- */
    public static void chunkInTest()
	throws Exception
    {
	Test test = new Test("com.mortbay.HTTP.HttpInputStream");

	try{
	    FileInputStream fin= new FileInputStream("test.chunkIn");
	    HttpInputStream cin = new HttpInputStream(fin);
	    cin.chunking(true);
	    test.checkEquals(cin.read(),'a',"Read 1st char");
	    test.checkEquals(cin.read(),'b',"Read cont char");
	    test.checkEquals(cin.read(),'c',"Read next chunk char");

	    byte[] buf = new byte[18];
	    test.checkEquals(cin.read(buf),17,"Read array chunk limited");
	    test.checkEquals(new String(buf,0,17),
			     "defghijklmnopqrst","Read array chunk");
	    test.checkEquals(cin.read(buf,1,10),6,"Read Offset limited");
	    test.checkEquals(new String(buf,0,17),"duvwxyzklmnopqrst",
			     "Read offset");
	    test.checkEquals(cin.read(buf),6,"Read CRLF");
	    test.checkEquals(new String(buf,0,6),
			     "12"+HttpHeader.CRLF+"34",
			     "Read CRLF");
	    test.checkEquals(cin.read(buf),12,"Read to EOF");
	    test.checkEquals(new String(buf,0,12),
			     "567890abcdef","Read to EOF");
	    test.checkEquals(cin.read(buf),-1,"Read EOF");
	    test.checkEquals(cin.read(buf),-1,"Read EOF again");

	    test.checkEquals(cin.getFooters().getHeader("some-footer"),
			     "some-value","Footer fields");
  
	}
	catch(Exception e)
	{
	    test.check(false,e.toString());
	}
    }
    
    /* -------------------------------------------------------------- */
    public static void chunkOutTest()
	throws Exception
    {
	Test test = new Test("com.mortbay.HTTP.HttpOutputStream");

	try{
	    FileOutputStream fout = new FileOutputStream("tmp.chunkOut");
	    HttpOutputStream cout = new HttpOutputStream(fout,null);
	    cout.setChunking(true);
	    
	    cout.flush();
	    cout.write('a');
	    cout.flush();
	    cout.write('b');
	    cout.write('c');
	    cout.flush();
	    cout.write("defghijklmnopqrstuvwxyz".getBytes());
	    cout.flush();
	    cout.write("XXX0123456789\nXXX".getBytes(),3,11);
	    cout.flush();
	    byte[] eleven = "0123456789\n".getBytes();
	    for (int i=0;i<400;i++)
		cout.write(eleven);
	    cout.close();
	    
	    FileInputStream ftmp= new FileInputStream("tmp.chunkOut");
	    HttpInputStream cin = new HttpInputStream(ftmp);
	    cin.chunking(true);

	    test.checkEquals(cin.read(),'a',"chunk out->in a");
	    byte[] b = new byte[100];
	    test.checkEquals(cin.read(b,0,2),2,"chunk out->in 23");
	    test.checkEquals(b[0],'b',"chunk out in b");
	    test.checkEquals(b[1],'c',"chunk out in c");
	    test.checkEquals(cin.readLine(b,0,100),34,"readline length");
	    test.checkEquals(new String(b,0,33),
			     "defghijklmnopqrstuvwxyz0123456789",
			     "readline");
	    int chars=0;
	    while (cin.read()!=-1)
		chars++;
	    test.checkEquals(chars,400*11,"Auto flush");
	    
	    ftmp= new FileInputStream("tmp.chunkOut");
	    FileInputStream ftest= new FileInputStream("test.chunkOut");
	    test.checkEquals(ftmp,ftest,"chunked out");
	}
	catch(Exception e)
	{
	    test.check(false,e.toString());
	}
    }
    
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
	try{
	    pathMap();
	    httpHeader();
	    httpRequest();
	    chunkInTest();
	    chunkOutTest();
	}
	catch(Exception e)
	{
	    Code.warning(e);
	    new Test("com.mortbay.HTTP.HttpTests").check(false,e.toString());
	}
	finally
	{
	    Test.report();
	}
    }
    
};
