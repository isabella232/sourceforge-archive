// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================


package com.mortbay.JDBC;
import com.mortbay.Base.*;


public class MultiTestTable  extends Table
{
    /*
     * Column definitions for the table of Tracker Users
     */
    public static final Column NameCol =
    new Column("name","Full Name",Column.CHAR(40),Column.PRIMARY);

    public static final Column EmailCol =
    new Column("email","Email Address",Column.CHAR(40),Column.PRIMARY);

    public static final Column LoginCol =
    new Column("login","Login",Column.CHAR(40),Column.KEY);

    public static final Column PasswordCol =
    new Column("password","Password",Column.CHAR(40),Column.KEY);
    
    public static final Column HeightCol =
    new Column("height","Height cm",Column.INT,Column.NONE);
    
    public static final Column WeightCol =
    new Column("weight","Weight kg",Column.REAL,Column.NONE);



    /*
     * Collection of all columns
     */
    static Column[] columns =
    {
	NameCol, EmailCol, LoginCol, PasswordCol, HeightCol, WeightCol
    };

    /* ------------------------------------------------------------ */
    /**   MultiTestTable constructor
     */
    public MultiTestTable()
    {
	super ("MultiTestTable", columns, null);
    } 

    /* ------------------------------------------------------------ */
    public MultiTestTable(Database dtb)
    {
	super ("MultiTestTable", columns, dtb);
    }
    
    /* ------------------------------------------------------------ */
    static void test()
    {
	Test t = new Test("com.mortbay.JDBC.MultiTestTable");
	Database db=null;
	MultiTestTable table=null;
	try {	    
	    db = new Database("com.mortbay.JDBC.MsqlAdaptor",
			      "jdbc:msql://localhost:1114/TestDB");
	    t.check(true,"Open DB");
	    table = new MultiTestTable(db);
	    t.check(true,"New Table");
	    
	    Row r = table.newRow();
	    
	    t.check(true,"Created empty row");
	
	    if (r != null)
	    {
		Object[] values = 
		{"Peer Gynt","peter@wolf.com","peer","gynt",
		 new Integer(1),new Double(2.3)
		};

		r.set(table,values);  
		r.update ();
	    }
	    t.check(true,"added row values");
	
	    Object[] keyVal = new Object[2];
	    keyVal[0] = (Object)new String("Peer Gynt");
	    keyVal[1] = (Object)new String("peter@wolf.com");
	    t.check(true,"make a new key value");
    
	    Row rr = table.getRow (keyVal);
	    t.check(rr!=null,"retrieved a row");

	    if (rr != null)
	    {
		t.checkEquals(rr.get(table.LoginCol.getName()).toString(),
			      "peer","Retrieved login column");
		t.checkEquals(rr.get(table.PasswordCol.getName()).toString(),
			      "gynt","Retrieved password column");
	    }
	    
	}
	catch (Exception e)
	{
	    Code.warning(e);
	    t.check(false,"Exception");
	}
    }
}


