// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;

import com.mortbay.Base.*;
import com.mortbay.JDBC.*;
import java.sql.*;
import java.util.*;


public abstract class RelationalTable extends Table
{
     
    private Hashtable AllDeleteRelations = null;
    private Hashtable AllNullRelations = null;
    private Vector AllDeleteRelationsVec = null;
    private Vector AllNullRelationsVec = null;


    public static int REAL_DELETE = 0;
    public static int FAKE_DELETE = 1;

    private String fakeDeleteColumnName = null;
    private Object fakeDeleteColumnValue = null;
    private int deleteMethod = REAL_DELETE;
    private Database dtb = null;
    

    
    public RelationalTable (String name, Column[] cols, Database dtb,
			    Hashtable deleteRelations,
			    Hashtable nullRelations,
			    Vector deleteRelationsVec,
			    Vector nullRelationsVec,
			    int deleteMethod,
			    String columnName,
			    Object deleteValue)
    {
	super (name, cols, dtb);
	AllDeleteRelations = deleteRelations;
	AllNullRelations = nullRelations;
	AllDeleteRelationsVec = deleteRelationsVec;
	AllNullRelationsVec = nullRelationsVec;
	this.deleteMethod = deleteMethod;
	fakeDeleteColumnName = columnName;
	fakeDeleteColumnValue = deleteValue;
	dtb = dtb;
    }

        
    public void setDeleteRelationWith (RelationalTable tbl,
				       ColumnGroup cols)
    {
	if (AllDeleteRelations != null)
	    AllDeleteRelations.put (tbl, cols);
	if (AllDeleteRelationsVec != null)
	    AllDeleteRelationsVec.addElement(tbl);
	
    } 

    
    public void setNullRelationWith (RelationalTable tbl,
				     ColumnGroup cols)
    {
	if (AllNullRelations != null)
	    AllNullRelations.put (tbl, cols);
	
	if (AllNullRelationsVec != null)
	    AllNullRelationsVec.addElement(tbl);
    } 


    
    public void delete (Object[] val)
	 throws SQLException
    {
	try
	{
	    callNullRelations(val);
	    callDeleteRelations(val);
	}
	catch (SQLException e)
	{
	    throw e;
	}
	
    }

    
    public void callDeleteRelations (Object[] values)
	 throws SQLException
    {
	if ((AllDeleteRelationsVec != null) && (AllDeleteRelations != null))
	{
	    for (int i = 0 ; i < AllDeleteRelationsVec.size(); i ++)
	    {
		RelationalTable tbl =
		(RelationalTable)AllDeleteRelationsVec.elementAt(i);

		ColumnGroup cols = (ColumnGroup)AllDeleteRelations.get(tbl);
		
		try
		{
		    tbl.deleteRelation (cols, values);
		}
		catch (SQLException e)
		{
		    throw e;
		}
	    }
	}
    }

    
    public void callNullRelations (Object[] values)
	 throws SQLException
    {
	if ((AllNullRelationsVec != null) && (AllNullRelations != null))
	{
	    for (int i = 0 ; i < AllNullRelationsVec.size(); i ++)
	    {
		RelationalTable tbl = (RelationalTable)AllNullRelationsVec.elementAt(i);
		ColumnGroup cols = (ColumnGroup)AllNullRelations.get(tbl);
		
		
		try
		{
		    tbl.nullRelation (cols, values);
		}
		catch (SQLException e)
		{
		    throw e;
		}
	    }
	    
	}
    }


    
    private void deleteRelation(ColumnGroup cols, Object[] values)
	 throws SQLException
    {
	try
	{
	   
	    
	    if (deleteMethod == REAL_DELETE)
		deleteRows (cols, values);
	    else
	    {
		RowEnumeration rows = getRows(cols, values);
		
		
		
		while (rows.hasMoreElements())
		{
		    Row r = rows.nextRow();
		    
		    r.set(fakeDeleteColumnName, fakeDeleteColumnValue);
		    r.update();
		    // TrackerDB.commit(); - done by caller.
		}
	    }
	}
	catch (SQLException e)
	{
	    throw e;
	}
	
	
    }
    

    private void nullRelation(ColumnGroup cols, Object[] values)
	 throws SQLException
    {
	try
	{
	    RowEnumeration rows = getRows(cols, values);
	    
	    
	    
	    while (rows.hasMoreElements())
	    {
				
		Row r = rows.nextRow();
		
		for (int i = 0; i < cols.columns.length ; i ++)
		{
		    r.set(cols.columns[i].getName(), new Integer(0));
		}
		r.update();
		//TrackerDB.commit(); - done by caller
	    }
	    
	}
	catch (SQLException e)
	{
	    throw e;
	}
	
    }
}
