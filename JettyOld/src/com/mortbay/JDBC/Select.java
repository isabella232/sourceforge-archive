// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.JDBC;
import com.mortbay.Base.Code;
import com.mortbay.Base.Test;
import java.util.*;
import java.sql.*;


/* ------------------------------------------------------------ */
/** Select produces SQL SELECT statements.
 * <p>Most of the clauses of a SELECT statement are supported,
 * including SELECT DISTINCT, FROM, WHERE, ORDER BY.
 * The WHERE subclauses can use either columns or values as
 * the right hand side of expressions.
 *
 *
 * <p><h4>Notes</h4>
 * <p>This class DOES NOT check the grammatical accuracy of
 * the SQL statement produced. 
 *
 * <p><h4>Usage</h4>
 * <pre>
 * s = new Select();
 * s.select(UserTable, NameColumn)
 *  .from(UserTable)
 *  .where(UserTable, AgeColumn,
 *         Select.GREATER_THAN,
 *         new Integer(30))
 *  .and()
 *  .where(UserTable,AgeColumn,
 *         Select.LESS_THAN,
 *         new Integer(50));
 * Enumeration results = statement.query(db);
 * while (answer.hasMoreElements())
 * {
 *    Vector row = (Vector)answer.nextElement());
 *    String name = (String)row.firstElement();
 * }
 * </pre>
 *
 * @see Clause
 * @version 1.0 Fri Nov 21 1997
 * @author Jan Bartel (janb)
 */
public class Select extends Clause
{
    /* ------------------------------------------------------------ */
    /** indicates if this select should use DISTINCT keyword */
    public final static boolean DISTINCT = true;

    /** for ORDER BY clause */
    public final static int DESC  = 1;
    
    /* ------------------------------------------------------------ */
    /** ANSI SQL standard operators and keywords */
    public final static String NOT_EQUALS = " <> ";
    public final static String EQUALS = " = ";
    public final static String LESS_THAN = " < ";
    public final static String GREATER_THAN = " > ";
    public final static String AND = " AND ";
    public final static String OR = " OR ";
    public final static String LIKE = " LIKE ";
    
    /* ------------------------------------------------------------ */
    // XXX - need to configure these
    private final static String __DISTINCT = " DISTINCT ";
    private final static String __DOT = ".";
    public final static String __SEPARATOR = " , ";

