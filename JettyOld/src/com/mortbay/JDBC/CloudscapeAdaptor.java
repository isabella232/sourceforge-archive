// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;
import com.mortbay.Base.*;
import java.sql.*;
import java.util.*;

public class CloudscapeAdaptor extends DbAdaptor
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public CloudscapeAdaptor()
    {
	dbDriver="COM.cloudscape.core.JDBCDriver";
    }
	
    /* ------------------------------------------------------------ */
    /** Quote a string value for the database.
     * @param s A string
     * @return The string in quotes with any internal special
     *         characters handled.
     */
    public String quote(String s)
    {
	int sq = s.indexOf("'");
	while(sq>=0)
	{
	    Code.debug(s);
	    s=s.substring(0,sq)+"'"+s.substring(sq);
	    sq=s.indexOf("'",sq+2);
	}

	return "'" + s + "'";   
    }

    /* ------------------------------------------------------------ */
    /** The marker used when creating a table to mark a primary key.
     * @return The default implementation returns "NOT NULL"
     */
    public String primaryMarker()
    {
	return "NOT NULL";
    }
    
    
    /* ------------------------------------------------------------ */
    /** The go string
     * @return The string used by the underlying database to trigger
     * handling of meta data commands. Default is "go".
     */
    public String go()
    {
	return ";\n";
    }
    
};










