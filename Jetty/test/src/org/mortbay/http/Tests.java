// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import junit.framework.TestSuite;


/* ------------------------------------------------------------ */
/** Util meta JUnitTestHarness.
 * @version $Id$
 * @author Juancarlo A�ez <juancarlo@modelistica.com>
 */
public class Tests extends junit.framework.TestCase
{
    public Tests(String name)
    {
      super(name);
    }

    public static junit.framework.Test suite() {
      return new TestSuite(Tests.class);
    }

    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }

    public void testISODate()
    {
      System.err.println(HttpFields.formatDate(System.currentTimeMillis(),  true));
      System.err.println(HttpFields.formatDate(System.currentTimeMillis(), false));

      assertEquals("Thu, 01 Jan 1970 00:00:00 GMT",HttpFields.formatDate(0,false));
      assertEquals("Thu, 01-Jan-70 00:00:01 GMT",HttpFields.formatDate(1000,true));

      assertEquals("Thu, 01 Jan 1970 00:01:00 GMT",HttpFields.formatDate(60000,false));
      assertEquals("Thu, 01-Jan-70 00:01:01 GMT",HttpFields.formatDate(61000,true));
      
      
      assertTrue(true);
    }
}
