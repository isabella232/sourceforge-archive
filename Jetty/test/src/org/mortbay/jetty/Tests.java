// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty;


import junit.framework.*;


public class Tests extends junit.framework.TestCase
{
    public Tests(String name) 
    {
      super(name);
    }
    
    public static junit.framework.Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(TestServer.suite());
        return suite;                  
    }
    
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }    
    
}
