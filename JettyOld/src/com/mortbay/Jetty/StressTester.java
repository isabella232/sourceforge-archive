/** $Id$ */
// (c)1999 Transparent Language, Inc.


package com.mortbay.Jetty;

import java.io.FileReader;
import java.io.BufferedReader;
import java.util.*;
import java.net.*;
import java.io.*;

/* ------------------------------------------------------------ */
/** Stress test a WWW server. 
 * (c)1999 Transparent Language, Inc.
 * @version $Id$
 * @author Kent Johnson <kjohnson@transparent.com>
 * @author Juancarlo Añez <juancarlo@modelistica.com>
 * @author Greg Wilkins <gregw@mortbay.com>
 */
public class  StressTester
{
    int totalRequests=0;
    long totalBytes=0;
    
    public static void main(String[] args) throws Exception
    {
        if (args.length < 3) 
            usage();
        
        new StressTester().stress(args[0], args[1], args[2]);
    }

    static void usage() {
        System.err.println("StressTester -- stress a WWW server");
        System.err.println("usage:");
        System.err.println("java com.mortbay.Jetty.StressTester <n-threads> <baseURL> <URLFile>");
        System.err.println();
        System.exit(1);
    }


    void stress(String snthreads, String baseURL, String URLFile) 
        throws Exception 
    {
        FileReader f = new FileReader(URLFile);
        BufferedReader b = new BufferedReader(f);
      
        Vector v = new Vector();
        String s;
        while( (s = b.readLine()) != null) 
            v.addElement(s);

        String[] urls = new String[v.size()];
        v.copyInto(urls);

        int nthreads = Integer.parseInt(snthreads);

        Thread[] threads = new Thread[nthreads];
        for(int it = 0; it < nthreads; it++ ) {
            Runnable r = new URLGetter(Integer.toString(it) , baseURL, urls);
            threads[it] = new Thread(r);
        }

        long start = System.currentTimeMillis();
        for(int it = 0; it < threads.length; it++)
            threads[it].start();
        
        for(int it = 0; it < threads.length; it++)
            threads[it].join();
        long end = System.currentTimeMillis();
        
        System.err.println("TOTAL REQUESTS = "+totalRequests+
                           ",   "+
                           (totalRequests/((end-start)/1000))+
                           " requests/sec");
        System.err.println("TOTAL BYTES = "+totalBytes+
                           ",   "+
                           (totalBytes/((end-start)/1000))+
                           " bytes/sec");
        
      
    }



    /* ------------------------------------------------------------ */
    class URLGetter implements Runnable
    {

        String   _id;
        String   _baseURL;
        String[] _urls;
    
        /* ------------------------------------------------------------ */
        public URLGetter(String id, String baseURL, String[] urls) {
            this._id      = id;
            this._baseURL = baseURL;
            this._urls    = urls;
        }
    
        /* ------------------------------------------------------------ */
        public void run()
        {  
            Random random = new Random(this.hashCode());
            int bytes = 0;
            int requests = 0;
            for (int iurl = 5*_urls.length;iurl-->0;)
            {
                requests++;
                int n = new Float((_urls.length-1) * random.nextFloat()).intValue();
                String  url = _baseURL + _urls[n];
                try
                {
                    bytes += fetchURL(url);
                }
                catch (FileNotFoundException ignored) {}
                catch (Exception e) {
                    System.err.println(id() + " error [" + iurl + "] " + e.toString() + " : " + url);
                }
            }
            System.err.println(id() + " finished, " +
                               requests + " requests, " +
                               bytes + " bytes");

            synchronized(StressTester.this)
            {
                totalBytes+=bytes;
                totalRequests+=requests;
            }
        }


        /* ------------------------------------------------------------ */
        public int fetchURL(String inURL) throws Exception
        {
            int totalBytes = 0;
            URL u = new URL(inURL);
            URLConnection conn = u.openConnection();
                conn.setRequestProperty("Accept", "*/*");
                InputStream in = conn.getInputStream(); // Open the URL
                try
                {
                    totalBytes = flush(in);// Read and throw away the contents
                }
                finally
                {
                    in.close();
                }
                
                // Check for a successful connection
                HttpURLConnection httpConn = (HttpURLConnection)conn;
                if (httpConn.getResponseCode() >= 400)
                    throw new Exception(Integer.toString(httpConn.getResponseCode()));
           
            
            return totalBytes;
 
        }
 

        /* ------------------------------------------------------------ */
        int flush(InputStream in)
            throws IOException
        {
            int totalBytes = 0;
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer, 0, 4096)) != -1)
                totalBytes += bytesRead;
          
            return totalBytes;
        }


        /* ------------------------------------------------------------ */
        String id()
        {
            return this._id;
        }
 
    }   // URLGetter




    
}


