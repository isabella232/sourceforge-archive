// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;

import com.mortbay.Base.*;

import java.util.*;
import java.sql.*;


/* ---------------------------------------------------------------- */
/** Group of SQL column definitions
 * <p> This class represents a named collection of column definitions,
 * which may be used to define a table, multi-field key or any arbitrary
 * grouping of columns.
 * <p>
 * Support is provided for formating strings and values against these
 * groupings for the generation of various SQL statements.
 *
 * <p><h4>Notes</h4>
 *
 * <p><h4>Usage</h4>
 * <pre>
 * </pre>
 *
 * @version $Id$
 * @author Jan Bartel
 */
public class ColumnGroup
{
    /* ------------------------------------------------------------ */
    public final Column[] columns;
    
    /* ------------------------------------------------------------ */
    final String comaList;
    final Hashtable column2index = new Hashtable();
    
    /* ------------------------------------------------------------ */
    private String name;

    /* ------------------------------------------------------------ */
    public ColumnGroup(String name, Column[] columns)
    {
        this.name = name;
        this.columns = columns;
        
        StringBuffer b = new StringBuffer();
        for (int i=0;i<columns.length;i++)
        {
            column2index.put(columns[i].getName(),new Integer(i));
            
            b.append(columns[i].getName());
            if ((i + 1) < columns.length)
                b.append(", ");
        }
        comaList=b.toString();
        
        Code.debug("Created : ColumnGroup "+name+ " as " + toString());
    }
    
    /* ------------------------------------------------------------ */
    /** Return column by name
     * @param name Name of the column
     * @return The column
     */
    public Column column(String name)
    {
        return columns[index(name)];
    }
    
    /* ------------------------------------------------------------ */
    /** Return the index of a column
     * @param column the name of the column
     * @return The index of the column
     */
    public int index(String column)
    {
        try
        {
            return ((Integer)column2index.get(column)).intValue();
        }
        catch(Exception e){
            Code.debug(e);
            return -1;
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Convert column group to String.
     * Format as coma separated list of column names
     * e.g. "AAA, BBB, CCC"
     */
    public String toString()
    {
        return comaList;
    }
    
    /* ------------------------------------------------------------ */
    /** Convert column group to value String.
     * Format as coma separated list of column values
     * e.g. "AAA=1, BBB=2, CCC=3"
     */
    public String toString(Object[] values)
    {
        return toString(values,", ");
    }

    /* ------------------------------------------------------------ */
    /** Get the group name
     * @return 
     */
    public  String getName()
    {
        return name;
    }

    /* ------------------------------------------------------------ */
    /** Convert column group to value String.
     * Format as separated list of column values
     * e.g. "AAA=1<separator>BBB=2<separator>CCC=3"
     */
    public String toString(Object[] values, String separator)
    {
        Code.assert(values!=null,"NULL values passed");
        Code.assert(values.length==columns.length,
                    "Number of values does not match number of columns");
        
        StringBuffer b = new StringBuffer();

        for (int i = 0; i < columns.length; i++)
        {
            Column c = columns[i];

            if (i > 0)
                b.append (separator);

            b.append(c.getName());
            b.append("=");
            b.append(c.toString(values[i]));
        }
        
        return b.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Convert column group to values only String.
     * Format as separated list of column values
     * e.g. "1, 2, 3"
     */
    public String toValuesString(Object[] values)
    {
        Code.assert(values!=null,"NULL values passed");
        Code.assert(values.length==columns.length,
                    "Number of values does not match number of columns");
        
        StringBuffer b = new StringBuffer();

        for (int i = 0; i < columns.length; i++)
        {
            Column c = columns[i];
            if (i > 0)
                b.append (", ");
            b.append(c.toString(values[i]));
        }
        
        return b.toString();
    }
    
    /* ------------------------------------------------------------ */
    static void test()
    {
        Database db = new Database();
        Column NameCol =
            new Column("name","User Name",Column.CHAR(20),Column.PRIMARY);
        NameCol.setAdaptor(db.getAdaptor());
        Column FullNameCol =
            new Column("fullName","Full Name",Column.CHAR(40),Column.KEY);
        FullNameCol.setAdaptor(db.getAdaptor());
        Column HeightCol =
            new Column("height","Height cm",Column.INT,Column.NONE);
        HeightCol.setAdaptor(db.getAdaptor());
        Column[] testCols = {NameCol,FullNameCol,HeightCol};
    
        Test t = new Test("com.mortbay.JDBC.ColumnGroup");
        ColumnGroup cg = new ColumnGroup("Test",testCols);

        t.checkEquals(cg.getName(),"Test","Name constructed");
        t.checkEquals(cg.columns.length,3,"Columns constructed");
        t.checkEquals(cg.toString(),"name, fullName, height","toString()");

        Object[] values = new Object[3];
        values[0] = "gregw";
        values[1] = "Greg Wilkins";
        values[2] = new Integer(184);
        t.checkEquals(cg.toString(values),
                      "name='gregw', fullName='Greg Wilkins', height=184",
                      "toString(values)");

        t.checkEquals(NameCol,cg.column(TestTable.NameCol.getName()),
                      "Get by name column 0");
        t.checkEquals(FullNameCol,cg.column(TestTable.FullNameCol.getName()),
                      "Get by name column 1");
        t.checkEquals(cg.columns[0],cg.column("name"),"Get column 0");
        t.checkEquals(cg.columns[1],cg.column("fullName"),"Get column 1");
        t.checkEquals(cg.columns[2],cg.column("height"),"Get column 2");
    }
    
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
        test();
        Test.report();
    }
}