    /* ------------------------------------------------------------ */
    private Vector resultTargets = new Vector();
    private Vector selectClauses = new Vector();
    private Vector fromClauses = new Vector();
    private Vector orderByClauses = new Vector();
    private boolean isDistinct = false;
    private boolean isDescending = false;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param name - name for the query
     */
    public Select(boolean distinct)
    {
	this.isDistinct = distinct;
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public Select ()
    {
	this (!DISTINCT);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a subclause to the SELECT clause
     * @param table 
     * @param col 
     */
    public Select select (Table table, Column col)
    {
	String target = null;
	target = qualifyName (table, col);
	
	// add it into our list of subclauses for the select
	this.selectClauses.addElement (target);
	this.resultTargets.addElement (col);
	return this;
    }

    /* ------------------------------------------------------------ */
    /** Like SELECT *, but the actual column names are inserted.
     * @param table 
     */
    public Select select (Table table)
    {
	Column[] cols = table.columns;
	for (int i = 0; (cols != null) && ( i < cols.length); i++)
	{
	    String clause = qualifyName(table, cols[i]);
	    this.selectClauses.addElement(clause);
	    this.resultTargets.addElement(cols[i]);
	}
	return this;
    }

    /* ------------------------------------------------------------ */
    /** Add a table name to the FROM clause
     * @param table 
     */
    public Select from (Table table)
    {
	this.fromClauses.addElement (table.getName());
	return this;
    }


    /* ------------------------------------------------------------ */
    /** Specify a column whose values should select the order of the
     * results.
     * @param table 
     * @param column 
     */
    public Select orderBy (Table table, Column column)
    {
	this.orderByClauses.addElement(qualifyName(table, column));
	return this;
    }
    
    /* ------------------------------------------------------------ */
    /** ORDER DESCENDING, rather than ASCENDING
     * @param descending 
     */
    public Select orderSequence (int descending)
    {
	if (descending == DESC)
	    isDescending = true;
	return this;
    }

    /* ------------------------------------------------------------ */
    /** Build the query out of all of the clauses that have
     * been established, and do it against the database
     * @param db 
     * @return 
     * @exception java.sql.SQLException 
     */
    public Enumeration query(Database db)
	throws java.sql.SQLException
    {
	String thequery = toString();
	
	// what's it look like??
	Code.debug (thequery);

	// Get the answer
	java.sql.ResultSet rs = db.query(thequery);

	return new Enumerator(rs);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return - the query as a string
     */
    public String toString()
    {
	StringBuffer thequery = new StringBuffer();
	Enumeration clauses = null;
	
	// select 
	thequery.append ("SELECT ");
	if (this.isDistinct)
	    thequery.append (__DISTINCT);
	
	clauses = this.selectClauses.elements();
	while (clauses.hasMoreElements())
	{
	    thequery.append ((String)clauses.nextElement());
	    if (clauses.hasMoreElements())
		thequery.append (__SEPARATOR);  
	}

	// from
	thequery.append (" FROM ");
	clauses = this.fromClauses.elements();
	while (clauses.hasMoreElements())
	{
	    thequery.append ((String)clauses.nextElement());
	    if (clauses.hasMoreElements())
		thequery.append(__SEPARATOR);
	}


	// where
	String where = super.toString();
	if ((where != null) && (!where.equals("")))
	{
	    thequery.append (" WHERE ");
	    thequery.append (where);
	}
	

	// order by
	clauses = this.orderByClauses.elements();
	if (clauses.hasMoreElements())
	    thequery.append (" ORDER BY ");
	    	
	while (clauses.hasMoreElements())
	{
	    thequery.append ((String)clauses.nextElement());
	    if (clauses.hasMoreElements())
		thequery.append(__SEPARATOR);
	}
	if (this.isDescending)
	    thequery.append(" DESC ");
	
	return thequery.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Build a fully qualified name of the form:
     *  table.column
     * @param table 
     * @param column 
     * @return 
     */
    public static String qualifyName (Table table, Column column)
    {
	String name = null;
	name = table.getName() + __DOT + column.getName();
	return name;
    }
    
    /* ------------------------------------------------------------ */
    /** This adapter class is defined as part of DBQuery, because
     * it needs to access the ResultSet of the query method.
     */
    class Enumerator implements java.util.Enumeration
    {
	java.sql.ResultSet resultset = null;
	boolean nextAlreadyCalled=false;
	boolean hasMore = false;

	/* -------------------------------------------------------- */
	public Enumerator (java.sql.ResultSet rs)
	{
	    resultset = rs;
	}
	
	/* -------------------------------------------------------- */
	public boolean hasMoreElements()
	{
	    if (!nextAlreadyCalled)
	    {
		try
		{
		    hasMore=resultset.next();
		}
		catch(SQLException e)
		{
		    Code.debug(e);
		    hasMore=false;
		}    
		nextAlreadyCalled=true;
	    }
	    return hasMore;
	}
	
	/* -------------------------------------------------------- */
	public Object nextElement()
	    throws NoSuchElementException
	{
	    // go and get the next result set
	    if (!nextAlreadyCalled)
	    {
		try
		{
		    hasMore=resultset.next();
		}
		catch(SQLException e)
		{
		    Code.debug(e);
		    hasMore=false;
		}
	    }
	    else
		nextAlreadyCalled=false;

	    if (!hasMore)
		throw new NoSuchElementException();
	    try
	    {
		// use result set and the tablenames we saved
		// to turn the result set into a Vector reply
		Vector onerow = new Vector();

		// There will be one column in the result set for
		// each column nominated in a select.
		Enumeration cols = resultTargets.elements();
		int i = 0;
		while (cols.hasMoreElements())
		{
		    Column col = (Column)cols.nextElement();
		    // ResultSet results start from 1
		    i++;
		    Object value = resultset.getObject(i);
		    // Do some shady stuff with converting data types -
		    // Greg - do something about this.
		    if (col.isType(Column.DATETIME))
			value =
			new java.util.Date(1000*((Integer)value).longValue());
		    if (col.isEnum())
			value = col.enum2str(Integer.parseInt(value.toString()));
		    onerow.addElement(value);
		}
		return onerow;
		
	    }
	    catch(SQLException e)
	    {
		throw new NoSuchElementException(e.toString());
	    }
	}
    }

    /* ------------------------------------------------------------ */
    /** test
     */
    public static void test(Database db)
    {
	Test t = new Test("com.mortbay.JDBC.Select");
	TestTable table = new TestTable(db);
	
	Select s = new Select();
	s.select(table, table.NameCol)
	.from(table)
	.where(table, table.HeightCol,
	       Select.GREATER_THAN,
	       new Integer(150))
	.and()
	.where(table, table.HeightCol,
	       Select.LESS_THAN,
	       new Integer(200));

	t.checkEquals(s.toString(),
		      "SELECT TestTable.name FROM TestTable WHERE TestTable.height > 150 AND TestTable.height < 200","Select string");
    }
 
    /* ------------------------------------------------------------ */
    /** main
     */
    public static void main(String[] args)
    {
	test(new Database());
	Test.report();
    }
};

