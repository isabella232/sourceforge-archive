// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

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
      return new TestSuite(Tests.class);
    }

    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }    

    public void testPlaceHolder()
    {
      assertTrue(true);
    }
}
