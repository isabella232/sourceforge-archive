// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import java.util.Dictionary;

/** Class to convert Dictionary's to complex object types
 * This class tries to convert a Dictionary (Hashtable or Properties etc) to
 * a complex Object. This is done by iterating over all the public settable
 * bean Properties and fileds of an Object and trying to set them from values
 * in the Dictionary. Types in the dictionary will be converted using the
 * context as passed to this Converter, so the user should make sure that
 * this Converter is part of a ConverterSet capable of handling the requires
 * conversions.
 */
public class DictionaryConverter implements Converter
{
    /* ------------------------------------------------------------ */
    public Object convert(Object toConvert, Class convertTo,
			  Converter context)
    {
	if (toConvert.getClass().equals(convertTo))
	    // Already correct type!
	    return toConvert;
	// Make sure we have a dictionary
	if (!(toConvert instanceof Dictionary))
	    return null;
	// Check its not a primitive type
	Class[] primitives = ConverterSet.PrimitiveConverter.getPrimitives();
	for (int i = 0; i < primitives.length; i++)
	    if (primitives[i].equals(convertTo)) return null;
	// Get an instance
	Object obj = null;
	try {
	    obj = convertTo.newInstance();
	} catch (Exception ex){
	    Code.debug("While instantiating "+convertTo.getName(), ex);
	    return null;
	}
	Converter converter = context == null ? this : context;
	Dictionary dict = (Dictionary)toConvert;
	fillObject(obj, dict, converter);
	return obj;
    }
    /* ------------------------------------------------------------ */
    public static void fillObject(Object toFill, Dictionary from,
				  Converter converter)
    {
	PropertyEnumeration enum =
	    new PropertyEnumeration(toFill.getClass(), true, true);
	while (enum.hasMoreElements()){
	    String field = enum.nextElement().toString();
	    Object value = from.get(field);
	    if (value == null) continue;
	    value = converter.convert(value, enum.getType(), converter);
	    if (value == null) continue;
	    try {
		boolean done = PropertyEnumeration.set(toFill, field, value);
	    } catch (Exception ex){
		Code.debug("While setting value "+field+"="+value, ex);
	    }
	}
    }
    /* ------------------------------------------------------------ */
};
