// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Tools.Converter;

import com.mortbay.Util.Code;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.lang.reflect.Array;

/** Class to convert Vectors and Strings to Arrays.
 * This class will convert Vectors and Strings to arrays. String are
 * converted by parsing them into elements based on a separator (as passed to
 * the constructor).
 *
 * <h4>Notes</h4>
 * <p> If unsafeConvert is called, this class will only report errors if the
 * elements cannot be converted to the component type of the array. The
 * Exception will be of the ArrayConverter.ArrayConvertFail subtype.
 */
public class ArrayConverter extends ConverterBase
{
    /* ------------------------------------------------------------ */
    String separator = null;
    /* ------------------------------------------------------------ */
    public ArrayConverter(String separator){
        this.separator = separator;
    }
    /* ------------------------------------------------------------ */
    protected Object doConvert(Object toConvert,
			       Class convertTo,
			       Converter context,
			       boolean safe)
    {
	if (!convertTo.isArray())
	    return null;
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
	if (safe)
	    for (Enumeration enum = elems.elements();
		 enum.hasMoreElements(); i++)
	    {
		Array.set(res, i, converter.convert(enum.nextElement(), typeC,
						    converter));
	    }
	else {
	    ArrayConvertFail errs = null;
	    for (Enumeration enum = elems.elements();
		 enum.hasMoreElements(); i++)
	    {
		try {
		    Array.set(res, i,
			      converter.unsafeConvert(enum.nextElement(),
						      typeC,
						      converter));
		} catch (ConvertFail err){
		    if (errs == null) errs = new
			ArrayConvertFail(elems.size(), convertTo, toConvert);
		    errs.setError(i, err);
		}
	    }
	    if (errs != null)
		return errs;
	}
	return res;
    }
    /* ------------------------------------------------------------ */
    public static class ArrayConvertFail extends ConvertFail 
    {
	private ConvertFail errors[] = null;
	public ArrayConvertFail(int size, Class convertTo, Object toConvert){
	    super("Error converting element to Array Type",
		  convertTo, toConvert);
	    errors = new ConvertFail[size];
	}
	public ConvertFail[] getErrors(){
	    return errors;
	}
	void setError(int i, ConvertFail err){
	    errors[i] = err;
	}
    }
    /* ------------------------------------------------------------ */
}
