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
    com.mortbay.JDBC.Connection connection = null;

    Transaction(Database db)
	throws java.sql.SQLException
    {
	connection = (com.mortbay.JDBC.Connection)db.getConnection();
	connection.inTransaction=true;
	database = db;
    }

    public void finalize()
    {
	try{
	    if (connection!=null)
		connection.rollback();
	}
	catch(Exception e){
	    Code.debug(e);
	}    
	finally{
	    connection.inTransaction=false;
	    connection.recycle();
	    connection=null;
	}
    }
    
    public java.sql.Connection getConnection()
    {
	return connection;
    }

    public void commit()
	 throws java.sql.SQLException
    {
	try{
	    connection.commit();
	}
	finally{
	    connection.inTransaction=false;
	    connection.recycle();
	    connection=null;
	}
    }
    
    public void rollback()
	 throws java.sql.SQLException
    {
	try{
	    connection.rollback();
	}
	finally{
	    connection.inTransaction=false;
	    connection.recycle();
	    connection=null;
	}
    }
};








