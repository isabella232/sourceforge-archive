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
        try
        {    
            Column.test();
            ColumnGroup.test();
            Key.test();
        
            Database db = new Database("com.mortbay.JDBC.CloudscapeAdaptor",
                                       "jdbc:cloudscape:/tmp/TestDB");
            TestTable.test(db);
            MultiTestTable.test(db);
        }
        catch(Exception e)
        {
            Code.warning(e);
            Test t = new Test("com.mortbay.JDBC");
            t.check(false,e.toString());
        }
        
        Test.report();
    }
};










