// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay;

import junit.framework.*;


/* ------------------------------------------------------------ */
/** Util meta JUnitTestHarness.
 * @version $Id$
 * @author Juancarlo A&ntilde;ez (juanco)
 */
public class Tests extends junit.framework.TestCase
{
    public Tests(String name) 
    {
      super(name);
    }
    
    public static junit.framework.Test suite() {
      TestSuite suite = new TestSuite("Jetty tests");

      suite.addTest( org.mortbay.util.Tests.suite() );
      suite.addTest( org.mortbay.html.Tests.suite() );
      suite.addTest( org.mortbay.http.Tests.suite() );
      suite.addTest( org.mortbay.http.handler.Tests.suite() );
      suite.addTest( org.mortbay.xml.Tests.suite() );

      return suite;
    }

    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }    
}
