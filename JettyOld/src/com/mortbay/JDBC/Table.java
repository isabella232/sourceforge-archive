// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;
import com.mortbay.Base.*;
import java.sql.*;
import java.util.*;

/** JDBC Table wrapper
 * <p>  
 * The Table class holds Meta data about a JDBC table.  It is used
 * in preferance to JDBC metadata as: <UL>
 * <LI> JDBC MetaData is not always implemented
 * <LI> It can be used to programatically create tables and index's
 * <LI> Some degree of additional DB portability is obtained
 * </UL>
 *
 * @see com.mortbay.JDBC.Database
 * @version $Id$
 * @author Greg Wilkins
*/
public class Table extends ColumnGroup
{
    /* ------------------------------------------------------------ */
    final public Key primaryKey;
    final public ColumnGroup otherKeys; // Non primary keys
    final public ColumnGroup otherCols; // Non primary columns
    
    /* ------------------------------------------------------------ */
    private Database database;
    private DbAdaptor adaptor;
    
    /* ------------------------------------------------------------ */
    /** Table contructor
     * @parma name The name of the table
     * @param columns Array of column descriptors
     * @param database The database to use.
     */
    public Table(String name,
		 Column[] columns,
		 Database database)
    {
	super(name,columns);

	Code.assert(database!=null,"Constructed with null database");
	this.database=database;
	this.adaptor=database.getAdaptor();
	Code.assert(this.adaptor!=null,"Database with null adaptor");

	/*
	 * Find all the keys
	 */
	Vector primarys = new Vector();
	Vector keys = new Vector();
	Vector others = new Vector();
	
	for (int i=0; i<columns.length ; i++)
	{
	    columns[i].setAdaptor(adaptor);
	    if (columns[i].isPrimary())
		primarys.addElement(columns[i]);
	    else
	    {
		others.addElement(columns[i]);
		if (columns[i].isKey())
		    keys.addElement(columns[i]);
	    }
	}
	    
	/* If we found some primary columns, make a key out of them */
	if (!primarys.isEmpty())
	{
	    Column[] temp = new Column[primarys.size()];
	    primarys.copyInto(temp);
	    try{
		this.primaryKey = new Key ("primary", temp);
	    }
	    catch(NonKeyException nke){
		Code.ignore(nke);
		this.primaryKey=null;
	    }
	}
	else
	    this.primaryKey=null;
	
	/* If we found some key columns, make a key out of them */
	if (!keys.isEmpty())
	{
	    Column[] temp = new Column[keys.size()];
	    keys.copyInto(temp);
	    this.otherKeys = new ColumnGroup("otherKeys", temp);
	}
	else
	    this.otherKeys=null;
	
	/* If we found some non primary columns, make a key out of them */
	if (!others.isEmpty())
	{
	    Column[] temp = new Column[others.size()];
	    others.copyInto(temp);
	    this.otherCols = new ColumnGroup("otherCols", temp);
	}
	else
	    this.otherCols=null;
    }    

    /* ------------------------------------------------------------ */
    /** 
     * @return Database for table
     */
    public Database getDatabase()
    {
	return database;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return DbAdaptor for table 
     */
    public  DbAdaptor getAdaptor()
    {
	return adaptor;
    }

    /* ------------------------------------------------------------ */
    /** Does this table have a primary key */
    public boolean hasPrimary(){
	return primaryKey != null;
    }
    
    /* ------------------------------------------------------------ */
    /** Create a new row in the table
     * @ return The new Row instance
     */
    public Row newRow()
    {
	Code.assert(database!=null,"Null Database");
	return new Row(this);
    }
    
    /* ------------------------------------------------------------ */
    /** Get a row from the table if it has a single primary key
     * Create it if it does not exist.
     */
    public Row getRow(Object primaryValue)
	 throws SQLException
    {
	Code.assert(primaryKey!=null,"No Primary Key");
	Code.assert(primaryKey.columns.length==1,"Too many Primary keys");

	Object[] value = new Object[1];
	value[0]=primaryValue;
	return getRow(value);
    }

    /* ------------------------------------------------------------ */
    /** Get a row from the table.
     */
    public Row getRow(Object[] primaryValues)
	 throws SQLException
    {
	String query =
	    "SELECT * FROM " +
	    getName() +
	    " WHERE "+
	    primaryKey.toString(primaryValues," AND ");
	
	java.sql.ResultSet rs = database().query(query);

	if (rs.next())
	{
	    Row row = new Row(this, rs);
	    Code.assert(!rs.next(),"multiple rows for primary");
	    return row;
	}
	return null;    
    }
    
    /* ------------------------------------------------------------ */
    /** Get a rows from the table by non primary key
     */
    public RowEnumeration getRows(ColumnGroup columns, Object[] values)
	 throws SQLException
    {
	String where = columns.toString(values," AND ");
	return getRows(where);
    }
    
    /* ------------------------------------------------------------ */
    /** Get a rows from the table by arbitrary where clause
     */
    public RowEnumeration getRows(String whereClause)
	 throws SQLException
    {
	String query =
	    "SELECT * FROM " +
	    getName() +
	    " WHERE "+
	    whereClause;
	
	java.sql.ResultSet rs = database().query(query);

	return new RowEnumeration(this,rs);
    }
    
    /* ------------------------------------------------------------ */
    /** Delete a row from the table singular primary key
     */
    public void deleteRow(Object primaryValues)
	 throws SQLException
    {
	Object[] pv = new Object[1];
	pv[0]=primaryValues;
	String where = primaryKey.toString(pv," AND ");
	deleteRows(where);
    }
    
    /* ------------------------------------------------------------ */
    /** Delete a row from the table by primary key values
     */
    public void deleteRows(Object[] primaryValues)
	 throws SQLException
    {
	String where = primaryKey.toString(primaryValues," AND ");
	deleteRows(where);
    }
    
    /* ------------------------------------------------------------ */
    /** Delete a row from the table by column group values
     */
    public void deleteRows(ColumnGroup columns, Object[] values)
	 throws SQLException
    {
	String where = columns.toString(values," AND ");
	deleteRows(where);
    }
    
    /* ------------------------------------------------------------ */
    /** Delete a row from the table by arbitrary WHERE clause
     */
    public void deleteRows(String whereClause)
	 throws SQLException
    {
	String query =
	    "DELETE FROM " +
	    getName() +
	    " WHERE "+
	    whereClause;
	database().update(query);
    }
	
    /* ------------------------------------------------------------ */
    /** @return The database for this table
     */
    public Database database()
    {
	Code.assert(database!=null,"Null Database");
	return database;
    }
	
    /* ------------------------------------------------------------ */
    /** Set the database used by this table. This can only be called if null
     * was passed to the constructor */
    public void database(Database db){
	Code.assert(database == null,"DB already inited");
	database = db;
    }
    
    /* ------------------------------------------------------------ */
    public String create()
    {
	return
	    adaptor.formatCreateTable(this) +
	    adaptor.formatCreateIndex(this);
    }
    
    /* ------------------------------------------------------------ */
    public String drop()
    {
	return adaptor.dropTable(this) ;
    }
    
    /* ------------------------------------------------------------ */
    /** Create table description
     */
    public String toString()
    {
	if (adaptor==null)
	    return super.toString();
	return adaptor.formatTable(this);
    }
};


