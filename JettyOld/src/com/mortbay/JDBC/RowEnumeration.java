// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;

import com.mortbay.Base.*;
import java.sql.*;
import java.util.*;

/** Wraps a ResultSet into enumerated Row instances
 * <p>
 *
 * <p><h4>Notes</h4>
 * <p> Rows are only extracted from ResultSet as they are requested.
 *
 * <p><h4>Usage</h4>
 * <p>As per Enumeration
 *
 * @version $Id$
 * @author Greg Wilkins
*/
public class RowEnumeration implements Enumeration
{
    /* ------------------------------------------------------------ */
    final Table table;
    final java.sql.ResultSet rs;
    boolean nextAlreadyCalled=false;
    boolean hasMore=false;
    
    /* ------------------------------------------------------------ */
    public RowEnumeration(Table table,java.sql.ResultSet rs)
    {
	this.table=table;
	this.rs=rs;
    }
    
    /* ------------------------------------------------------------ */
    public boolean hasMoreElements()
    {
	if (!nextAlreadyCalled)
	{
	    try{
		hasMore=rs.next();
	    }
	    catch(SQLException e){
		Code.debug(e);
		hasMore=false;
	    }    
	    nextAlreadyCalled=true;
	}
	if (hasMore==false)
	{
	    try{
		rs.close();
	    }
	    catch(SQLException e){
		Code.warning(e);
	    }
	}
	
	return hasMore;
    }
    
    /* ------------------------------------------------------------ */
    public Object nextElement()
	 throws NoSuchElementException
    {
	return nextRow();
    }
	
    /* ------------------------------------------------------------ */
    public Row nextRow()
	 throws NoSuchElementException
    {
	if (!nextAlreadyCalled)
	    try{
		hasMore=rs.next();
	    }
	    catch(SQLException e){
		Code.debug(e);
		hasMore=false;
	    }    
	else
	    nextAlreadyCalled=false;

	if (!hasMore)
	{ 
	    try{
		rs.close();
	    }
	    catch(SQLException e){
		Code.warning(e);
		throw new NoSuchElementException();
	    }
	}	
	try{
	    return new Row(table,rs);
	}
	catch(SQLException e){
	    throw new NoSuchElementException();
	}
    }
    
};





