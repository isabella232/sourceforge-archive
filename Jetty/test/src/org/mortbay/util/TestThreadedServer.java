// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import junit.framework.TestSuite;

public class TestThreadedServer extends junit.framework.TestCase
{
    TestServer server;
        
    public TestThreadedServer(String name)
    {
        super(name);
    }

    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }
    
    /* ------------------------------------------------------------ */
    public static junit.framework.Test suite()
    {
        return new TestSuite(TestThreadedServer.class);
    }

    /* ------------------------------------------------------------ */
    protected void setUp()
        throws Exception
    {
        server=new TestServer();
        server.start();
        Log.event("ThreadedServer test started");
        Thread.sleep(500);
    }

    
    /* ------------------------------------------------------------ */
    protected void tearDown()
        throws Exception
    {
        server.stop();
    }
    

    /* ------------------------------------------------------------ */
    public void testThreadedServer()
        throws Exception
    {
        assertTrue("isStarted",server.isStarted());
        assertEquals("Minimum Threads",0,server._connections);
        assertEquals("Minimum Threads",0,server._jobs);
        assertEquals("Minimum Threads",2,server.getThreads());

        
        PrintWriter p1 = server.stream();
        Thread.sleep(250);
        assertEquals("New connection",1,server._connections);
        assertEquals("New connection",1,server._jobs);
        assertEquals("New connection",2,server.getThreads());
            

        PrintWriter p2 = server.stream();
        System.err.print(".");System.err.flush();
        Thread.sleep(250);
        assertEquals("New thread",2,server._connections);
        assertEquals("New thread",2,server._jobs);
        assertEquals("New thread",2,server.getThreads());
        Thread.sleep(250);
        assertEquals("Steady State",2,server._connections);
        assertEquals("Steady State",2,server._jobs);
        assertEquals("Steady State",2,server.getThreads());
        
        p1.print("Exit\015");
        p1.flush();
        Thread.sleep(250);
        assertEquals("exit job",2,server._connections);
        assertEquals("exit job",1,server._jobs);
        assertEquals("exit job",2,server.getThreads());

        p1 = server.stream();
        Thread.sleep(250);
        assertEquals("reuse thread",3,server._connections);
        assertEquals("reuse thread",2,server._jobs);

        // XXX - this needs to be reworked.
    }
    
    /* ------------------------------------------------------------ */
    static class TestServer extends ThreadedServer
    {
        int _jobs=0;
        int _connections=0;
        HashSet _sockets=new HashSet();
        
        /* -------------------------------------------------------- */
        TestServer()
            throws Exception
        {
            super(new InetAddrPort(8765));
            setMinThreads(2);
            setMaxThreads(4);
            setMaxIdleTimeMs(5000);
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
                    {
                        return;
                    }
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
    
    
}
