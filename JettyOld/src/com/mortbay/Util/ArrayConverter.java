// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.lang.reflect.Array;

/** Class to convert Vectors and Strings to Arrays.
 * This class will convert Vectors and Strings to arrays. String are
 * converted by parsing them into elements based on a separator (as passed to
 * the constructor).
 */
public class ArrayConverter implements Converter
{
    /* ------------------------------------------------------------ */
    String separator = null;
    /* ------------------------------------------------------------ */
    public ArrayConverter(String separator){
	this.separator = separator;
    }
    /* ------------------------------------------------------------ */
    public Object convert(Object toConvert, Class convertTo,
			  Converter context)
    {
	if (toConvert == null || toConvert.getClass().equals(convertTo))
	    // Already correct type!
	    return toConvert;	
	if (convertTo.isArray()){
	    Vector elems = null;
	    if (toConvert instanceof Vector)
		elems = (Vector)toConvert;
	    else {
		StringTokenizer st =
		    new StringTokenizer(toConvert.toString(), separator);
		elems = new Vector();
		while (st.hasMoreElements())
		    elems.addElement(st.nextElement());
	    }
	    Class typeC = convertTo.getComponentType();
	    Object res = Array.newInstance(typeC, elems.size());
	    int i = 0;
	    Converter converter = context == null ? this : context;
	    for (Enumeration enum = elems.elements();
		 enum.hasMoreElements(); i++)
	    {
		Array.set(res, i, converter.convert(enum.nextElement(), typeC,
						    converter));
	    }
	    return res;
	}
	return null;
    }
    /* ------------------------------------------------------------ */
};
