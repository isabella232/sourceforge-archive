// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import com.sun.java.util.collections.*;
import com.mortbay.Util.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** 
 *
 * @see
 * @version 1.0 Wed Sep 29 1999
 * @author Greg Wilkins (gregw)
 */
public class TestHarness
{
    public static String CRLF = HttpFields.__CRLF;

            
    /* -------------------------------------------------------------- */
    public static void chunkInTest()
        throws Exception
    {
        Test test = new Test("com.mortbay.HTTP.ChunkableInputStream");

        byte[] buf = new byte[18];
        
        try{
            FileInputStream fin= new FileInputStream("TestData/test.chunkIn");
            ChunkableInputStream cin = new ChunkableInputStream(fin);
            cin.setContentLength(10);
            test.checkEquals(cin.read(buf),10,"content length limited");
            test.checkEquals(cin.read(buf),-1,"content length EOF");
            
            fin= new FileInputStream("TestData/test.chunkIn");
            cin = new ChunkableInputStream(fin);
            cin.setChunking(true);
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
                             "12"+CRLF+"34",
                             "Read CRLF");
            test.checkEquals(cin.read(buf),12,"Read to EOF");
            test.checkEquals(new String(buf,0,12),
                             "567890abcdef","Read to EOF");
            test.checkEquals(cin.read(buf),-1,"Read EOF");
            test.checkEquals(cin.read(buf),-1,"Read EOF again");

            test.checkEquals(cin.getFooters().get("some-footer"),
                             "some-value","Footer fields");
  
            fin= new FileInputStream("TestData/test.chunkIn");
            cin = new ChunkableInputStream(fin);
            cin.__maxLineLength=8;
            test.checkEquals(cin.readRawLine().length(),8,
                             "line length limited");
            test.checkEquals(cin.readRawLine().length(),8,
                             "line length limited");
            test.checkEquals(cin.readRawLine().length(),4,
                             "line length limited");
            cin.__maxLineLength=8192;
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
        Test test = new Test("com.mortbay.HTTP.ChunkableOutputStream");

        try{
            FileOutputStream fout = new FileOutputStream("TestData/tmp.chunkOut");
            ChunkableOutputStream cout = new ChunkableOutputStream(fout,null);
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
            
            FileInputStream ftmp= new FileInputStream("TestData/tmp.chunkOut");
            ChunkableInputStream cin = new ChunkableInputStream(ftmp);
            cin.setChunking(true);

            test.checkEquals(cin.read(),'a',"a in 1");
            byte[] b = new byte[100];
            test.checkEquals(cin.read(b,0,2),2,"bc in 23");
            test.checkEquals(b[0],'b',"b in 2");
            test.checkEquals(b[1],'c',"c in 3");

            LineInput lin = new LineInput(cin);
            String line=lin.readLine();
            
            test.checkEquals(line.length(),33,"def...");
            test.checkEquals(line,"defghijklmnopqrstuvwxyz0123456789",
                             "readLine");
            int chars=0;
            while (cin.read()!=-1)
                chars++;
            test.checkEquals(chars,400*11,"Auto flush");
            
            ftmp= new FileInputStream("TestData/tmp.chunkOut");
            FileInputStream ftest= new FileInputStream("TestData/test.chunkOut");
            test.checkEquals(ftmp,ftest,"chunked out");
        }
        catch(Exception e)
        {
            test.check(false,e.toString());
        }
    }
    
    /* --------------------------------------------------------------- */
    public static void httpFields()
        throws IOException
    {
        String h1 =
            "Content-Type: xyz" + CRLF +
            "I1	:	42   " + CRLF +
            "D1: Fri, 31 Dec 1999 23:59:59 GMT" + CRLF +
            "D2: Friday, 31-Dec-99 23:59:59 GMT" + CRLF +
            "D3: Fri Dec 31 23:59:59 1999" + CRLF +
            "D4: Mon Jan 1 2000 00:00:01" + CRLF +
            "D5: Tue Feb 29 2000 12:00:00" + CRLF +
            "C1: Continuation  " + CRLF +
            "    Value  " + CRLF +
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
            "C1: Continuation Value" + CRLF +
            CRLF;
        

        ByteArrayInputStream bais = new ByteArrayInputStream(h1.getBytes());
        ChunkableInputStream cis = new ChunkableInputStream(bais);


        Test t = new Test("com.mortbay.HTTP.HttpFields");

        try
        {    
            HttpFields f = new HttpFields();
            f.read(cis);
            
            t.checkEquals(cis.readRawLine(),
                          "Other Stuff","Read headers");
        
            t.checkEquals(f.get(HttpFields.ContentType),
                          "xyz","getHeader");
            f.put(HttpFields.ContentType,"pqy");
            t.checkEquals(f.get(HttpFields.ContentType),
                          "pqy","setHeader");
        
            t.checkEquals(f.getIntField("I1"),42,"getIntHeader");
            f.putIntField("I1",-33);
            t.checkEquals(f.getIntField("I1"),-33,"setIntHeader");
        
        
            long d1 = f.getDateField("D1");
            long d2 = f.getDateField("D2");
            long d3 = f.getDateField("D3");
            long d4 = f.getDateField("D4");
            long d5 = f.getDateField("D5");
            t.check(d1>0,"getDateHeader1");
            t.check(d2>0,"getDateHeader2");
            t.checkEquals(d1,d2,"getDateHeader12");
            t.checkEquals(d2,d3,"getDateHeader23");
            t.checkEquals(d3+2000,d4,"getDateHeader34");

            f.putDateField("D2",d1);
            t.checkEquals(f.get("D1"),f.get("D2"),
                          "setDateHeader12");

            String h3 = f.toString();
            t.checkEquals(h2,h3,"toString");


            HashMap params = new HashMap();
            String value = HttpFields.valueParameters(" v ; p1=v1 ; p2 = \" v 2 \";p3 ; p4='v4=;' ;",params);
            t.checkEquals(value,"v","value");
            t.checkEquals(params.size(),4,"params");
            t.checkEquals(params.get("p1"),"v1","p1=v1");
            t.checkEquals(params.get("p2")," v 2 ","p2=\" v 2 \"");
            t.checkEquals(params.get("p3"),null,"p3=null");
            t.check(params.containsKey("p3"),"p3");
            t.checkEquals(params.get("p4"),"v4=;","p4=v4=;");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
        try{
            chunkInTest();
            chunkOutTest();
            httpFields();
            //httpRequest();
            //pathMap();
        }
        catch(Throwable e)
        {
            Code.warning(e);
            new Test("com.mortbay.HTTP.TestHarness").check(false,e.toString());
        }
        finally
        {
            Test.report();
        }
    }
};






