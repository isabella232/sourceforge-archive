// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;

import com.mortbay.Base.*;

import java.util.*;
import java.sql.*;


/* ---------------------------------------------------------------- */
/** A specialized Column group representing a Table Key
 * <p> A Key is a ColumnGroup where all columns are Keys of a table.
 * The main extra ability of a Key ColumnGroup is to be passed to
 * methods on Table.
 *
 * <p><h4>Notes</h4>
 *
 * <p><h4>Usage</h4>
 * <pre>
 * </pre>
 *
 * @version $Id$
 * @author Jan Bartel
 */
public class Key extends ColumnGroup
{
    /* ------------------------------------------------------------ */
    final boolean primary;

    /* ------------------------------------------------------------ */
    public Key(String name, Column[] columns)
	 throws NonKeyException
    {
	super(name,columns);

	boolean p=true;
	for (int i=0;i<columns.length;i++)
	{
	    if (!columns[i].isKey())
		throw new NonKeyException(columns[i]);
	    if (!columns[i].isPrimary())
		p=false;
	}
	primary=p;
	
	Code.debug("Created : Key "+name+
		   " as " +
		   super.toString());
    }

    /* ------------------------------------------------------------ */
    public boolean isPrimary()
    {
	return primary;
    }

    /* ------------------------------------------------------------ */
    static void test()
    {
	Column NameCol =
	    new Column("name","User Name",Column.CHAR(20),Column.PRIMARY);
	Column FullNameCol =
	    new Column("full","Full Name",Column.CHAR(40),Column.KEY);
	Column HeightCol =
	    new Column("height","Height cm",Column.INT,Column.NONE);
	Column[] cols1 = {NameCol,FullNameCol,HeightCol};
	Column[] cols2 = {NameCol,FullNameCol};
	Column[] cols3 = {NameCol};


	
	Test t = new Test("com.mortbay.JDBC.Key");
	try{
	    new Key("Test1",cols1);
	    t.check(false,"NonKeyException");
	}
	catch (NonKeyException nke){
	    t.check(true,"NonKeyException");
	    t.checkEquals(nke.column,HeightCol,"HeightCol is not key");
	}
	
	try{
	    Key k2 = new Key("Test2",cols2);
	    t.check(!k2.isPrimary(),"Non primary key");
	
	    Key k3 = new Key("Test3",cols3);
	    t.check(k3.isPrimary(),"Primary key");
	}
	catch (NonKeyException nke){
	    t.check(false,"unexpected NonKeyException");
	}
    }
    
    
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
	test();
	Test.report();
    }
}
