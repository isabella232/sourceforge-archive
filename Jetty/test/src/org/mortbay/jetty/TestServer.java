// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestSuite;

public class TestServer extends junit.framework.TestCase
{
    File home;
    
    public TestServer(String name)
        throws IOException
    {
        super(name);

        File test=new File("./test");
        if (!test.exists())
            test=new File("../test");
        home= new File(new File(test.getParent()).getCanonicalPath());
        System.setProperty("jetty.home",home.toString());
        System.err.println("jetty.home="+home);
    }

    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }
    
    /* ------------------------------------------------------------ */
    public static junit.framework.Test suite()
    {
        return new TestSuite(TestServer.class);
    }

    /* ------------------------------------------------------------ */
    protected void setUp()
        throws Exception
    {
    }
    
    /* ------------------------------------------------------------ */
    protected void tearDown()
        throws Exception
    {
    }    

    /* ------------------------------------------------------------ */
    public void testServer()
        throws Exception
    {
        Server server = new Server(new File(home,"etc/demo.xml").toString());
        server.start();
        assertTrue("started",server.isStarted());

        File tmp = File.createTempFile("JettyServer",".serialized");
        ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(tmp));
        oo.writeObject(server);
        oo.flush();
        oo.close();
        assertTrue("serialized",tmp.exists());

        server.stop();
        assertTrue("stopped",!server.isStarted());
        server.destroy();

        ObjectInputStream oi = new ObjectInputStream(new FileInputStream(tmp));
        server = (Server)oi.readObject();
        oi.close();
        server.start();
        assertTrue("restarted",server.isStarted());
        server.stop();
        assertTrue("restopped",!server.isStarted());
        server.destroy();
    }
    
}
