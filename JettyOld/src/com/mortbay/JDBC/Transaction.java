// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;

import com.mortbay.Base.Code;

/* ------------------------------------------------------------ */
/** 
 * <p>
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 * </pre>
 *
 * @see
 * @version $Revision$ $Date$
 * @author Greg Wilkins (gregw)
 */
public class Transaction
{
    Database database=null;
    java.sql.Connection connection = null;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param db 
     * @exception java.sql.SQLException 
     */
    Transaction(Database db)
	throws java.sql.SQLException
    {
	connection = db.getConnection();
	database = db;
    }

    /* ------------------------------------------------------------ */
    /** 
     */
    public void finalize()
    {
	try{
	    if (connection!=null)
		connection.rollback();
	}
	catch(Exception e)
	{
	    Code.debug(e);
	}    
	finally
	{
	    connection=null;
	}
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public java.sql.Connection getConnection()
    {
	return connection;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @exception java.sql.SQLException 
     */
    public void commit()
	 throws java.sql.SQLException
    {
	try{
	    connection.commit();
	}
	finally
	{
	    database.recycleConnection(connection);
	    connection=null;
	}
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @exception java.sql.SQLException 
     */
    public void rollback()
	 throws java.sql.SQLException
    {
	try{
	    connection.rollback();
	}
	finally
	{
	    database.recycleConnection(connection);
	    connection=null;
	}
    }
};









