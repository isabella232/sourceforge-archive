// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import com.mortbay.Base.Test;
import java.lang.reflect.*;

/* ------------------------------------------------------------ */
/** Data class Helper
 * <p>
 * This class provides static utlility routines that assist
 * with the creation and output of hierarcies of agregated
 * classes that contain public data members (what used to be
 * called structures :-).
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see
 * @version 1.0 Fri Dec  5 1997
 * @author Greg Wilkins (gregw)
 */
public class DataClass
{
    /* ------------------------------------------------------------ */
    private static Class[] __intArg = { java.lang.Integer.TYPE};
    private static Object[] __zeroArg = { new Integer(0) };
    private static String __emptyString = "";
    private static String __space = "                                                                               ";
    
    /* ------------------------------------------------------------ */
    /** Construct and return an "empty" instance of a class. An empty
     * instance is a class constructed with its simplest constructor and
     * with all its public members non null. The recursive process to
     * create a null instance is as follows: <BR>
     * 1. Attempt to construct with null constructor.<BR>
     * 2. If construction failed, look for a constructor taking an
     *    int and call it with 0.
     * 3. For all public data members:<BR>
     * 3a.  If the member is a builtin type, leave as is.<BR>
     * 3b.  If the member is a string, set to the null string.
     * 3c.  If the member is an array:<BR>
     *          create an empty array<BR>
     * 3d.  If the member is an object, call emptyInstance to create it.<BR>
     * @param The class to construct.
     * @return An empty instance of the class 
     */
    public static Object emptyInstance(java.lang.Class instance_class)
    {
	Object instance = null;
	Code.debug("make empty instance of "+instance_class);
	
	try{
	    // Try the null constructor
	    try{
		instance = instance_class.newInstance();
	    }
	    catch (NoSuchMethodError nsm){
		// Code.ignore(nsm);
	    }
	    catch (InstantiationException ie){
		// Code.ignore(nsm);
	    }
	    catch (IllegalAccessException iae){
		// Code.ignore(iae);
	    }

	    // if that failed, try an int constructor
	    if (instance==null)
	    {
		try {
		    Code.debug("Default constructor for "+instance_class+
			       " failed.  Try from_int method...");
		    Constructor constructor = instance_class
			.getConstructor(__intArg);
		    instance = constructor.newInstance(__zeroArg);
		}
		catch (Exception e)
		{
		    Code.debug("Return null due to exception",e);
		    return null;
		}
	    }	

	    // for all fields of the instance
	    Field[] fields = instance_class.getFields();
	    for (int f=0; f<fields.length ; f++)
	    {
		int modifier = fields[f].getModifiers();

		// skip if static, not public or final
		if (Modifier.isStatic(modifier) ||
		    Modifier.isFinal(modifier) ||
		    !Modifier.isPublic(modifier))
		    continue;
	    
		// get the field type.
		Class field_type = fields[f].getType();
	    
		
		// if the field is primative, leave it as is.
		if (field_type.isPrimitive())
		    continue;
		
		// if the field is a string, set to the empty string
		if (field_type.equals(String.class))
		{
		    fields[f].set(instance,__emptyString);
		    continue;
		}

		// if the field is an array...
		if (field_type.isArray())
		{
		    // create empty array
		    fields[f].set(instance,
				  Array.newInstance(field_type
						    .getComponentType(),
						    0));
		    continue;
		}

		// field must be an object, so recurse to
		// emptyInstance.
		fields[f].set(instance,
			      emptyInstance(field_type));
	    }
	}
	catch(Exception e){
	    Code.fail("Can't instantiate empty instance for "+
		      instance_class,
		      e);
	}
	return instance;
    }

    /* ------------------------------------------------------------ */
    /** Formatted object toString.
     *
     * Presents members in formated string
     * @param object 
     * @return 
     */
    public static String toString(java.lang.Object object)
    {
	StringBuffer buf = new StringBuffer();
	itemToString(object,buf,0);
	return buf.toString();
    }

    /* ------------------------------------------------------------ */
    private static void itemToString(Object o,StringBuffer buf,int indent)
    {
	// if the element is null
	if (o==null)
	    buf.append("null");
	else
	{
	    Class field_type = o.getClass();
	    // else if an array, handle each element
	    if (field_type.isArray())
		arrayToString(o,buf,indent);
	    // else if a string, print it
	    else if (field_type.equals(java.lang.String.class))
	    {
		buf.append('"');
		buf.append(o.toString());
		buf.append('"');
	    }
	    else 
	    {
		// look for at least one fields public datamembers
		Field[] subFields = field_type.getFields();
		int sf=0;
		for (sf=0; sf<subFields.length ; sf++)
		{
		    int m = subFields[sf].getModifiers();
		    if (!Modifier.isStatic(m) && Modifier.isPublic(m))
			break;
		}
		// if Public subfields, then call classToString
		if (sf<subFields.length)
		    classToString(o,buf,indent);
		else // just print it
		    buf.append(o.toString());
	    }
	}
    }
    
    
    /* ------------------------------------------------------------ */
    private static void classToString(java.lang.Object object,
				 StringBuffer buf,
				 final int indent)
    {
	Class objClass = object.getClass();

	// calculate indents
	String base_space = __space.substring(0,indent);
	String indent_space = __space.substring(0,indent+2);

	// write object header
	buf.append(objClass.toString()+
		   "<"+object.hashCode()+">\n");
	buf.append(base_space);
	buf.append("{\n");

	// write object fields
	// for each fields
	
	Field[] fields = objClass.getFields();
	for (int f=0; f<fields.length ; f++)
	{
	    int modifier = fields[f].getModifiers();

	    // skip if static, not public or final
	    if (Modifier.isStatic(modifier) ||
		!Modifier.isPublic(modifier))
		continue;
	    
	    // get the field type.
	    Class field_type = fields[f].getType();

	    // write field label
	    buf.append(indent_space);
	    buf.append(fields[f].getName());
	    buf.append(" = ");

	    // write field value
	    Object o = null;
	    try {
		o = fields[f].get(object);
	    }
	    catch(IllegalAccessException e){
		Code.warning(e);
		o = "<Access Denied!>";
	    }

	    itemToString(o,buf,indent+2);
	    
	    buf.append(";\n");
	}

	// write object tail
	buf.append(base_space);
	buf.append("}");
    }
    
    /* ------------------------------------------------------------ */
    private static void arrayToString(java.lang.Object object,
				      StringBuffer buf,
				      final int indent)
    {
	Class objClass = object.getClass();
	Code.assert(objClass.isArray(),"Must be an array");

	
	// calculate indents
	String base_space = __space.substring(0,indent);
	String indent_space = __space.substring(0,indent+2);

	// setup array style
	String sep="\n"+indent_space;
	String sep2="," +sep;
	String end="\n"+base_space;
	int length = Array.getLength(object);

	// if a primitive array make it in-line
	buf.append(objClass.getComponentType().getName());
	if (objClass.getComponentType().isPrimitive())
	{
	    sep="";
	    sep2=",";
	    end="";
	}
	else // put it on multiple lines
	{
	    buf.append("\n");
	    buf.append(__space.substring(0,indent));
	}
	
	// start array
	buf.append("[");
	
	// for each element
	for (int e=0;e<length;e++)
	{
	    Object o = Array.get(object,e);
	    
	    // write element separator
	    buf.append(sep);
	    if (sep!=sep2)sep=sep2;

	    itemToString(o,buf,indent+2);
	}

	// write object tail
	buf.append(end);
	buf.append(']');
    }


};
