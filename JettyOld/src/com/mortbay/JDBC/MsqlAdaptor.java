// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;
import com.mortbay.Base.*;
import java.sql.*;
import java.util.*;

public class MsqlAdaptor extends DbAdaptor
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public MsqlAdaptor()
    {
	dbDriver="com.imaginary.sql.msql.MsqlDriver";
    }
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @param s 
     * @return 
     */
    public String quote(String s)
    {
	StringBuffer b = new StringBuffer();
	b.append("'");
	
	int i=0;
	int last=-1;
	
	while((i=s.indexOf("'",i))!=-1)
	{	
	    b.append(s.substring(last+1,i));
	    b.append("\\'");
	    last=i++;
	}
	if (s.length()>last+1)
	    b.append(s.substring(last+1));

	b.append("'");
	s=b.toString();
	return s;
    } 

    /* ------------------------------------------------------------ */
    /** 
     * @param type 
     * @return 
     */
    public String columnType(int type)
    {
	if (type>Column.textTag)
	    return "TEXT("+(type-Column.textTag)+")";

	return super.columnType(type);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param table 
     * @return 
     */
    public String formatKeys(Table table)
    {
	return "";
    }
    
	
    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public String go()
    {
	return "\n\\g\n";
    }   
};
