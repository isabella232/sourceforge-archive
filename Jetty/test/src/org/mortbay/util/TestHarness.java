// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import java.util.jar.JarFile;


/* ------------------------------------------------------------ */
/** Util meta TestHarness.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class TestHarness
{
    public final static String __CRLF = "\015\012";
    public static String __userDir =
        System.getProperty("user.dir",".");
    public static URL __userURL=null;
    private static String __relDir="";
    static
    {
        try{
            File file = new File(__userDir);
            file=new File(file.getCanonicalPath());
            __userURL=file.toURL();
            if (!__userURL.toString().endsWith("/util/"))
            {
                __userURL=new URL(__userURL.toString()+
                                  "test/src/org/mortbay/util/");
                FilePermission perm = (FilePermission)
                    __userURL.openConnection().getPermission();
                __userDir=new File(perm.getName()).getCanonicalPath();
                __relDir="test/src/org/mortbay/util/".replace('/',
                                                         File.separatorChar);
            }

//             System.err.println("User Dir="+__userDir);
//             System.err.println("Rel  Dir="+__relDir);
//             System.err.println("User URL="+__userURL);
            
        }
        catch(Exception e)
        {
            Code.fail(e);
        }
    }    

    

    /* ------------------------------------------------------------ */
    public static void testBlockingQueue()
        throws Exception
    {
        System.err.print("Testing BlockingQueue.");
        System.err.flush();
        final TestCase t = new TestCase("org.mortbay.util.BlockingQueue");

        final BlockingQueue bq=new BlockingQueue(5);
        t.checkEquals(bq.size(),0,"empty");
        bq.put("A");
        t.checkEquals(bq.size(),1,"size");
        t.checkEquals(bq.get(),"A","A");
        t.checkEquals(bq.size(),0,"size");
        bq.put("B");
        bq.put("C");
        bq.put("D");
        t.checkEquals(bq.size(),3,"size");
        t.checkEquals(bq.get(),"B","B");
        t.checkEquals(bq.size(),2,"size");
        bq.put("E");
        t.checkEquals(bq.size(),3,"size");
        t.checkEquals(bq.get(),"C","C");
        t.checkEquals(bq.get(),"D","D");
        t.checkEquals(bq.get(),"E","E");

        new Thread(new Runnable()
                   {
                       public void run(){
                           try{
                               Thread.sleep(1000);
                               System.err.print(".");
                               System.err.flush();
                               bq.put("F");
                           }
                           catch(InterruptedException e){}
                       }
                   }
                   ).start();  
        
        t.checkEquals(bq.get(),"F","F");
        t.checkEquals(bq.get(100),null,"null");
        
        bq.put("G1");
        bq.put("G2");
        bq.put("G3");
        bq.put("G4");
        bq.put("G5");
        
        new Thread(new Runnable()
                   {
                       public void run(){
                           try{
                               Thread.sleep(500);
                               System.err.print(".");
                               System.err.flush();
                               t.checkEquals(bq.get(),"G1","G1");
                           }
                           catch(InterruptedException e){}
                       }
                   }
                   ).start();  
        try{
            bq.put("G6",100);
            t.check(false,"put timeout");
        }
        catch(InterruptedException e)
        {
            t.checkContains(e.toString(),"Timed out","put timeout");
        }
        
        bq.put("G6");
        t.checkEquals(bq.get(),"G2","G2");
        t.checkEquals(bq.get(),"G3","G3");
        t.checkEquals(bq.get(),"G4","G4");
        t.checkEquals(bq.get(),"G5","G5");
        t.checkEquals(bq.get(),"G6","G6");
        t.checkEquals(bq.get(100),null,"that's all folks");
        System.err.println();
    }
    
    /* ------------------------------------------------------------ */
    // moved to JUnit testing
    // public static void testURI()
    
    /* ------------------------------------------------------------ */
    public static void testQuotedStringTokenizer()
    {
        TestCase test = new TestCase("org.mortbay.util.QuotedStringTokenizer");
        try
        {
            QuotedStringTokenizer tok;
            
            tok=new QuotedStringTokenizer
                ("aaa, bbb, 'ccc, \"ddd\", \\'eee\\''",", ");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.checkEquals(tok.nextToken(),"aaa","aaa");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.checkEquals(tok.nextToken(),"bbb","bbb");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.checkEquals(tok.nextToken(),"ccc, \"ddd\", 'eee'","quoted");
            test.check(!tok.hasMoreTokens(),"hasMoreTokens");
            test.check(!tok.hasMoreTokens(),"hasMoreTokens");
            
            tok=new QuotedStringTokenizer
                ("aaa, bbb, 'ccc, \"ddd\", \\'eee\\''",", ",false,true);
            test.checkEquals(tok.nextToken(),"aaa","aaa");
            test.checkEquals(tok.nextToken(),"bbb","bbb");
            test.checkEquals(tok.nextToken(),"'ccc, \"ddd\", \\'eee\\''","quoted");
            
            tok=new QuotedStringTokenizer
                ("aa,bb;\"cc\",,'dd',;'',',;','\\''",";,");
            test.checkEquals(tok.nextToken(),"aa","aa");
            test.checkEquals(tok.nextToken(),"bb","bb");
            test.checkEquals(tok.nextToken(),"cc","cc");
            test.checkEquals(tok.nextToken(),"dd","dd");
            test.checkEquals(tok.nextToken(),"","empty");
            test.checkEquals(tok.nextToken(),",;","delimiters");
            test.checkEquals(tok.nextToken(),"'","escaped");
            
            tok=new QuotedStringTokenizer
                ("xx,bb;\"cc\",,'dd',;'',',;','\\''",";,",true);
            test.checkEquals(tok.nextToken(),"xx","xx");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),"bb","bb");
            test.checkEquals(tok.nextToken(),";",";");
            test.checkEquals(tok.nextToken(),"cc","cc");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),"dd","dd");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),";",";");
            test.checkEquals(tok.nextToken(),"","empty");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),",;","delimiters");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),"'","escaped");
            
            tok=new QuotedStringTokenizer
                ("aaa;bbb,ccc;ddd",";");
            test.checkEquals(tok.nextToken(),"aaa","aaa");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.checkEquals(tok.nextToken(","),"bbb","bbb");
            test.checkEquals(tok.nextToken(),"ccc;ddd","ccc;ddd");
            
            test.checkEquals(tok.quote("aaa"," "),"aaa","no quote");
            test.checkEquals(tok.quote("a a"," "),"\"a a\"","quote");
            test.checkEquals(tok.quote("a'a"," "),"\"a'a\"","quote");
            test.checkEquals(tok.quote("a,a",","),"\"a,a\"","quote");
            test.checkEquals(tok.quote("a\\a",""),"\"a\\\\a\"","quote");
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }

    /* ------------------------------------------------------------ */
    /** 
     */
    static final void testLineInput()
    {
        TestCase test = new TestCase("org.mortbay.util.LineInput");
        try
        {
                
            String data=
                "abcd\015\012"+
                "E\012"+
                "\015"+
                "fghi";
            
            ByteArrayInputStream dataStream;
            LineInput in;
                
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            
            test.checkEquals(in.readLine(),"abcd","1 read first line");
            test.checkEquals(in.readLine(),"E","1 read line");
            test.checkEquals(in.readLine(),"","1 blank line");
            test.checkEquals(in.readLine(),"fghi","1 read last line");
            test.checkEquals(in.readLine(),null,"1 read EOF");
            test.checkEquals(in.readLine(),null,"1 read EOF again");

            int bs=7;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abcd","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fghi","1."+bs+" read last line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=6;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abcd","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fghi","1."+bs+" read last line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=5;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abcd","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fghi","1."+bs+" read last line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=4;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abcd","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fghi","1."+bs+" read last line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=3;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abc","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"d","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fgh","1."+bs+" read last line");
            test.checkEquals(in.readLine(),"i","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=2;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"ab","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"cd","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fg","1."+bs+" read last line");
            test.checkEquals(in.readLine(),"hi","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            char[] b = new char[8];
            test.checkEquals(in.readLine(b,0,8),4,"2 read first line");
            test.checkEquals(in.readLine(b,0,8),1,"2 read line");
            test.checkEquals(in.readLine(b,0,8),0,"2 blank line");
            test.checkEquals(in.readLine(b,0,8),4,"2 read last line");
            test.checkEquals(in.readLine(b,0,8),-1,"2 read EOF");
            test.checkEquals(in.readLine(b,0,8),-1,"2 read EOF again");

            
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            test.checkEquals(in.readLineBuffer().size,4,"3 read first line");
            test.checkEquals(in.readLineBuffer().size,1,"3 read line");
            test.checkEquals(in.readLineBuffer().size,0,"3 blank line");
            test.checkEquals(in.readLineBuffer().size,4,"3 read last line");
            test.checkEquals(in.readLineBuffer(),null,"3 read EOF");
            test.checkEquals(in.readLineBuffer(),null,"3 read EOF again");
            
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            test.checkEquals(in.readLineBuffer(2).size,2,"4 read first line");
            test.checkEquals(in.readLineBuffer(2).size,2,"4 read rest of first line");
            test.checkEquals(in.readLineBuffer(2).size,1,"4 read line");
            test.checkEquals(in.readLineBuffer(2).size,0,"4 blank line");
            test.checkEquals(in.readLineBuffer(2).size,2,"4 read last line");
            test.checkEquals(in.readLineBuffer(2).size,2,"4 read rest of last line");
            test.checkEquals(in.readLineBuffer(2),null,"4 read EOF");
            test.checkEquals(in.readLineBuffer(2),null,"4 read EOF again");

            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            in.setByteLimit(8);
            test.checkEquals(in.readLine(),"abcd","read first line");
            test.checkEquals(in.readLine(),"E","read line");
            test.checkEquals(in.readLine(),null,"read EOF");
            test.checkEquals(in.readLine(),null,"read EOF again");

            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            test.checkEquals(in.readLine(),"abcd","1 read first line");
            in.setByteLimit(0);
            test.checkEquals(in.skip(4096),0,"bytelimit==0");
            in.setByteLimit(-1);
            test.checkEquals(in.readLine(),"E","1 read line");
            test.checkEquals(in.readLine(),"","1 blank line");
            in.setByteLimit(1);
            test.checkEquals(in.skip(4096),1,"bytelimit==1");
            in.setByteLimit(-1);
            test.checkEquals(in.readLine(),"ghi","1 read last line");
            test.checkEquals(in.readLine(),null,"1 read EOF");
            test.checkEquals(in.readLine(),null,"1 read EOF again");

            String dataCR=
                "abcd\015"+
                "E\015"+
                "\015"+
                "fghi";
            dataStream=new ByteArrayInputStream(dataCR.getBytes());
            in = new LineInput(dataStream,5);
            test.checkEquals(in.readLine(),"abcd","CR read first line");
            test.checkEquals(in.readLine(),"E","CR read line");
            test.checkEquals(in.readLine(),"","CR blank line");
            test.checkEquals(in.readLine(),"fghi","CR read last line");
            test.checkEquals(in.readLine(),null,"CR read EOF");
            test.checkEquals(in.readLine(),null,"CR read EOF again");            
            
            String dataLF=
                "abcd\012"+
                "E\012"+
                "\012"+
                "fghi";
            dataStream=new ByteArrayInputStream(dataLF.getBytes());
            in = new LineInput(dataStream,5);
            test.checkEquals(in.readLine(),"abcd","LF read first line");
            test.checkEquals(in.readLine(),"E","LF read line");
            test.checkEquals(in.readLine(),"","LF blank line");
            test.checkEquals(in.readLine(),"fghi","LF read last line");
            test.checkEquals(in.readLine(),null,"LF read EOF");
            test.checkEquals(in.readLine(),null,"LF read EOF again");

            String dataCRLF=
                "abcd\015\012"+
                "E\015\012"+
                "\015\012"+
                "fghi";
            dataStream=new ByteArrayInputStream(dataCR.getBytes());
            in = new LineInput(dataStream,5);
            test.checkEquals(in.readLine(),"abcd","CRLF read first line");
            test.checkEquals(in.readLine(),"E","CRLF read line");
            test.checkEquals(in.readLine(),"","CRLF blank line");
            test.checkEquals(in.readLine(),"fghi","CRLF read last line");
            test.checkEquals(in.readLine(),null,"CRLF read EOF");
            test.checkEquals(in.readLine(),null,"CRLF read EOF again");
     

            String dataEOF=
                "abcd\015\012"+
                "efgh\015\012"+
                "ijkl\015\012";
            dataStream=new ByteArrayInputStream(dataEOF.getBytes());
            in = new LineInput(dataStream,14);
            test.checkEquals(in.readLine(),"abcd","EOF read first line");
            in.setByteLimit(6);
            test.checkEquals(in.readLine(),"efgh","EOF read second line");
            test.checkEquals(in.readLine(),null,"read EOF");
            in.setByteLimit(-1);
            test.checkEquals(in.readLine(),"ijkl","EOF read second line");
        
            String dataEOL=
                "abcdefgh\015\012"+
                "ijklmnop\015\012"+
                "12345678\015\012"+
                "87654321\015\012";
            
            dataStream=new PauseInputStream(dataEOL.getBytes(),11);
            in = new LineInput(dataStream,100);
            test.checkEquals(in.readLine(),"abcdefgh","EOL read 1");
            test.checkEquals(in.readLine(),"ijklmnop","EOL read 2");
            test.checkEquals(in.readLine(),"12345678","EOL read 3");
            test.checkEquals(in.readLine(),"87654321","EOL read 4");

            dataStream=new PauseInputStream(dataEOL.getBytes(),100);
            in = new LineInput(dataStream,11);
            test.checkEquals(in.readLine(),"abcdefgh","EOL read 1");
            test.checkEquals(in.readLine(),"ijklmnop","EOL read 2");
            test.checkEquals(in.readLine(),"12345678","EOL read 3");
            test.checkEquals(in.readLine(),"87654321","EOL read 4");
            
            dataStream=new PauseInputStream(dataEOL.getBytes(),50);
            in = new LineInput(dataStream,19);
            test.checkEquals(in.readLine(),"abcdefgh","EOL read 1");
            test.checkEquals(in.readLine(),"ijklmnop","EOL read 2");
            in.setByteLimit(5);
            test.checkEquals(in.readLine(),"12345","EOL read 3 limited");
            in.setByteLimit(-1);
            test.checkEquals(in.readLine(),"678","EOL read 4 unlimited");
            test.checkEquals(in.readLine(),"87654321","EOL read 5");

            for (int s=20;s>1;s--)
            {
                dataStream=new PauseInputStream(dataEOL.getBytes(),s);
                in = new LineInput(dataStream,100);
                test.checkEquals(in.readLine(),"abcdefgh",s+" read 1");
                test.checkEquals(in.readLine(),"ijklmnop",s+" read 2");
                test.checkEquals(in.readLine(),"12345678",s+" read 3");
                test.checkEquals(in.readLine(),"87654321",s+" read 4");
            }

        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }

    /* ------------------------------------------------------------ */
    private static class PauseInputStream extends ByteArrayInputStream
    {
        int size;
        int c;
        
        PauseInputStream(byte[] data,int size)
        {
            super(data);
            this.size=size;
            c=size;
        }
        
        public synchronized int read()
        {
            c--;
            if(c==0)
                c=size;
            return super.read();
        }
        
        /* ------------------------------------------------------------ */
        public synchronized int read(byte b[], int off, int len)
        {
            if (len>c)
                len=c;
            if(c==0)
            {
                Code.debug("read(b,o,l)==0");
                c=size;
                return 0;
            }
            
            len=super.read(b,off,len);
            if (len>=0)
                c-=len;
            return len;
        }

        /* ------------------------------------------------------------ */
        public int available()
        {   
            if(c==0)
            {
                Code.debug("available==0");
                c=size;
                return 0;
            }
            return c;
        }
    }
    
    /* ------------------------------------------------------------ */
    static class TestThreadPool extends ThreadPool
    {
        /* -------------------------------------------------------- */
        int _calls=0;
        int _waiting=0;
        String _lock="lock";
        
        /* -------------------------------------------------------- */
        TestThreadPool()
            throws Exception
        {
            setName("TestPool");
            setMinThreads(2);
            setMaxThreads(4);
            setMaxIdleTimeMs(500);
        }
        
        /* -------------------------------------------------------- */
        protected void handle(Object job)
            throws InterruptedException
        {
            synchronized(_lock)
            {
                _calls++;
                _waiting++;
            }
            synchronized(job)
            {
                Code.debug("JOB wait: ",job);
                job.wait();
                Code.debug("JOB wake: ",job);
            }
            synchronized(_lock)
            {
                _waiting--;
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    static void testThreadPool()
    {
        TestCase test = new TestCase("org.mortbay.util.ThreadPool");
        System.err.print("Testing ThreadPool.");System.err.flush();
        try
        {
            TestThreadPool pool = new TestThreadPool();
            test.check(true,"Constructed");
            pool.start();
            Thread.sleep(100);
            test.check(pool.isStarted(),"Started");
            test.checkEquals(pool.getThreads(),2,"Minimum Threads");
            test.checkEquals(pool._calls,0,"Minimum Threads");
            test.checkEquals(pool._waiting,0,"Minimum Threads");
            
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getThreads(),2,"Minimum Threads");
            test.checkEquals(pool._calls,0,"Minimum Threads");
            test.checkEquals(pool._waiting,0,"Minimum Threads");

            String j1="Job1";
            String j2="Job2";
            String j3="Job3";
            String j4="Job4";
            String j5="Job5";

            pool.run(j1);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),2,"Job1");
            test.checkEquals(pool._calls,1,"Job1");
            test.checkEquals(pool._waiting,1,"Job1");
            
            pool.run(j2);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"Job2");
            test.checkEquals(pool._calls,2,"Job2");
            test.checkEquals(pool._waiting,2,"Job2");

            pool.run(j3);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"Job3");
            test.checkEquals(pool._calls,3,"Job3");
            test.checkEquals(pool._waiting,3,"Job3");
            
            pool.run(j4);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"Job4");
            test.checkEquals(pool._calls,4,"Job4");
            test.checkEquals(pool._waiting,4,"Job4");
            
            pool.run(j5);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"Job5");
            test.checkEquals(pool._calls,4,"Job5");
            test.checkEquals(pool._waiting,4,"Job5");
            
            synchronized(j1){j1.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"max threads");
            test.checkEquals(pool._calls,5,"max threads");
            test.checkEquals(pool._waiting,4,"max threads");
            
            synchronized(j2){j2.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,3,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(1000);
            test.checkEquals(pool.getThreads(),4,"idle wait");
            test.checkEquals(pool._calls,5,"idle wait");
            test.checkEquals(pool._waiting,3,"idle wait");
            
            synchronized(j3){j3.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,2,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getThreads(),3,"idle death");
            test.checkEquals(pool._calls,5,"idle death");
            test.checkEquals(pool._waiting,2,"idle death");

            synchronized(j4){j4.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,1,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getThreads(),2,"idle death");
            test.checkEquals(pool._calls,5,"idle death");
            test.checkEquals(pool._waiting,1,"idle death");
            
            synchronized(j5){j5.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),2,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,0,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getThreads(),2,"min idle");
            test.checkEquals(pool._calls,5,"min idle");
            test.checkEquals(pool._waiting,0,"min idle");
            
            pool.run(j1);
            pool.run(j2);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"steady state");
            test.checkEquals(pool._calls,7,"steady state");
            test.checkEquals(pool._waiting,2,"steady state");
            synchronized(j2){j2.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            pool.run(j2);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"steady state");
            test.checkEquals(pool._calls,8,"steady state");
            test.checkEquals(pool._waiting,2,"steady state");
            synchronized(j1){j1.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            pool.run(j2);
            System.err.println(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"steady state");
            test.checkEquals(pool._calls,9,"steady state");
            test.checkEquals(pool._waiting,2,"steady state");
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }
        

    /* ------------------------------------------------------------ */
    static class TestThreadedServer extends ThreadedServer
    {
        int _jobs=0;
        int _connections=0;
        HashSet _sockets=new HashSet();
        
        /* -------------------------------------------------------- */
        TestThreadedServer()
            throws Exception
        {
            super(new InetAddrPort(8765));
            setMinThreads(2);
            setMaxThreads(4);
            setMaxIdleTimeMs(500);
            setMaxReadTimeMs(60000);
        }
        
        /* -------------------------------------------------------- */
        protected void handleConnection(InputStream in,OutputStream out)
        {
            try
            {
                synchronized(this.getClass())
                {
                    Code.debug("Connection ",in);
                    _jobs++;
                    _connections++;
                }
                
                String line=null;
                LineInput lin= new LineInput(in);
                while((line=lin.readLine())!=null)
                {
                    Code.debug("Line ",line);		    
                    if ("Exit".equals(line))
                        return;
                }
            }
            catch(Error e)
            {
                Code.ignore(e);
            }
            catch(Exception e)
            {
                Code.ignore(e);
            }
            finally
            {    
                synchronized(this.getClass())
                {
                    _jobs--;
                    Code.debug("Disconnect: ",in);
                }
            }
        }

        /* -------------------------------------------------------- */
        PrintWriter stream()
            throws Exception
        {
            InetAddrPort addr = new InetAddrPort();
            addr.setInetAddress(InetAddress.getByName("127.0.0.1"));
            addr.setPort(8765);
            Socket s = new Socket(addr.getInetAddress(),addr.getPort());
            _sockets.add(s);
            Code.debug("Socket ",s);
            return new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
        }    
    }
    
    /* ------------------------------------------------------------ */
    static void testThreadedServer()
    {
        TestCase test = new TestCase("org.mortbay.util.ThreadedServer");
        System.err.print("Testing ThreadedServer.");System.err.flush();
        try
        {
            TestThreadedServer server = new TestThreadedServer();
            server.setMaxReadTimeMs(5000);
            test.check(true,"Constructed");
            server.start();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.check(server.isStarted(),"Started");
            test.checkEquals(server._connections,0,"Minimum Threads");
            test.checkEquals(server._jobs,0,"Minimum Threads");
            test.checkEquals(server.getThreads(),2,"Minimum Threads");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.check(server.isStarted(),"Started");
            test.checkEquals(server._connections,0,"Minimum Threads");
            test.checkEquals(server._jobs,0,"Minimum Threads");
            test.checkEquals(server.getThreads(),2,"Minimum Threads");
            
            PrintWriter p1 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(200);
            test.checkEquals(server._connections,1,"New connection");
            test.checkEquals(server._jobs,1,"New connection");
            test.checkEquals(server.getThreads(),2,"New connection");
            
            PrintWriter p2 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(200);
            test.checkEquals(server._connections,2,"New thread");
            test.checkEquals(server._jobs,2,"New thread");
            test.checkEquals(server.getThreads(),3,"New thread");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(server._connections,2,"Steady State");
            test.checkEquals(server._jobs,2,"Steady State");
            test.checkEquals(server.getThreads(),3,"Steady State");

            p1.print("Exit\015");
            p1.flush();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            
            test.checkEquals(server._connections,2,"exit job");
            test.checkEquals(server._jobs,1,"exit job");
            test.checkEquals(server.getThreads(),3,"exit job");
            p1 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(server._connections,3,"reuse thread");
            test.checkEquals(server._jobs,2,"reuse thread");
            test.checkEquals(server.getThreads(),3,"reuse thread");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(server._connections,3,"1 idle");
            test.checkEquals(server._jobs,2,"1 idle");
            test.checkEquals(server.getThreads(),3,"1 idle");

            
            p1.print("Exit\015");
            p1.flush();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            
            test.checkEquals(server._connections,3,"idle thread");
            test.checkEquals(server._jobs,1,"idle thread");
            test.checkEquals(server.getThreads(),3,"idle thread");
            System.err.print(".");System.err.flush();
            Thread.sleep(800);
            test.checkEquals(server._connections,3,"idle death");
            test.checkEquals(server._jobs,1,"idle death");
            test.checkEquals(server.getThreads(),2,"idle death");
            
            
            p1 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(server._connections,4,"restart thread");
            test.checkEquals(server._jobs,2,"restart thread");
            test.checkEquals(server.getThreads(),3,"restart thread");
            
            PrintWriter p3 = server.stream();
            PrintWriter p4 = server.stream();
            System.err.println(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(server._connections,6,"max thread");
            test.checkEquals(server._jobs,4,"max thread");
            test.checkEquals(server.getThreads(),4,"max thread");

            server.stop();
            server.join();
            test.check(!server.isStarted(),"Stopped");
            test.checkEquals(server.getThreads(),0,"No Threads");
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }    

    /* ------------------------------------------------------------ */
    static void testSingletonList()
    {
        TestCase t = new TestCase("org.mortbay.util.SingletonList");
        
        try
        {
            Object o="X";
            SingletonList sl = SingletonList.newSingletonList(o);
            t.checkEquals(sl.size(),1,"SingletonList.size()");
            t.checkEquals(sl.get(0),o,"SingletonList.get(0)");
            Iterator i=sl.iterator();
            ListIterator li=sl.listIterator();
            t.check(i.hasNext(),"SingletonList.iterator().hasNext()");
            t.check(li.hasNext(),"SingletonList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"SingletonList.listIterator().hasPrevious()");

            t.checkEquals(i.next(),o,"SingletonList.iterator().next()");
            t.check(!i.hasNext(),"SingletonList.iterator().hasNext()");
            t.check(li.hasNext(),"SingletonList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"SingletonList.listIterator().hasPrevious()");
            
            t.checkEquals(li.next(),o,"SingletonList.listIterator().next()");
            t.check(!i.hasNext(),"SingletonList.iterator().hasNext()");
            t.check(!li.hasNext(),"SingletonList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"SingletonList.listIterator().hasPrevious()");
            
            t.checkEquals(li.previous(),o,"SingletonList.listIterator().previous()");
            t.check(!i.hasNext(),"SingletonList.iterator().hasNext()");
            t.check(li.hasNext(),"SingletonList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"SingletonList.listIterator().hasPrevious()");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    static void testLazyList()
    {
        TestCase t = new TestCase("org.mortbay.util.LazyList");
        
        try
        {
            LazyList list = null;
            Object o1="X1";
            Object o2="X2";

            // empty list
            List empty = LazyList.getList(list);
            t.checkEquals(empty.size(),0,"empty LazyList");

            // singleton list
            list=LazyList.add(list,o1);
            
            t.checkEquals(list.size(),1,"singleton LazyList.size()");
            t.checkEquals(list.get(0),o1,"singleton LazyList.get(0)");
            Iterator i=list.iterator();
            ListIterator li=list.listIterator();
            t.check(i.hasNext(),"singleton LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"singleton LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"singleton LazyList.listIterator().hasPrevious()");

            t.checkEquals(i.next(),o1,"singleton LazyList.iterator().next()");
            t.check(!i.hasNext(),"singleton LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"singleton LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"singleton LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.next(),o1,"singleton LazyList.listIterator().next()");
            t.check(!i.hasNext(),"singleton LazyList.iterator().hasNext()");
            t.check(!li.hasNext(),"singleton LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"singleton LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.previous(),o1,"singleton LazyList.listIterator().previous()");
            t.check(!i.hasNext(),"singleton LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"singleton LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"singleton LazyList.listIterator().hasPrevious()");


            // normal list
            list=LazyList.add(list,o2);
            
            t.checkEquals(list.size(),2,"normal LazyList.size()");
            t.checkEquals(list.get(0),o1,"normal LazyList.get(0)");
            t.checkEquals(list.get(1),o2,"normal LazyList.get(0)");
            i=list.iterator();
            li=list.listIterator();
            t.check(i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");

            t.checkEquals(i.next(),o1,"normal LazyList.iterator().next()");
            t.check(i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.next(),o1,"normal LazyList.listIterator().next()");
            t.check(i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");

            t.checkEquals(i.next(),o2,"normal LazyList.iterator().next()");
            t.check(!i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.next(),o2,"normal LazyList.listIterator().next()");
            t.check(!i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(!li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.previous(),o2,"normal LazyList.listIterator().previous()");
            t.check(!i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    static void testStringMap()
    {
        TestCase t = new TestCase("org.mortbay.util.StringMap");
        
        try
        {
            StringMap map = new StringMap();

            map.put("K1","V1");
            t.checkEquals(map.get("K1"),"V1","1V1");
            map.put("K2","V2");
            t.checkEquals(map.get("K1"),"V1","2V1");
            t.checkEquals(map.get("K2"),"V2","2V2");
            map.put("0","V0");
            t.checkEquals(map.get("0"),"V0","3V0");
            t.checkEquals(map.get("K1"),"V1","3V1");
            t.checkEquals(map.get("K2"),"V2","3V2");
            
            map.put("K03","V3");
            t.checkEquals(map.get("0"),"V0","4V0");
            t.checkEquals(map.get("K1"),"V1","4V1");
            t.checkEquals(map.get("K2"),"V2","4V2");
            t.checkEquals(map.get("K03"),"V3","4V3");
            t.checkEquals(map.get("???"),null,"4null");
            
            map.put("ABCD","V4");
            map.put("ABCDEFGH","V5");
            map.put("ABXX","V6");
            t.checkEquals(map.get("AB"),null,"nullAB");
            map.put("AB","V7");
            map.put("ABCDEF","V8");
            map.put("ABCDXXXX","V9");
            
            t.checkEquals(map.get("ABCD"),"V4","V4");
            t.checkEquals(map.get("ABCDEFGH"),"V5","V5");
            t.checkEquals(map.get("ABXX"),"V6","V6");
            t.checkEquals(map.get("AB"),"V7","V7");
            t.checkEquals(map.get("ABCDEF"),"V8","V8");
            t.checkEquals(map.get("ABCDXXXX"),"V9","V9");
            t.checkEquals(map.get("ABC"),null,"null1");
            t.checkEquals(map.get("AB?"),null,"null2");
            t.checkEquals(map.get("ABCDE"),null,"null3");
            t.checkEquals(map.get("ABCD?"),null,"null4");
            t.checkEquals(map.get("ABCDEFG"),null,"null5");
            t.checkEquals(map.get("ABCDEF?"),null,"null6");
            t.checkEquals(map.get("ABCDEFGHI"),null,"null7");
            t.checkEquals(map.get("ABCDEFGH?"),null,"null8");
            
            
            t.checkEquals(map.getEntry("x0x",1,1).getValue(),"V0","5V0");
            t.checkEquals(map.getEntry("xK1x",1,2).getValue(),"V1","5V1");
            t.checkEquals(map.getEntry("xK2x",1,2).getValue(),"V2","5V2");
            t.checkEquals(map.getEntry("xK03x",1,3).getValue(),"V3","5V3");
            t.checkEquals(map.getEntry("???",1,1),null,"5null");
            
            t.checkEquals(map.getEntry("xKx",1,1),null,"5K");
            
            t.checkEquals(map.getEntry("x0x".toCharArray(),1,1).getValue(),"V0","6V0");
            t.checkEquals(map.getEntry("xK1x".toCharArray(),1,2).getValue(),"V1","6V1");
            t.checkEquals(map.getEntry("xK2x".toCharArray(),1,2).getValue(),"V2","6V2");
            t.checkEquals(map.getEntry("xK03x".toCharArray(),1,3).getValue(),"V3","6V3");
            t.checkEquals(map.getEntry("???".toCharArray(),1,1),null,"6null");
            
            t.checkEquals(map.getEntry("x0x".getBytes(),1,1).getValue(),"V0","7V0");
            t.checkEquals(map.getEntry("xK1x".getBytes(),1,2).getValue(),"V1","7V1");
            t.checkEquals(map.getEntry("xK2x".getBytes(),1,2).getValue(),"V2","7V2");
            t.checkEquals(map.getEntry("xK03x".getBytes(),1,3).getValue(),"V3","7V3");
            t.checkEquals(map.getEntry("???".getBytes(),1,1),null,"7null");
            
            t.checkEquals(map.size(),10,"8size");
            t.checkEquals(map.get("0"),"V0","8V0");
            t.checkEquals(map.get("k1"),null,"8V1");
            t.checkEquals(map.get("k2"),null,"8V2");
            t.checkEquals(map.get("k03"),null,"8V3");
            t.checkEquals(map.get("???"),null,"8null");

            map.clear();
            map.setIgnoreCase(true);
            map.put("K1","V1");
            map.put("K2","V2");
            map.put("0","V0");
            map.put("K03","V3");
            map.put("ABCD","V4");
            map.put("ABCDEFGH","V5");
            map.put("ABXX","V6");
            map.put("AB","V7");
            map.put("ABCDEF","V8");
            map.put("ABCDXXXX","V9");
            
            t.checkEquals(map.size(),10,"9size");
            t.checkEquals(map.get("0"),"V0","9V0");
            t.checkEquals(map.get("k1"),"V1","9V1");
            t.checkEquals(map.get("k2"),"V2","9V2");
            t.checkEquals(map.get("k03"),"V3","9V3");
            t.checkEquals(map.get("???"),null,"9null");

            map.put(null,"Vn");
            t.checkEquals(map.size(),11,"10size");
            t.checkEquals(map.get("0"),"V0","10V0");
            t.checkEquals(map.get("k1"),"V1","10V1");
            t.checkEquals(map.get("k2"),"V2","10V2");
            t.checkEquals(map.get("k03"),"V3","10V3");
            t.checkEquals(map.get("???"),null,"10null");
            t.checkEquals(map.get(null),"Vn","10Vn");

            map.remove("XXX");
            t.checkEquals(map.size(),11,"11size5");
            map.remove("k2");
            t.checkEquals(map.size(),10,"11size4");
            map.remove(null);
            t.checkEquals(map.size(),9,"11size3");
            
            map.remove("AB");
            map.remove("ABCDXXXX");
            map.remove("ABCDEF");
            map.remove("ABCDEFGH");
            t.checkEquals(map.size(),5,"12size");
            
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }    
    
    /* ------------------------------------------------------------ */
    static void testMultiMap()
    {
        TestCase t = new TestCase("org.mortbay.util.MultiMap");
        
        try
        {
            MultiMap mm = new MultiMap();

            mm.put("K1","V1");
            t.checkEquals(mm.get("K1"),"V1","as Map");
            t.checkEquals(mm.getValues("K1").get(0),"V1","as List");
            mm.add("K1","V2");
            t.checkEquals(mm.getValues("K1").get(0),"V1","add List");
            t.checkEquals(mm.getValues("K1").get(1),"V2","add List");

            mm.put("K2",new Integer(2));
            t.checkEquals(mm.getValues("K2").get(0),new Integer(2),"as Object");

            MultiMap m2=(MultiMap)mm.clone();
            m2.add("K1","V3");
            
            t.checkEquals(mm.getValues("K1").size(),2,"unchanged List");
            t.checkEquals(mm.getValues("K1").get(0),"V1","unchanged List");
            t.checkEquals(mm.getValues("K1").get(1),"V2","unchanged List");
            t.checkEquals(m2.getValues("K1").get(0),"V1","clone List");
            t.checkEquals(m2.getValues("K1").get(1),"V2","clone List");
            t.checkEquals(m2.getValues("K1").get(2),"V3","clone List");
            t.checkEquals(m2.getValue("K1",0),"V1","clone List");
            t.checkEquals(m2.getValue("K1",1),"V2","clone List");
            t.checkEquals(m2.getValue("K1",2),"V3","clone List");            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }


    /* ------------------------------------------------------------ */
    public static void testJarURL()
    {
        TestCase t = new TestCase("org.mortbay.util.Zip");
        try
        {
            // Test jar update
            File tmpJar = File.createTempFile("test",".jar");
            tmpJar.deleteOnExit();
            
            URL jar1 = new URL(__userURL+"TestData/test.zip");
            System.err.println(jar1);
            IO.copy(jar1.openStream(),new FileOutputStream(tmpJar));
            URL url1 = new URL("jar:"+tmpJar.toURL()+"!/");
            JarURLConnection jc1 = (JarURLConnection)url1.openConnection();
            JarFile j1=jc1.getJarFile();
            System.err.println("T1:");
            Enumeration e = j1.entries();
            while(e.hasMoreElements())
                System.err.println(e.nextElement());
            
            
            URL jar2 = new URL(__userURL+"TestData/alt.zip");
            System.err.println(jar2);
            IO.copy(jar2.openStream(),new FileOutputStream(tmpJar));
            URL url2 = new URL("jar:"+tmpJar.toURL()+"!/");
            JarURLConnection jc2 = (JarURLConnection)url2.openConnection();
            JarFile j2=jc2.getJarFile();
            System.err.println("T2:");
            e = j2.entries();
            while(e.hasMoreElements())
                System.err.println(e.nextElement());
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
        try
        {
            testStringMap();
            testSingletonList();
            testLazyList();
            testMultiMap();
            testQuotedStringTokenizer();            
            testBlockingQueue();
            testLineInput();
            testThreadPool();
            testThreadedServer();

        }
        catch(Throwable th)
        {
            Code.warning(th);
            TestCase t = new TestCase("org.mortbay.util.TestHarness");
            t.check(false,th.toString());
        }
        finally
        {
            TestCase.report();
        }
    }
}
