// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.TestSuite;


/* ------------------------------------------------------------ */
/** Util meta Tests.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class IOTest extends junit.framework.TestCase
{
    public IOTest(String name)
    {
      super(name);
    }
    
    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite(IOTest.class);
        return suite;                  
    }

    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }    
    
    

    /* ------------------------------------------------------------ */
    public void testIO() throws InterruptedException
    {
        // Only a little test
        ByteArrayInputStream in = new ByteArrayInputStream
            ("The quick brown fox jumped over the lazy dog".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IO.copyThread(in,out);
        Thread.sleep(500);

        assertEquals( "copyThread",
                      out.toString(),
                      "The quick brown fox jumped over the lazy dog");
    }

}
