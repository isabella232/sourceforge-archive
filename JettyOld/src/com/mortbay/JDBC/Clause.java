// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.JDBC;
import com.mortbay.Base.Code;
import java.util.*;


/* ------------------------------------------------------------ */
/** A Where Clause for a select statement

 * <p><h4>Notes</h4>
 * <p>clause = where | where BOOL clause | ( clause );
 *    where = thing OP thing;
 *
 * <p><h4>Usage</h4>
 *
 *
 * @see Select
 * @version 1.0 Fri Nov 28 1997
 * @author Jan Bartel (janb)
 */
public class Clause 
{
    StringBuffer clause = null;
    int bracketcount = 0;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public Clause ()
    {
	clause = new StringBuffer();
    }
    
    /* ------------------------------------------------------------ */
    /** Open brace
     * @return 
     */
    public Clause open ()
    {
	if (Code.debug())
	    clause.append("\n(\n");
	else
	    clause.append (" ( ");
	bracketcount++;
	return this;
    }

    /* ------------------------------------------------------------ */
    /** Close brace
     * @return 
     */
    public Clause close ()
    {
	bracketcount--;
	if (Code.debug())
	    clause.append("\n)\n");
	else
	    clause.append (" ) ");
	return this;
    };

    /* ------------------------------------------------------------ */
    /** Boolean and
     * @return 
     */
    public Clause and ()
    {
	clause.append (Select.AND);
	return this;
    }
    
    /* ------------------------------------------------------------ */
    /** Boolean or
     * @return 
     */
    public Clause or ()
    {
	clause.append (Select.OR);
	return this;
    }    
    
    /* ------------------------------------------------------------ */
    /** Add comparison to where clause
     * @deprecated Use where(lhsTable,lhsColumn,operator,value);
     * @param operator 
     * @param table 
     * @param column 
     * @param value 
     * @return 
     */
    public Clause where(String operator,
			Table table, Column column,
			Object value)
    {
	return where(table,column,
		     operator,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Add comparison to where clause
     * @param operator 
     * @param table 
     * @param column 
     * @param value 
     * @return 
     */
    public Clause where(Table table, Column column,
			String operator,
			Object value)
    {
	clause.append (Select.qualifyName (table, column));
	clause.append (operator);
	clause.append (column.toString(value));
	return this;
    }
    

    /* ------------------------------------------------------------ */
    /** Add comparison to where clause
     * @deprecated Use where(lhsTable,lhsColumn,operator,rhsTable,rhsColumn);
     * @param operator 
     * @param lhsTable 
     * @param lhsColumn 
     * @param rhsTable 
     * @param rhsColumn 
     * @return
     */
    public Clause where(String operator,
			Table lhsTable, Column lhsColumn,
			Table rhsTable, Column rhsColumn)
    {
	return where(lhsTable,lhsColumn,
		     operator,
		     rhsTable,rhsColumn);
    }
    
    /* ------------------------------------------------------------ */
    /** Add comparison to where clause
     * @param lhsTable 
     * @param lhsColumn 
     * @param operator 
     * @param rhsTable 
     * @param rhsColumn 
     * @return 
     */
    public Clause where(Table lhsTable, Column lhsColumn,
			String operator,
			Table rhsTable, Column rhsColumn)
    {
	String expression = null;
	String left = null;
	String right = null;
	
	clause.append (Select.qualifyName (lhsTable, lhsColumn));
	clause.append (operator);
	clause.append (Select.qualifyName (rhsTable, rhsColumn));
	return this;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public String toString()
    {
	Code.assert(bracketcount == 0,
		    "Mismatched braces\n" + clause.toString());
	return clause.toString();
    }

};






