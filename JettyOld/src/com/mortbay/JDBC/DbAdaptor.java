// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;
import com.mortbay.Base.*;
import java.sql.*;
import java.util.*;

/* ------------------------------------------------------------ */
/** An adaptor class for JDBC drivers
 * <p>
 * this class provides helper functions to handle the
 * differences in JDBC drivers.
 * <p>
 * Specifically it provides help with data types, quoting strings
 * and allow table creation from java described meta data (the
 * opposite of the intended JDBC style where the driver provides
 * an often unimplemented API to access the metadata).
 * <p>
 * The default implementation is for a standard ANSI SQL database
 * whose JDBC driver is specified by the java property "DbDriver"
 *
 * This class must be specialized to handle any specific behaviour
 * of a particular DB/JDBCdriver combination and the derived class
 * name should be passed to the constructor of Database
 *
 * @see Database
 * @version $Revision$ $Date$
 * @author Greg Wilkins (gregw)
 */
public class DbAdaptor
{
    protected String dbDriver;

    /* ------------------------------------------------------------ */
    /** Constructor.
     * Select DbDriver from java property DbDriver
     */
    public DbAdaptor()
    {
        dbDriver=System.getProperty("DbDriver");
    }

    /* ------------------------------------------------------------ */
    /** Get the JDBC driver class name for this adaptor.
     * @return 
     */
    public String getJdbcDriver()
    {
        Code.assert(dbDriver!=null,
                    "Default DbAdaptor must have JDBC driver specified in DbDriver property");
        return dbDriver;
    }
    
    /* ------------------------------------------------------------ */
    /** Quote a string value for the database.
     * @param s A string
     * @return The string in quotes with any internal special
     *         characters handled.
     */
    public String quote(String s)
    {
        Code.debug("QUOTE1: ",s);
        boolean sq = s.indexOf("'")>=0;
        boolean dq = s.indexOf('"')>=0;

        if (sq)
        {
            if (dq)
                s = '"' + s.replace('"','\'') + '"';
            else
                s = '"' + s + '"';
        }
        else
            s = "'" + s + "'";

        Code.debug("QUOTE2: ",s);
        return s;    
    }


    /* ------------------------------------------------------------ */
    /** Map the a column type to database column type.
     *    
     * @param type The column type (see Column)
     * @return Column type name recognized by underlying database. 
     */
    public String columnType(int type)
    {
        switch (type)
        {
          case Column.INT:
          case Column.ENUM:
          case Column.DATETIME:
              return "INT";
          case Column.REAL:
              return "REAL";
              
          default:
              if (type>Column.textTag)
                  return "VARCHAR("+(type-Column.textTag)+")";
              return "CHAR("+type+")";
        }
    }

    /* ------------------------------------------------------------ */
    /** Format a column value
     * @param column The column to format the value for
     * @param value The value of the column
     * @return A string representing how the value should be presented
     *         to the underlying database (e.g. quoted string for VARCHAR).
     */
    public String formatColumnValue(Column column, Object value)
    {    
        if (column.isChar() || column.isText())
            return quote(value.toString());

        if (column.isEnum())
        {
            if (value instanceof Integer)
                return value.toString();
            return Integer.toString(column.str2enum(value.toString()));
        }
        
        if (column.isType(Column.DATETIME))
        {
            java.util.Date d=null;
            if (value instanceof java.util.Date)
                d = (java.util.Date)value;
            else if (value instanceof Number)
                d = new java.util.Date(((Number)value).longValue());
            else
                d=new java.util.Date(value.toString());

            return Long.toString(d.getTime()/1000);
        }
        
        return value.toString();
        
    }
    
    /* ------------------------------------------------------------ */
    /** A null value for the column
     * @param column The Column
     * @return An object which represents a null value for the
     * java type used to handle the column type.
     */
    public Object nullColumnValue(Column column)
    {   
        if (column.isChar() || column.isText())
           return "";
        
        if (column.isEnum())
           return column.enum2str(0);

        if (column.isType(Column.REAL))
           return new Double(0);

        if (column.isType(Column.DATETIME))
           return new java.util.Date();
        
        return new Integer(0);
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
        return "\ngo\n";
    }

    /* ------------------------------------------------------------ */
    /** Format the key columns for a table
     * Return string describing the key columns for a CREATE statement.
     * @param table The table
     * @return default returns "primary key ( column [,...] )"
     */
    public String formatKeys(Table table)
    {
        StringBuffer b = new StringBuffer();
        boolean pkey = false;
        String s=",\n    primary key ( ";
        for (int i=0;i<table.columns.length;i++)
        {
            if (table.columns[i].isPrimary())
            {
                pkey = true;
                b.append(s);
                b.append(table.columns[i].getName());
                s=",";
            }
        }
        if (pkey) b.append(" )");
        return b.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Format table columns
     * @param table The table
     * @return A text description of the tables columns suitable for
     * inclusion in a CREATE statement.
     */
    public String formatTable(Table table)
    {   
        StringBuffer b = new StringBuffer();
        b.append("TABLE ");
        b.append(table.getName());
        b.append(" (\n    ");
        
        for (int i=0;i<table.columns.length;i++)
        {
            if (i>0)
                b.append(",\n    ");
            b.append(table.columns[i].toString());
        }

        b.append(formatKeys(table));
        b.append("\n)");
        
        return b.toString();
    }

    /* ------------------------------------------------------------ */
    /** Format a table create statement.
     * @param table The table
     * @return A CREATE statement for the table.
     */
    public String formatCreateTable(Table table)
    {
        StringBuffer b = new StringBuffer();
        b.append("CREATE ");
        b.append(formatTable(table));
        b.append(go());
        return b.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Format the index creation statements for a table
     * @param table The Table
     * @return Index creation statements for the table.
     */
    public String formatCreateIndex(Table table)
    {
        StringBuffer b = new StringBuffer();
        
        if (table.primaryKey!=null)
        {
            b.append("CREATE UNIQUE INDEX ");
            b.append(table.getName());
            b.append("_primary ON ");
            b.append(table.getName());
            b.append(" ( ");
            b.append(table.primaryKey.toString());
            b.append(" ) ");
            b.append(go());
        }

        if (table.otherKeys!=null)
        {
            for (int i=0;i<table.otherKeys.columns.length;i++)
            {
                b.append("CREATE INDEX ");
                b.append(table.getName());
                b.append("_");
                b.append(table.otherKeys.columns[i].getName());
                b.append(" ON ");
                b.append(table.getName());
                b.append(" ( ");
                b.append(table.otherKeys.columns[i].getName());
                b.append(" ) ");
                b.append(go());
            }   
        }
        
        return b.toString();
    }


    /* ------------------------------------------------------------ */
    /** Format Drop table statement
     * @param table The Table
     * @return Drop table statement
     */
    String dropTable(Table table)
    {
        return "drop table "+table.getName()+go();
    }
    
}
