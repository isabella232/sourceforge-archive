/** $Id$ */
// (c)1999 Transparent Language, Inc.


package org.mortbay.tools;

import java.io.FileReader;
import java.io.BufferedReader;
import java.util.*;
import java.net.*;
import java.io.*;
import org.mortbay.util.*;
import org.mortbay.http.*;

/* ------------------------------------------------------------ */
/** Stress test a WWW server. 
 * (c)1999 Transparent Language, Inc.
 * @version $Id$
 * @author Kent Johnson <kjohnson@transparent.com>
 * @author Juancarlo Añez <juancarlo@modelistica.com>
 * @author Greg Wilkins <gregw@mortbay.com>
 */
public class  StressTester implements Runnable
{
    Random random = new Random(this.hashCode());

    ArrayList _modes = new ArrayList();
    ArrayList _requests = new ArrayList();
    
    int _loops=0;
    String _host;
    int _port;
    int _grandConnects=0;
    int _grandRequests=0;
    int _grandFailures=0;
    int _totalConnects=0;
    int _totalRequests=0;
    int _totalFailures=0;
    long _totalBytes=0;
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 4) 
            usage();
        
        new StressTester(args[0],Integer.parseInt(args[1]))
            .stress(Integer.parseInt(args[2]),
                    Integer.parseInt(args[3]));
    }

    static void usage()
    {
        System.err.println("StressTester -- stress a WWW server");
        System.err.println("usage:");
        System.err.println("java org.mortbay.jetty.StressTester <host> <port> <n-threads> <n-loops>");
        System.err.println();
        System.exit(1);
    }


    public StressTester(String host,int port)
        throws Exception
    {
        _host=host;
        _port=port;
        
        BufferedReader b =
            new BufferedReader(new InputStreamReader(StressTester.class.getClassLoader()
                                                     .getResourceAsStream("test-modes.urls")));
        
        String mode;
        while( (mode = b.readLine()) != null)
        {
            _modes.add(mode);

            BufferedReader bf =
                new BufferedReader(new InputStreamReader(StressTester.class.getClassLoader()
                                                         .getResourceAsStream("test-files.urls")));
            ArrayList files=new ArrayList();
            _modes.add(files);
            String f;

            String[] conditions = 
                {
                    "",
                    "If-Modified-Since: Sat, 1 Jan 2000 00:00:00 GMT\r\n",
                    "If-Modified-Since: Sat, 1 Jan 2000 00:00:00 GMT\r\n",
                    "If-Modified-Since: Sat, 1 Jan 2000 00:00:00 GMT\r\n",
                    "If-Modified-Since: Thu, 1 Jan 1970 00:00:00 GMT\r\n",
                    "If-Modified-Since: Fri, 1 Jan 2100 12:00:00 GMT\r\n",
                    "If-Modified-Since: 1-Jan-00 00:00:00\r\n",
                };
            int condition=0;
            while( (f = bf.readLine()) != null)
            {
                String url="/stresstest"+mode+f;
                
                String request=
                    "GET "+url+" HTTP/1.1\r\n"+
                    "Host: "+host+":"+port+"\r\n"+
                    "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.1) Gecko/20020913 Debian/1.1-1\r\n"+
                    "Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,text/css,*/*;q=0.1\r\n"+
                    "Accept-Encoding: gzip\r\n"+
                    "Accept-Charset: ISO-8859-1, utf-8;q=0.66, *;q=0.66\r\n"+
                    "Authorization: Basic dXNlcjpwYXNzd29yZA==\r\n"+
                    "Cookie: JSESSION_ID=nosuchid\r\n"+
                    conditions[condition++%conditions.length]+
                    "\r\n";
                byte[] bytes = request.getBytes();
                files.add(bytes);
            }
            
            BufferedReader bs =
                new BufferedReader(new InputStreamReader(StressTester.class.getClassLoader()
                                                         .getResourceAsStream("test-servlets.urls")));
            ArrayList servlets=new ArrayList();
            _modes.add(servlets);
            String s;
            while( (s=bs.readLine()) != null)
            {
                String url="/stresstest"+mode+s;
                
                String request=
                    "GET "+url+" HTTP/1.1\r\n"+
                    "Host: "+host+":"+port+"\r\n"+
                    "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.1) Gecko/20020913 Debian/1.1-1\r\n"+
                    "Accept: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,video/x-mng,image/png,image/jpeg,image/gif;q=0.2,text/css,*/*;q=0.1\r\n"+
                    "Accept-Encoding: gzip\r\n"+
                    "Accept-Charset: ISO-8859-1, utf-8;q=0.66, *;q=0.66\r\n"+
                    "Cookie: Name=Value\r\n"+
                    "Authorization: Basic dXNlcjpwYXNzd29yZA==\r\n"+
                    "If-Modified-Since: 1 Jan 2000 12:00:00 GMT\r\n"+
                    "\r\n";
                byte[] bytes = request.getBytes();
                servlets.add(bytes);
            }    
        }
    }
    
    
    void stress(int nthreads, int loops)
        throws Exception 
    {

        // warm up
        _loops=1;

        for (int m=0;m<_modes.size();)
        {
            String mode=(String)_modes.get(m++);
            
            for(int i=0;i<2;i++)
            {
                _requests=(ArrayList)_modes.get(m++);
                
                System.err.println("MODE: "+mode+
                                 (i==0?"/file":"/servlet")+
                                 "\tWARMUP...");                
            
                Thread[] threads = new Thread[nthreads];
                for(int it = 0; it < nthreads; it++ )
                {
                    threads[it] = new Thread(this);
                    threads[it].setName("stress-"+it);
                }
        
                for(int it = 0; it < threads.length; it++)
                    threads[it].start();
                
                for(int it = 0; it < threads.length; it++)
                    threads[it].join();
            }
        }
        
        // actually measure
        _loops=loops;
        
        long gstart = System.currentTimeMillis();
        for (int m=0;m<_modes.size();)
        {
            String mode=(String)_modes.get(m++);
            
            for(int i=0;i<2;i++)
            {
                _requests=(ArrayList)_modes.get(m++);
                _totalConnects=0;
                _totalRequests=0;
                _totalFailures=0;
                
                System.err.print("MODE: "+mode+
                                 (i==0?"/file":"/servlet")+
                                 "\t");
                System.err.flush();
                
            
                Thread[] threads = new Thread[nthreads];
                for(int it = 0; it < nthreads; it++ )
                {
                    threads[it] = new Thread(this);
                    threads[it].setName("stress-"+it);
                }
        
                long start = System.currentTimeMillis();
                for(int it = 0; it < threads.length; it++)
                    threads[it].start();
                
                for(int it = 0; it < threads.length; it++)
                    threads[it].join();
                long end = System.currentTimeMillis();
                
                System.err.print("CONNECTS="+_totalConnects+
                                   "("+
                                   ((_totalConnects*1000)/(end-start))+
                                   "/sec) ");
                
                System.err.print("REQUESTS="+_totalRequests+
                                   "("+
                                   ((_totalRequests*1000)/(end-start))+
                                   "/sec) ");
                
                System.err.println("FAILURES="+_totalFailures+
                                   "("+
                                   ((_totalFailures*1000)/(end-start))+
                                   "/sec)");

                _grandConnects+=_totalConnects;
                _grandRequests+=_totalRequests;
                _grandFailures+=_totalFailures;
                
            }
        }
        long gend = System.currentTimeMillis();
        System.err.println();
        System.err.print("GRAND TOTAL:  CONNECTS="+_grandConnects+
                         "("+
                         ((_grandConnects*1000)/(gend-gstart))+
                         "/sec) ");
            
        System.err.print("REQUESTS="+_grandRequests+
                         "("+
                         ((_grandRequests*1000)/(gend-gstart))+
                         "/sec) ");
        
        System.err.println("FAILURES="+_grandFailures+
                           "("+
                           ((_grandFailures*1000)/(gend-gstart))+
                           "/sec)");
    }
    
    
    /* ------------------------------------------------------------ */
    public void run()
    {  
        HttpFields header = new HttpFields();
        String resLine=null;
        int connects=0;
        int requests=0;
        int failures=0;
        
        int i=0;
        int j=0;
        try
        {         
            for (i=0;i<_loops;i++)
            {
                
                Socket socket = new Socket(_host,_port);
                socket.setSoTimeout(10000);
                InputStream in = socket.getInputStream();
                LineInput lin = new LineInput(in);
                ChunkableInputStream cin = new ChunkableInputStream(lin,4096);
                OutputStream out = socket.getOutputStream();

                connects++;
                
                for (j=0;j<_requests.size();j++)
                {
                    header.clear();
                    resLine=null;
                    requests++;
                    out.write((byte[])_requests.get(j));
                    out.flush();
                    
                    resLine = lin.readLine();
                    if (resLine==null)
                    {
                        System.err.println("No response");
                        failures++;
                        break;
                    }
                    
                    while (resLine.indexOf(" 100")>0)
                        resLine = lin.readLine();

                    // check header
                    if (resLine.indexOf(" 40")>0 ||
                        resLine.indexOf(" 50")>0)
                    {
                        System.err.println(resLine);
                        failures++;
                    }
                    
                    // Read Response lne
                    header.read(lin);

//                     System.err.println(resLine);
//                     System.err.println(header);
                    
                    String te=header.get(HttpFields.__TransferEncoding);
                    int cl=header.getIntField(HttpFields.__ContentLength);
                    String ct=header.get(HttpFields.__ContentType);
                    String cn=header.get(HttpFields.__Connection);
                    
                    if ("chunked".equalsIgnoreCase(te))
                    {
                        cin.setChunking();
                        IO.copy(cin,IO.getNullStream());
                    }
                    else if (cl>=0)
                    {
                        IO.copy(cin,IO.getNullStream(),cl);
                    }
                    else if (ct!=null && ct.length()>0 && resLine.indexOf(" 20")>0)
                    {
                        IO.copy(cin,IO.getNullStream());
                    }
                    cin.resetStream();

                    if (cn!=null && cn.equalsIgnoreCase("close"))
                        break;
                }

                socket.close();
                socket=null;
            }
            
//             System.err.println(Thread.currentThread()+
//                                " connects="+connects+
//                                " requests="+requests+
//                                " failures="+failures);
        }
        catch (Exception e)
        {
            failures++;
            System.err.println("loop="+i+" request="+j);
            System.err.println(resLine);
            e.printStackTrace();
        }
        
        synchronized(this)
        {
            _totalConnects+=connects;
            _totalRequests+=requests;
            _totalFailures+=failures;
        }
    }
}


