// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.thread;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;

import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.LogSupport;

public class ThreadedServerTest extends junit.framework.TestCase
{
    static Log log = LogFactory.getLog(ThreadedServerTest.class);

    TestServer server;
        
    public ThreadedServerTest(String name)
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
        return new TestSuite(ThreadedServerTest.class);
    }

    /* ------------------------------------------------------------ */
    protected void setUp()
        throws Exception
    {
        server=new TestServer();
        server.start();
        log.info("ThreadedServer test started");
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

        // TODO - this needs to be reworked.
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
            super(8765);
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
                    if(log.isDebugEnabled())log.debug("Connection "+in);
                    _jobs++;
                    _connections++;
                }
                
                int c=0;
                int s=0;
                while((c=in.read())>=0)
                {	    
                    switch(c)
                    {
                        case 'E' : if (s==0) s++; break;
                        case 'x' : if (s==1) s++; break;
                        case 'i' : if (s==2) s++; break;
                        case 't' : if (s==3) return; break;
                        default :s=0;
                    }
                }
            }
            catch(Error e)
            {
                LogSupport.ignore(log,e);
            }
            catch(Exception e)
            {
                LogSupport.ignore(log,e);
            }
            finally
            {    
                synchronized(this.getClass())
                {
                    _jobs--;
                    if(log.isDebugEnabled())log.debug("Disconnect: "+in);
                }
            }
        }

        /* -------------------------------------------------------- */
        PrintWriter stream()
            throws Exception
        {
            InetAddress inetAddress = InetAddress.getByName("127.0.0.1");
            Socket s = new Socket(inetAddress,8765);
            _sockets.add(s);
            if(log.isDebugEnabled())log.debug("Socket "+s);
            return new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
        }    
    }
    
    
}
