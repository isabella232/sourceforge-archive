// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;
import com.mortbay.Base.*;
import com.mortbay.Util.IO;
import java.sql.*;
import java.util.*;

/** Test harness for com.mortbay.JDBC
 * @version $Id$
 * @author Greg Wilkins
*/
class TestHarness
{    
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
	Column.test();
	ColumnGroup.test();
	Key.test();
	
	Test t = new Test("com.mortbay.JDBC.CreateTable");
	try{
	    Process process = Runtime.getRuntime().exec("make msql");
	    IO.copyThread(process.getErrorStream(),
			  System.err);
	    IO.copyThread(process.getInputStream(),
			  System.out);

	    process.waitFor();
	    t.check(true,"Build TestDB");
	}
	catch (Exception e){
	    Code.debug(e);
	    t.check(false,"Build TestDB");
	}

	TestTable.test();
	MultiTestTable.test();
	
	t.report();
    }
};










