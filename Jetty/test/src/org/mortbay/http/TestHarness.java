// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.FilterInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.Enumeration;
import java.util.HashMap;
import org.mortbay.util.Code;
import org.mortbay.util.LineInput;
import org.mortbay.util.TestCase;
import javax.servlet.http.Cookie;

/* ------------------------------------------------------------ */
/** Top level test harness.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class TestHarness
{
    public final static String CRLF = "\015\012";
    public static String __userDir =
        System.getProperty("user.dir",".");
    public static URL __userURL=null;
    static
    {
        try{
            File file = new File(__userDir);
            __userURL=file.toURL();
            if (!__userURL.toString().endsWith("/http/"))
            {
                __userURL=new URL(__userURL.toString()+
                                  "test/src/org/mortbay/http/");
                FilePermission perm = (FilePermission)
                    __userURL.openConnection().getPermission();
                __userDir=new File(perm.getName()).getCanonicalPath();
            }                
        }
        catch(Exception e)
        {
            Code.fail(e);
        }
    }
    
    /* -------------------------------------------------------------- */
    public static void chunkInTest()
        throws Exception
    {
        TestCase test = new TestCase("org.mortbay.http.HttpInputStream");

        byte[] buf = new byte[18];
        
        try{
            FileInputStream fin=
                new FileInputStream(__userDir+File.separator+
                                    "TestData"+File.separator+
                                    "chunkIn.bin");
            HttpInputStream cin = new HttpInputStream(fin);
            cin.setContentLength(10);
            test.checkEquals(cin.read(buf),10,"content length limited");
            test.checkEquals(cin.read(buf),-1,"content length EOF");
            
            fin= new FileInputStream(__userDir+File.separator+
                                    "TestData"+File.separator+
                                    "chunkIn.bin");
            cin = new HttpInputStream(fin);
            cin.setChunking();
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

            System.err.println("\n\nTrailer:\n"+cin.getTrailer());
            test.checkEquals(cin.getTrailer().get("some-trailer"),
                             "some-value","Trailer fields");

            // Read some more after a reset
            cin.resetStream();
            cin.setChunking();
            test.checkEquals(cin.read(),'a',"2 Read 1st char");
            test.checkEquals(cin.read(),'b',"2 Read cont char");
            test.checkEquals(cin.read(),'c',"2 Read next chunk char");

            test.checkEquals(cin.read(buf),17,"2 Read array chunk limited");
            test.checkEquals(new String(buf,0,17),
                             "defghijklmnopqrst","2 Read array chunk");
            test.checkEquals(cin.read(buf,1,10),6,"2 Read Offset limited");
            test.checkEquals(new String(buf,0,17),"duvwxyzklmnopqrst",
                             "2 Read offset");
            test.checkEquals(cin.read(buf),6,"2 Read CRLF");
            test.checkEquals(new String(buf,0,6),
                             "12"+CRLF+"34",
                             "2 Read CRLF");
            test.checkEquals(cin.read(buf),12,"2 Read to EOF");
            test.checkEquals(new String(buf,0,12),
                             "567890abcdef","2 Read to EOF");
            test.checkEquals(cin.read(buf),-1,"2 Read EOF");
            test.checkEquals(cin.read(buf),-1,"2 Read EOF again");
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
        TestCase test = new TestCase("org.mortbay.http.HttpOutputStream");

        try{
            File tmpFile=File.createTempFile("HTTP.TestHarness",".chunked");

            if (!Code.debug())
                tmpFile.deleteOnExit();
            else
                Code.debug("Chunk out tmp = ",tmpFile);
            
            FileOutputStream fout = new FileOutputStream(tmpFile);
            HttpOutputStream cout = new HttpOutputStream(fout,4020);
            cout.setChunking();
            
            cout.write("Reset Output".getBytes());
            cout.resetBuffer();
            
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
            HttpFields trailer=new HttpFields();
            trailer.put("trailer1","value1");
            trailer.put("trailer2","value2");
            cout.setTrailer(trailer);
            cout.close();
            
            FileInputStream ftmp= new FileInputStream(tmpFile);
            ChunkingInputStream cin = new ChunkingInputStream(new LineInput(ftmp));

            test.checkEquals(cin.read(),'a',"a in 1");
            byte[] b = new byte[100];
            test.checkEquals(cin.read(b,0,2),2,"bc in 23");
            test.checkEquals(b[0],'b',"b in 2");
            test.checkEquals(b[1],'c',"c in 3");

            LineInput lin = new LineInput(cin);            
            String line=lin.readLine();
            
            test.checkEquals(line.length(),33,"def...");            test.checkEquals(line,"defghijklmnopqrstuvwxyz0123456789",
                             "readLine");
            int chars=0;
            while (cin.read()!=-1)
                chars++;
            test.checkEquals(chars,400*11,"Auto flush");

            
            ftmp= new FileInputStream(tmpFile);
            FileInputStream ftest=
                new FileInputStream(__userDir+File.separator+
                                    "TestData"+File.separator+
                                    "chunkOut.bin");
            test.checkEquals(ftmp,ftest,"chunked out");
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }

    /* -------------------------------------------------------------- */
    public static void chunkingOSTest()
        throws Exception
    {
        TestCase test = new TestCase("org.mortbay.http.ChunkingOutputStream");

        try{
            File tmpFile=File.createTempFile("HTTP.TestHarness",".chunked");

            if (!Code.debug())
                tmpFile.deleteOnExit();
            else
                Code.debug("Chunk out tmp = ",tmpFile);
            
            FileOutputStream fout = new FileOutputStream(tmpFile);
            ChunkingOutputStream cout = new ChunkingOutputStream(fout,4020);
            
            cout.write("Reset Output".getBytes());
            cout.reset();
            
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
            HttpFields trailer=new HttpFields();
            trailer.put("trailer1","value1");
            trailer.put("trailer2","value2");
            cout.setTrailer(trailer);
            cout.close();
            
            FileInputStream ftmp= new FileInputStream(tmpFile);
            HttpInputStream cin = new HttpInputStream(ftmp);
            cin.setChunking();

            test.checkEquals(cin.read(),'a',"a in 1");
            byte[] b = new byte[100];
            test.checkEquals(cin.read(b,0,2),2,"bc in 23");
            test.checkEquals(b[0],'b',"b in 2");
            test.checkEquals(b[1],'c',"c in 3");

            LineInput lin = new LineInput(cin);            
            String line=lin.readLine();
            
            test.checkEquals(line.length(),33,"def...");            test.checkEquals(line,"defghijklmnopqrstuvwxyz0123456789",
                             "readLine");
            int chars=0;
            while (cin.read()!=-1)
                chars++;
            test.checkEquals(chars,400*11,"Auto flush");
            
            ftmp= new FileInputStream(tmpFile);
            FileInputStream ftest=
                new FileInputStream(__userDir+File.separator+
                                    "TestData"+File.separator+
                                    "chunkOut.bin");
            test.checkEquals(ftmp,ftest,"chunked out");
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }

    /* --------------------------------------------------------------- */
    public static void filters()
    {
        TestCase t = new TestCase("org.mortbay.http.HttpXxxputStream");
        try
        {
            File tmpFile=File.createTempFile("HTTP.TestHarness",".gzip");
            tmpFile.deleteOnExit();
            FileOutputStream fout =
                new FileOutputStream(tmpFile);
            HttpOutputStream cout = new HttpOutputStream(fout);
            cout.setChunking();

            cout.setFilterStream(new java.util.zip.GZIPOutputStream(cout.getFilterStream()));
            
            byte[] data =
                "ABCDEFGHIJKlmnopqrstuvwxyz;:#$0123456789\n".getBytes();
            for (int i=0;i<400;i++)
                cout.write(data,0,data.length);
            
            cout.close();
            
            FileInputStream fin=
                new FileInputStream(tmpFile);
            HttpInputStream cin = new HttpInputStream(fin);
            cin.setChunking();
            cin.setFilterStream(new java.util.zip.GZIPInputStream(cin.getFilterStream()));
            
            for (int i=0;i<400;i++)
            {
                for (int j=0;j<data.length;j++)
                    if (cin.read()!=data[j])
                        t.check(false,"Data in differs at "+i+","+j);
            }
            t.checkEquals(cin.read(),-1,"EOF from gzip");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
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
            "L1: V1  " + CRLF +
            "L1: V2  " + CRLF +
            "L1: V,3  " + CRLF +
            "L2: V1, V2, 'V,3'" + CRLF +
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
            "L1: V1" + CRLF +
            "L1: V2" + CRLF +
            "L1: V,3" + CRLF +
            "L2: V1, V2, 'V,3'" + CRLF +
            CRLF;

        ByteArrayInputStream bais = new ByteArrayInputStream(h1.getBytes());
        LineInput lis = new LineInput(bais);


        TestCase t = new TestCase("org.mortbay.http.HttpFields");
        try
        {    
            HttpFields f = new HttpFields();
            f.read(lis);

            byte[] b = "xxxxxxxxxxxcl".getBytes();
            t.checkEquals(lis.read(b),13,"Read other");
            t.checkEquals(new String(b,0,11),
                          "Other Stuff","Read other");
        
            t.checkEquals(f.get(HttpFields.__ContentType),
                          "xyz","getHeader");
            f.put(HttpFields.__ContentType,"pqy");
            t.checkEquals(f.get(HttpFields.__ContentType),
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

            Enumeration e = f.getValues("L1");
            t.check(e.hasMoreElements(),"getValues L1[0]");
            if (e.hasMoreElements())
                t.checkEquals(e.nextElement(),"V1","getValues L1[0]==");
            t.check(e.hasMoreElements(),"getValues L1[1]");
            if (e.hasMoreElements())
                t.checkEquals(e.nextElement(),"V2","getValues L1[1]==");
            t.check(e.hasMoreElements(),"getValues L1[2]");
            if (e.hasMoreElements())
                t.checkEquals(e.nextElement(),"V,3","getValues L1[2]==");
            
            e = f.getValues("L2",", \t");
            t.check(e.hasMoreElements(),"getValues L2[0]");
            if (e.hasMoreElements())
                t.checkEquals(e.nextElement(),"V1","getValues L2[0]==");
            t.check(e.hasMoreElements(),"getValues L2[1]");
            if (e.hasMoreElements())
                t.checkEquals(e.nextElement(),"V2","getValues L2[1]==");
            t.check(e.hasMoreElements(),"getValues L2[2]");
            if (e.hasMoreElements())
                t.checkEquals(e.nextElement(),"V,3","getValues L2[2]==");
            
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

    /* --------------------------------------------------------------- */
    public static void pathMap()
    {
        TestCase t = new TestCase("org.mortbay.http.PathMap");
        try
        {
            PathMap p = new PathMap();

            p.put("/abs/path","1");
            p.put("/abs/path/longer","2");
            p.put("/animal/bird/*","3");
            p.put("/animal/fish/*","4");
            p.put("/animal/*","5");
            p.put("*.tar.gz","6");
            p.put("*.gz","7");
            p.put("/","8");
            p.put("/XXX:/YYY","9");


            String[][] tests =
            {
                {"/abs/path","1"},
                {"/abs/path/xxx","8"},
                {"/abs/pith","8"},
                {"/abs/path/longer","2"},
                {"/abs/path/","8"},
                {"/abs/path/xxx","8"},
                {"/animal/bird/eagle/bald","3"},
                {"/animal/fish/shark/grey","4"},
                {"/animal/insect/bug","5"},
                {"/animal","5"},
                {"/animal/","5"},
                {"/suffix/path.tar.gz","6"},
                {"/suffix/path.gz","7"},
                {"/animal/path.gz","5"},
                {"/Other/path","8"},
            };    

            for (int i=0;i<tests.length;i++)
            {
                t.checkEquals(p.getMatch(tests[i][0]).getValue(),tests[i][1],tests[i][0]);
                t.checkEquals(p.getMatch(tests[i][0]+"?a=1").getValue(),tests[i][1],tests[i][0]+"?a=1");
                t.checkEquals(p.getMatch(tests[i][0]+";a=1").getValue(),tests[i][1],tests[i][0]+";a=1");
                t.checkEquals(p.getMatch(tests[i][0]+";a=1?a=1").getValue(),tests[i][1],tests[i][0]+";a=1?a=1");
            }
            
            t.checkEquals(p.get("/abs/path"),"1","Get absolute path");
            t.checkEquals(p.getMatch("/abs/path").getKey(),"/abs/path",
                          "Match absolute path");
            
            t.checkEquals(p.getMatches("/animal/bird/path.tar.gz").toString(),
                          "[/animal/bird/*=3, /animal/*=5, *.tar.gz=6, *.gz=7, /=8]",
                          "all matches");
            
            t.checkEquals(p.getMatches("/animal/fish/").toString(),
                          "[/animal/fish/*=4, /animal/*=5, /=8]",
                          "Dir matches");
            t.checkEquals(p.getMatches("/animal/fish").toString(),
                          "[/animal/fish/*=4, /animal/*=5, /=8]",
                          "Dir matches");
            t.checkEquals(p.getMatches("/").toString(),
                          "[/=8]",
                          "Dir matches");
            t.checkEquals(p.getMatches("").toString(),
                          "[/=8]",
                          "Dir matches");

            t.checkEquals(p.pathMatch("/Foo/bar","/Foo/bar"),"/Foo/bar","pathMatch exact");
            t.checkEquals(p.pathMatch("/Foo/*","/Foo/bar"),"/Foo","pathMatch prefix");
            t.checkEquals(p.pathMatch("/Foo/*","/Foo/"),"/Foo","pathMatch prefix");
            t.checkEquals(p.pathMatch("/Foo/*","/Foo"),"/Foo","pathMatch prefix");
            t.checkEquals(p.pathMatch("*.ext","/Foo/bar.ext"),"/Foo/bar.ext","pathMatch suffix");
            t.checkEquals(p.pathMatch("/","/Foo/bar.ext"),"","pathMatch default");
            
            t.checkEquals(p.pathInfo("/Foo/bar","/Foo/bar"),null,"pathInfo exact");
            t.checkEquals(p.pathInfo("/Foo/*","/Foo/bar"),"/bar","pathInfo prefix");
            t.checkEquals(p.pathInfo("/Foo/*","/Foo/"),"/","pathInfo prefix");
            t.checkEquals(p.pathInfo("/Foo/*","/Foo"),null,"pathInfo prefix");
            t.checkEquals(p.pathInfo("*.ext","/Foo/bar.ext"),null,"pathInfo suffix");
            t.checkEquals(p.pathInfo("/","/Foo/bar.ext"),"/Foo/bar.ext","pathInfo default");
            t.checkEquals(p.getMatch("/XXX").getValue(),"9",
                          "multi paths");
            t.checkEquals(p.getMatch("/YYY").getValue(),"9",
                          "multi paths");
            
            p.put("/*","0");

            t.checkEquals(p.get("/abs/path"),"1","Get absolute path");
            t.checkEquals(p.getMatch("/abs/path").getKey(),"/abs/path",
                          "Match absolute path");
            t.checkEquals(p.getMatch("/abs/path").getValue(),"1",
                          "Match absolute path");
            t.checkEquals(p.getMatch("/abs/path/xxx").getValue(),"0",
                          "Mismatch absolute path");
            t.checkEquals(p.getMatch("/abs/pith").getValue(),"0",
                          "Mismatch absolute path");
            t.checkEquals(p.getMatch("/abs/path/longer").getValue(),"2",
                          "Match longer absolute path");
            t.checkEquals(p.getMatch("/abs/path/").getValue(),"0",
                          "Not exact absolute path");
            t.checkEquals(p.getMatch("/abs/path/xxx").getValue(),"0",
                          "Not exact absolute path");
            
            t.checkEquals(p.getMatch("/animal/bird/eagle/bald").getValue(),"3",
                          "Match longest prefix");
            t.checkEquals(p.getMatch("/animal/fish/shark/grey").getValue(),"4",
                          "Match longest prefix");
            t.checkEquals(p.getMatch("/animal/insect/bug").getValue(),"5",
                          "Match longest prefix");
            t.checkEquals(p.getMatch("/animal").getValue(),"5",
                          "mismatch exact prefix");
            t.checkEquals(p.getMatch("/animal/").getValue(),"5",
                          "mismatch exact prefix");
            
            t.checkEquals(p.getMatch("/suffix/path.tar.gz").getValue(),"0",
                          "Match longest suffix");
            t.checkEquals(p.getMatch("/suffix/path.gz").getValue(),"0",
                          "Match longest suffix");
            t.checkEquals(p.getMatch("/animal/path.gz").getValue(),"5",
                          "prefix rather than suffix");
            
            t.checkEquals(p.getMatch("/Other/path").getValue(),"0",
                          "default");
            
            t.checkEquals(p.pathMatch("/*","/xxx/zzz"),"","pathMatch /*");
            t.checkEquals(p.pathInfo("/*","/xxx/zzz"),"/xxx/zzz","pathInfo /*");
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
        try
        {
            chunkInTest();
            chunkOutTest();
            chunkingOSTest();
            filters();
            httpFields();
            pathMap();
            
            TestRequest.test();
            TestRFC2616.test();
        }
        catch(Throwable e)
        {
            Code.warning(e);
            new TestCase("org.mortbay.http.TestHarness").check(false,e.toString());
        }
        finally
        {
            TestCase.report();
        }
    }
}
