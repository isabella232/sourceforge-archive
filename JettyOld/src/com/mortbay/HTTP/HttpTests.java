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
	    "D4: Mon Jan 1 2000 00:00:01" + CRLF +
	    "D5: Tue Feb 29 2000 12:00:00" + CRLF +
	    CRLF +
	    "Other Stuff"+ CRLF;
	
	String h2 =
	    "Content-Type: pqy" + CRLF +
	    "I1: -33" + CRLF +
	    "D1: Fri, 31 Dec 1999 23:59:59 GMT" + CRLF +
	    "D2: Fri, 31 Dec 1999 23:59:59 GMT" + CRLF +
	    "D3: Fri Dec 31 23:59:59 1999" + CRLF +
	    "D4: Mon Jan 1 2000 00:00:01" + CRLF +
	    "D5: Tue Feb 29 2000 12:00:00" + CRLF +
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
	long d4 = h.getDateHeader("D4");
	long d5 = h.getDateHeader("D5");
	t.check(d1>0,"getDateHeader1");
	t.check(d2>0,"getDateHeader2");
	t.checkEquals(d1,d2,"getDateHeader12");
	t.checkEquals(d2,d3,"getDateHeader23");
	t.checkEquals(d3+2000,d4,"getDateHeader34");

	h.setDateHeader("D2",d1);
	t.checkEquals(h.getHeader("D1"),h.getHeader("D2"),
		      "setDateHeader12");

	String h3 = h.toString();
	t.checkEquals(h2,h3,"toString");
	
    }
    
    /* --------------------------------------------------------------- */
    public static void pathMap()
    {
	Test test =null;

	try{
	    test = new Test("com.mortbay.HTTP.PathMap$WildMap");

	    com.mortbay.HTTP.PathMap$WildMap wm =
		new com.mortbay.HTTP.PathMap$WildMap();
	    String[] h1 = {"*","p*","pp*","*s","p*s","pp*s","*ss","p*ss","pp*ss"};
	    wm.put(h1[5],h1[5]);wm.put(h1[6],h1[6]);wm.put(h1[1],h1[1]);
	    wm.put(h1[7],h1[7]);wm.put(h1[0],h1[0]);wm.put(h1[3],h1[3]);
	    wm.put(h1[2],h1[2]);wm.put(h1[8],h1[8]);wm.put(h1[4],h1[4]);
	    test.checkEquals(wm.holders.size(),9,"9 Holders");
	    wm.get("");
	    for (int i=9;i-->0;)
		test.checkEquals(h1[i],wm.cache[i].value,"order"+i);
	    wm.put(h1[5],h1[5]);wm.put(h1[4],h1[4]);wm.put(h1[8],h1[8]);
	    wm.put(h1[7],h1[7]);wm.put(h1[3],h1[3]);wm.put(h1[2],h1[2]);
	    wm.put(h1[6],h1[6]);wm.put(h1[0],h1[0]);wm.put(h1[1],h1[1]);
	    test.checkEquals(wm.holders.size(),9,"9 Holders");
	    wm.get("");
	    for (int i=9;i-->0;)
		test.checkEquals(h1[i],wm.cache[i].value,"order"+i);
	    
	    String[] h2 ={ "*","x*","xx*","*y","x*y","xx*y","*yy","z*yy","yy*yy"};
	    
	    wm.put(h2[5],h2[5]);wm.put(h2[4],h2[4]);wm.put(h2[8],h2[8]);
	    wm.put(h2[0],h2[0]);wm.put(h2[3],h2[3]);wm.put(h2[6],h2[6]);
	    wm.put(h2[7],h2[7]);wm.put(h2[2],h2[2]);wm.put(h2[1],h2[1]);
	    test.checkEquals(wm.holders.size(),17,"17 Holders");
	    wm.get("");
	    for (int i=9;i-->1;)
	    {
		test.checkEquals(h2[i],wm.cache[2*i].value,"order2-"+i);
		test.checkEquals(h1[i],wm.cache[2*i-1].value,"order1-"+i);
	    }
	    test.checkEquals(h1[0],wm.cache[0].value,"order1-0");
	    
	    test.checkEquals(wm.get("ppss"),h1[8],"match1");
	    test.checkEquals(wm.get("pppsss"),h1[8],"match2");
	    test.checkEquals(wm.get("ppxxss"),h1[8],"match3");
	    test.checkEquals(wm.get("pxxs"),h1[4],"match4");
	    test.checkEquals(wm.get("pppyy"),h2[6],"match5");
	    test.checkEquals(wm.get(""),h2[0],"match6");
	    test.checkEquals(wm.get("X"),h2[0],"match7");
	    test.checkEquals(wm.get("xxx"),h2[2],"match8");
	    test.checkEquals(wm.get("yyy"),h2[6],"match9");
	    test.checkEquals(wm.get("yyyy"),h2[8],"matchA");
	    
	    
	    
	    
	    test = new Test("com.mortbay.HTTP.PathMap");
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


	    pm = new PathMap();
	    pm.put("/aaa%","/aaa%");
	    pm.put("/aaa/bbb/ccc%","/aaa/bbb/ccc%");
	    pm.put("*.a","*.a");
	    pm.put("*.b","*.b");
	    pm.put("*","*");
	    pm.put("/bbb/","/bbb/");
	    pm.put("/bbb/*.b","/bbb/*.b");
	    pm.put("/bbb/xxx.b","/bbb/xxx.b");
	    pm.put("/ccc/*.c","/ccc/*.c");
	    
	    test.checkEquals(pm.longestMatch("/aaa"),"/aaa%","/aaa==/aaa%");
	    test.checkEquals(pm.longestMatch("/aaa.a"),"*.a","/aaa.a==*.a");
	    test.checkEquals(pm.longestMatch("/aaa/bbb/ccc/aaa.a"),"*.a","/aaa/bbb/ccc/aaa.a==*.a");
	    test.checkEquals(pm.longestMatch("/aaa/bbb/ccc/aaa.c"),"/aaa/bbb/ccc%","/aaa/bbb/ccc/aaa.c==/aaa/bbb/ccc%");
	    test.checkEquals(pm.longestMatch("/bbb/ccc"),"/bbb/","/bbb/ccc==/bbb/");
	    test.checkEquals(pm.longestMatch("/aaa/bbb.b"),"*.b","/aaa/bbb.b==*.b");
	    test.checkEquals(pm.longestMatch("/bbb/bbb.b"),"/bbb/*.b","/aaa/bbb.b==/bbb/*.b");
	    test.checkEquals(pm.longestMatch("/bbb/xxx.b"),"/bbb/xxx.b","/aaa/xxx.b==/bbb/xxx.b");
	    test.checkEquals(pm.longestMatch("anything"),"*","anything==*");
	    
	}
	catch(Exception e)
	{
	    test.check(false,e.toString());
	    Code.warning(e);
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

	byte[] buf = new byte[18];
	
	try{
	    FileInputStream fin= new FileInputStream("test.chunkIn");
	    HttpInputStream cin = new HttpInputStream(fin);
	    cin.setContentLength(10);
	    test.checkEquals(cin.read(buf),10,"content length limited");
	    test.checkEquals(cin.read(buf),-1,"content length EOF");
	    
	    fin= new FileInputStream("test.chunkIn");
	    cin = new HttpInputStream(fin);
	    cin.chunking(true);
	    test.checkEquals(cin.read(),'a',"Read 1st char");
	    test.checkEquals(cin.read(),'b',"Read cont char");
	    test.checkEquals(cin.read(),'c',"Read next chunk char");

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
