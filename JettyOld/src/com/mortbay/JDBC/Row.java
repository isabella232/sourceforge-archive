// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================


package com.mortbay.JDBC;
import com.mortbay.Base.*;
import java.sql.*;
import java.util.*;


/* ---------------------------------------------------------------- */
/** Wrapper for a JDBC table row
 * <p> Holds data for a JDBC table row
 *
 * <p><h4>Notes</h4>
 * <p> Performs conversion for DATETIME and ENUM column types
 * as the columns are loaded and/or updated.
 *
 * <p><h4>Usage</h4>
 * <pre>
 * See TestHarness
 * </pre>
 *
 * @see Class.ThisShouldHaveBeenChanged
 * @version $Id$
 * @author Greg Wilkins
*/
public class Row
{
    /* ------------------------------------------------------------ */
    Table table;
    Object[] values=null;
    boolean exists=false;   

    /* ------------------------------------------------------------ */
    /** Base Row constructor
     */
    private Row(Table table, boolean initValues)
    {
	this.table=table;
	values = new Object[table.columns.length];

	if (initValues)
	    for (int i=0;i<values.length;i++)
		values[i]=table.columns[i].nullValue();
    }
    
    /* ------------------------------------------------------------ */
    /** Base Row constructor
     */
    Row(Table table)
    {
	this(table,true);
    }
    
    /* ------------------------------------------------------------ */
    /** Build a row from the next result in the result set.
     * It expects that next() has already been called on the
     * ResultSet.
     */
    Row(Table table, java.sql.ResultSet rs)
	 throws java.sql.SQLException
    {
        this(table,false);
    
	for (int i=0;i<values.length;i++)
	{
	    values[i]=rs.getObject(i+1);

	    Column c = table.columns[i];
		
	    if (c.isType(Column.DATETIME))
		values[i]=
		    new java.util.Date(1000*((Integer)values[i]).longValue());
	    if (c.isEnum())
		values[i]=c.enum2str(Integer.parseInt(values[i].toString()));
	}
	exists=true;

	Code.debug("new Row: "+toString());
    }
    

    /* ------------------------------------------------------------ */
    /** Get value by column index
     */
    public Object get(int i)
    {
	if (values[i]==null)
	   return table.columns[i].nullValue();
	return values[i];
    }

    /* ------------------------------------------------------------ */
    /** Get value by column name
     */
    public Object get(String column)
    {
	int i = table.index(column);
	if (values[i]==null)
	   return table.columns[i].nullValue();
	return values[i];
    }

    /* ------------------------------------------------------------ */
    /** Get values by columnGroup
     */
    public Object[] get(ColumnGroup columns)
    {
	if (columns.equals(table))
	{
	    return values;
	}

	Object[] v = new Object[columns.columns.length];
	for (int i=0 ; i< columns.columns.length; i++)
	    v[i]=values[table.index(columns.columns[i].getName())];
	return v;
    }

    /* ------------------------------------------------------------ */
    /** Set value by column index
     */
    public void set(int i, Object value)
    {
    	Code.debug("Set "+table.columns[i].getName()+"='"+value+"'");
	if (value==null)
	   values[i]=table.columns[i].nullValue();
	else
	   values[i]=value;
    }

    /* ------------------------------------------------------------ */
    /** Set value by column name
     */
    public void set(String column,Object value)
    {
	set(table.index(column),value);
    }
    
    /* ------------------------------------------------------------ */
    /** Set value for all columns
     */
    public void set(Object[] newValues)
    {
	set(table,newValues);
    }
	
    /* ------------------------------------------------------------ */
    /** Set value by column Group
     */
    public void set(ColumnGroup columns, Object[] newValues)
    {
	if (columns.equals(table))
	{
	    for (int i=0; i<columns.columns.length; i++)
		values[i]=newValues[i];
	}
	else	
	{
	    for (int i=0; i<columns.columns.length; i++)
		values[table.index(columns.columns[i].getName())]=newValues[i];
	}
    }

    
    /* ------------------------------------------------------------ */
    /** Commit the row to the database
     */
    public void update()
	 throws SQLException
    {
	String update = "";

	Code.debug("Update: ",this);
	if (exists)
	{
	    Code.assert(table.primaryKey!=null && table.otherCols!=null,
			"Table not suitable for update");

	    Object[] keyValues   =new Object[table.primaryKey.columns.length];
	    Object[] nonKeyValues=new Object[table.columns.length-
					    table.primaryKey.columns.length];

	    int kv=0;
	    int nkv=0;
	    for (int i=0;i<values.length;i++)
	    {
		Column c = table.columns[i];
		if (c.isPrimary())
		    keyValues[kv++]=values[i];
		else
		    nonKeyValues[nkv++]=values[i];
	    }
	    
	    update =
		"UPDATE " + table.getName() +
		" SET " + table.otherCols.toString(nonKeyValues,", ") +
		" WHERE " + table.primaryKey.toString(keyValues," AND ");
	    Code.debug("Update Existing Row: ",update);
	}
	else
	{
	    update =
		"INSERT INTO " + table.getName() +
		" VALUES( " + table.toValuesString(values) +
		" )";
	    
	    Code.debug("Update New Row: ",update);
	    exists=true;
	}
	
	table.getDatabase().update(update);
    }

    /* ------------------------------------------------------------ */
    /** Set the internal state of the row to that of a new row
     * Used when adding many similar rows to a table without a
     * primary key
     */
    public void setNew()
    {
	exists=false;
    }
    
    /* ------------------------------------------------------------ */
    /** @return The table for this row
     */
    public Table getTable()
    {
	return table;
    }
	
    /* ------------------------------------------------------------ */
    /** @return The database for this row
     */
    public Database getDatabase()
    {
	return table.getDatabase();
    }
	
    /* ------------------------------------------------------------ */
    /** Rough dump of the Row as a String
     */
    public String toString()
    {
	return table.toString(values,", ");
    }	
};
