// ========================================================================
// Copyright 2000 (c) Mortbay Consulting Ltd.
// $Id$
// ========================================================================

package org.mortbay.servlets.packageindex;

import org.mortbay.util.TestCase;

public class PackageVersionOrdererTest 
{
    /* ------------------------------------------------------------ */
    /** Main for Test
     * @param argv 
     */
    public static void main(String argv[]) {
	TestCase test = new TestCase("comparison");
	PackageVersionOrderer pvo = new PackageVersionOrderer();
	test.checkEquals(pvo.compare("1", "2"), -1, "1 < 2");
	test.checkEquals(pvo.compare("2", "1"), 1, "2 > 1");
	test.checkEquals(pvo.compare("2", "2"), 0, "2 == 2");
	test.checkEquals(pvo.compare("1.A","1.3"), -1, "1.A < 1.3");
	test.checkEquals(pvo.compare("1.2","1.3"), -1, "1.2 < 1.3");
	test.checkEquals(pvo.compare("1.2","1.2.3"), -1, "1.2 < 1.2.3");
	test.checkEquals(pvo.compare("1.2.3.1","1.2.3"), 1, "1.2.3.1 > 1.2.3");
	test.checkEquals(pvo.compare("1.2.1", "1.2a.1"), -1,
			 "1.2.1 < 1.2a.1");
	test.checkEquals(pvo.compare("3.1.RC5", "3.1.2"), -1,
			 "3.1.RC5 < 3.1.2");
	test.checkEquals(pvo.compare("a", "1"), -1, "1 < a");
	test.report();
    }
    /* ------------------------------------------------------------ */
}
