// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd., Sydney
// $Id$
// ========================================================================

package com.mortbay.JDBC;
import com.mortbay.Base.*;
import java.sql.*;
import java.util.*;

/* ---------------------------------------------------------------- */
/** Meta data for database column
 * <p> Provides the Type, name, label and size of a column in a
 * JDBC database.
 *
 * <p><h4>Notes</h4>
 * <p> DATETIME and ENUMs are converted to int as mSQL does not
 * support those types yet.
 *
 * <p><h4>Usage</h4>
 * <pre>
 * See TestHarness
 * </pre>
 *
 * @see Class.ThisShouldHaveBeenChanged
 * @version $Id$
 * @author Greg Wilkins
*/
public class Column 
{
    /* ------------------------------------------------------------ */
    static final int textTag = 5000;
    
    /* ------------------------------------------------------------ */
    public static final int      INT  = -1;
    public static final int      REAL = -2;
    public static final int      DATETIME = -11;
    public static final int      CHAR(int length)  {return length;}
    public static final int      TEXT()            {return textTag+128;}
    public static final int      TEXT(int length)  {return textTag+length;}
    public static final int	 VARCHAR(int length){ return TEXT(length);}
    public static final int      ENUM = -12;
    public static final String[] ENUM(String[] e)  {return e;}

    /* ------------------------------------------------------------ */
    public static final int NONE  = 0;
    public static final int KEY   = 1;
    public static final int PRIMARY = 2|KEY;
    
    /* ------------------------------------------------------------ */
    private String name;
    private String label;
    private int type;
    private int options;
    private String[] enum=null;
    private DbAdaptor adaptor=null;
    

    /* ------------------------------------------------------------ */
    /** Column constructor
     * @param name The name of the column in the database.
     * @param label A long descriptive name of the column for humans
     * @param type The type of value the column can hold
     * @param optional status indicators for the column
     */
    public Column(String name,
		  String label,
		  int type,
		  int options)
    {
	this.name=name;
	this.label=label;
	this.type=type;
	this.options=options;
    }

    /* ------------------------------------------------------------ */
    /** Column constructor for enumerated column
     * @param name The name of the column in the database.
     * @param label A long descriptive name of the column for humans
     * @param enum An array of string enum descriptors.
     * @param optional status indicators for the column
     */
    public Column(String name,
		  String label,
		  String[] enum,
		  int options)
    {
	this(name,label,ENUM,options);
	this.enum=enum;
    }

    /* ------------------------------------------------------------ */
    public String getName()
    {
	return name;
    }

    /* ------------------------------------------------------------ */
    public String getLabel()
    {
	return label;
    }

    /* ------------------------------------------------------------ */
    public DbAdaptor getAdaptor()
    {
	return adaptor;
    }
    
    /* ------------------------------------------------------------ */
    void setAdaptor(DbAdaptor adaptor)
    {
	Code.assert(this.adaptor==null || this.adaptor==adaptor,
		    "Column can be have only 1 type of adaptor per instance");
	this.adaptor = adaptor;
    }

    /* ------------------------------------------------------------ */
    public boolean isKey()
    {
	return options==KEY || options==PRIMARY;
    }

    /* ------------------------------------------------------------ */
    public boolean isPrimary()
    {
	return options==PRIMARY;
    }

    /* ------------------------------------------------------------ */
    public boolean isChar()
    {
	return type>0 && type<textTag;
    }

    /* ------------------------------------------------------------ */
    public boolean isText()
    {
	return type>textTag;
    }

    /* ------------------------------------------------------------ */
    public boolean isEnum()
    {
	return type==ENUM && enum!=null;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isType(int type)
    {
	return (type==0 && this.type>0) || this.type==type;
    }

    
    /* ------------------------------------------------------------ */
    public int size()
    {
	if (type==ENUM && enum!=null)
	   return enum.length;
	   
	if (type<=0)
	    return 0;

	if (type>=textTag)
	    return type-textTag;
	
	return type;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
	return
	    name +
	    ' ' +
	    adaptor.columnType(type) +
	    ' ' +
	    (isPrimary()?adaptor.primaryMarker():"");  
    }

    /* ------------------------------------------------------------ */
    /** Format the value by column type
     */
    public String toString(Object value)
    {
	return adaptor.formatColumnValue(this,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Format the WHERE clause string in the form "colname op value"
     */
    public String toClause(String op, Object value)
    {
	return
	    name +
	    op +
	    toString(value);
    }
    
    /* ------------------------------------------------------------ */
    /** Get the null value for column
     */
    public Object nullValue()
    {
	return adaptor.nullColumnValue(this);
    }

    /* ------------------------------------------------------------ */
    /** Get the String value of an enumerated type
     */
    public String enum2str(int e)
    {
	return enum[e];
    }

    /* ------------------------------------------------------------ */
    /** Get the index of a String value of an enumerated type
     */
    public int str2enum(String s)
    {
	for (int i=0;i<enum.length;i++)
	   if (enum[i].equals(s))
	      return i;
	return Integer.parseInt(s);
    }
    
    /* ------------------------------------------------------------ */
    static void test()
    {
	Column FullNameCol =
	    new Column("full","Full Name",Column.CHAR(40),Column.KEY);
	Column HeightCol =
	    new Column("height","Height cm",Column.INT,Column.NONE);
    
	Test t = new Test("com.mortbay.JDBC.Column");

	t.checkEquals(FullNameCol.label,"Full Name","Meta Data label");
	t.checkEquals(FullNameCol.size(),40,"Meta Data length");
	t.check(FullNameCol.isChar(),"Meta Data char type");
	t.check(!HeightCol.isChar(),"Meta Data not char type");
    }
	
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
	test();
	Test.report();
    }
};






