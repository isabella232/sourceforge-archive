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

    ArrayList _requests = new ArrayList();
    int _loops=0;
    String _host;
    int _port;
    int _totalConnects=0;
    int _totalRequests=0;
    int _totalFailures=0;
    long _totalBytes=0;
    
    public static void main(String[] args) throws Exception
    {
        if (args.length != 5) 
            usage();
        
        new StressTester(args[0],Integer.parseInt(args[1]),args[2])
            .stress(Integer.parseInt(args[3]),
                    Integer.parseInt(args[4]));
    }

    static void usage()
    {
        System.err.println("StressTester -- stress a WWW server");
        System.err.println("usage:");
        System.err.println("java org.mortbay.jetty.StressTester <host> <port> <URLFile> <n-threads> <n-loops>");
        System.err.println();
        System.exit(1);
    }


    public StressTester(String host,int port,String urlFile)
        throws Exception
    {
        _host=host;
        _port=port;
        FileReader f = new FileReader(urlFile);
        BufferedReader b = new BufferedReader(f);        
        String s;
        while( (s = b.readLine()) != null)
        {
            String request=
                "GET "+s+" HTTP/1.1\r\n"+
                "Host: "+host+":"+port+"\r\n"+
                "\r\n";
            byte[] bytes = request.getBytes();
            _requests.add(bytes);
        }
    }
    
    
    void stress(int nthreads, int loops)
        throws Exception 
    {
        _loops=loops;
        
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

        System.err.println("TOTAL CONNECTS = "+_totalConnects+
                           ",   "+
                           ((_totalConnects*1000)/(end-start))+
                           " connects/sec");
       
        System.err.println("TOTAL REQUESTS = "+_totalRequests+
                           ",   "+
                           ((_totalRequests*1000)/(end-start))+
                           " requests/sec");
        
        System.err.println("TOTAL FAILURES = "+_totalFailures+
                           ",   "+
                           ((_totalFailures*1000)/(end-start))+
                           " failures/sec");
    }

    
    /* ------------------------------------------------------------ */
    public void run()
    {  
        int requests=0;
        int connects=0;
        int failures=0;

        HttpFields header = new HttpFields();
        String resLine=null;
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
                    else if (ct!=null && ct.length()>0)
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
            
            System.err.println(Thread.currentThread()+
                               " connects="+connects+
                               " requests="+requests+
                               " failures="+failures);
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


