// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================


package com.mortbay.JDBC;
import com.mortbay.Base.*;
import java.sql.*;
import java.util.*;

class TestTable extends Table
{
    /* ------------------------------------------------------------ */
    static final String RED="RED",
                      GREEN="GREEN",
                       BLUE="BLUE";
    
    static final String[] Colors=
    {
	RED,GREEN,BLUE
    };

    /* ------------------------------------------------------------ */
    public static final Column NameCol =
    new Column("name","User Name",Column.CHAR(20),Column.PRIMARY);
    public static final Column FullNameCol =
    new Column("full","Full Name",Column.CHAR(40),Column.KEY);
    public static final Column HeightCol =
    new Column("height","Height cm",Column.INT,Column.NONE);
    public static final Column WeightCol =
    new Column("weight","Weight kg",Column.REAL,Column.NONE);
    public static final Column DateCol =
    new Column("dateTime","Date Time",Column.DATETIME,Column.NONE);
    public static final Column ColorCol =
    new Column("color","Fav Color",Column.ENUM(Colors),Column.NONE);
    public static final Column DescCol =
    new Column("descript","Description",Column.VARCHAR(80),Column.NONE);

    /* ------------------------------------------------------------ */
    static Column[] columns =
    {NameCol,FullNameCol,HeightCol,WeightCol,DateCol,ColorCol,DescCol};
    
    
    /* ------------------------------------------------------------ */
    /** Normal constructor
     */
    public TestTable(Database db)
    {
	super("TestTable",columns,db);
    }
    
    /* ------------------------------------------------------------ */
    static void test()
    {
	Test t = new Test("com.mortbay.JDBC.TestTable");
	Database db=null;
	Table table=null;
	try {	    
	    db = new Database("com.mortbay.JDBC.MsqlAdaptor",
			      "jdbc:msql://localhost:1114/TestDB");
	    t.check(true,"Open DB");
	    table = new TestTable(db);
	    t.check(true,"New Table");
	}
	catch(Exception e){
	    Code.warning("",e);
	    t.check(false,"Open DB");
	    t.check(false,"New Table");
	}

	try{
	    java.sql.Connection con1 = db.getConnection();
	    java.sql.Connection con2 = db.getConnection();
	    t.check(con1!=con2,"Got different connections");
	    java.sql.Connection c1 =
		((com.mortbay.JDBC.Connection)con1).getDelegate();
	    java.sql.Connection c2 =
		((com.mortbay.JDBC.Connection)con2).getDelegate();
	    t.check(c1!=c2,"Got different delegates");
	    con1=null;
	    Thread.sleep(1000);
	    System.runFinalization();
	    java.sql.Connection con3 = db.getConnection();
	    java.sql.Connection c3 =
		((com.mortbay.JDBC.Connection)con3).getDelegate();
	    t.checkEquals(c1,c3,"Recycled Connection on finalize");
	}
	catch(Exception e){
	    Code.warning("",e);
	    t.check(false,"Recycle");
	}

	
	try {
	    table.deleteRow("gregw");
	    table.deleteRow("mos");
	    table.deleteRow("mattw");
	    t.check(true,"Delete non existant");
	}
	catch(Exception e){
	    e.printStackTrace();
	    t.check(false,"Delete non existant");
	}

	try {    
	    Row row = table.newRow();
	    Object mosVals[] = 
	    { "mos",
	      "Michael O'sillyman",
	      new Integer(164),
	      new Double(104.2),
	      new java.util.Date(0),
	      "GREEN",
	      "Sys admin from HELL!"
	    };
	    
	    row.set(mosVals);
	    row.update();

	    row = table.newRow();
	    row.set(0,"gregw");
	    row.set(1,"Greg Wilkins");
	    row.set(2,"184");
	    row.set(3,"70.2");
	    row.set(5,"GREEN");
	    row.update();

	    t.check(true,"ran update commit");
	}
	catch(Exception e){
	    t.check(true,"ran update commit");
	}

	try {
	    Row row = table.getRow("mos");
	    t.check(row!=null,"Get row");
	    if (row!=null)
	    {
		t.checkEquals(row.get(0).toString(),"mos","Get row value 0");
		t.checkEquals(row.get(1).toString(),"Michael O'sillyman","Get row value 1");
		t.checkEquals(row.get(2).toString(),"164","Get row value 2");
		t.checkEquals(row.get("color").toString(),"GREEN","Get ENUM");
		t.check(row.get("dateTime") instanceof java.util.Date,
			"date is a Date");
	    }
	    
	}
	catch(Exception e){
	    e.printStackTrace();
	    t.check(false,"Get row");
	}

	try {
	    Row row = table.getRow("NonExists");
	    t.checkEquals(row,null,"Not exists");
	}
	catch(Exception e){
	    e.printStackTrace();
	    t.check(false,"Not exists");
	}
	
	try {
	    Row row = table.newRow();
	    row.set(0,"gregw");
	    row.set(1,"Stuff");
	    row.set(2,"1");
	    row.set(3,"2");
	    row.update();
	    t.check(false,"Duplicate");
	}
	catch(Exception e){
	    t.checkContains(e.toString(),"Non unique","Duplicate");
	}

	try {
	    Row row = table.newRow();
	    row.set(0,"mattw");
	    row.set(3,"74.4");
	    row.set(5,"GREEN");
	    row.update();
	    
	    t.check(true,"Null fields");
	}
	catch(Exception e){
	    e.printStackTrace();
	    t.check(false,"Null fields");
	}

	try {
	    Row row = table.getRow("mos");
	    t.check(true,"Get row");
	    row.set(1,"Michael O'Sullivan");
	    row.update();
	    t.check(true,"Update called");

	    row = table.getRow("mos");
	    t.checkEquals(row.get(1).toString(),"Michael O'Sullivan","Get update value 1");
	}
	catch(Exception e){
	    e.printStackTrace();
	    t.check(false,"Update");
	}
	
	
	try {
	    Column[] colorGroup = {ColorCol};
	    Object[] colorVal = {"GREEN"};
	    ColumnGroup cg = new ColumnGroup("colorGrp",colorGroup);
	    RowEnumeration re = table.getRows(cg,colorVal);
	    t.check(re.hasMoreElements(),"RowEnumeration");
	    Row row = re.nextRow();
	    t.check(re.hasMoreElements(),"RowEnumeration");
	    row = re.nextRow();
	    t.check(re.hasMoreElements(),"RowEnumeration");
	    row = re.nextRow();
	    t.check(!re.hasMoreElements(),"RowEnumeration");
	    
	    table.deleteRow("mos");
	    
            re = table.getRows(HeightCol.toClause("<","150"));
	    t.check(re.hasMoreElements(),"RowEnumeration");
	    row = re.nextRow();
	    t.check(!re.hasMoreElements(),"RowEnumeration");
	    
	    table.deleteRows(cg,colorVal);
	    re = table.getRows(ColorCol.toClause("=","GREEN"));
	    t.check(!re.hasMoreElements(),"DeleteRows");
	}
	catch(Exception e){
	    Code.debug("",e);
	    t.checkContains(e.toString(),"mSQL","Rollback not supported");
	}
    }

    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
	try{
	    Database db = new Database("com.mortbay.JDBC.MsqlAdaptor",
				       "jdbc:msql://localhost:4333/TestDB");
	    Table table = new TestTable(db);
	    System.out.println(table.create());
	}
	catch(Exception e){
	    e.printStackTrace();
	}
    }
};
