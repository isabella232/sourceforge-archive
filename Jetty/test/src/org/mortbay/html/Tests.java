// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.html;

import junit.framework.TestSuite;


/* ------------------------------------------------------------ */
/** Util meta JUnitTestHarness.
 * @version $Id$
 * @author Juancarlo Añez <juancarlo@modelistica.com>
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
