// ========================================================================
// Copyright 2000 (c) Mortbay Consulting
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import com.mortbay.Base.Test;

import java.util.Hashtable;
import java.util.Dictionary;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;
import java.lang.reflect.Array;

/** Class to convert Objects into PropertyTrees.
 * <p> This class is a Converter that handles converting of arbitrary
 * Objects into PropertyTrees. This will work well with objects that have
 * public fields or have well-formed get/set pairs enabling their
 * deconstruction/reconstruction. The resultant PropertyTree can then be
 * output to a file and re-read in then converted back to an Object using the
 * ObjectConverter.
 * <p>The conversion continues recursively until object are resolved down to
 * primitive types.
 * (@see com.mortbay.Util.ConverterSet.PrimitiveConverter).
 *
 * <p><h4>Note</h4>
 * <p> Note that since this Converter handles ALL types, if asked to convert
 * to a PropertyTree, it will convert everything it is given. Arrays don't
 * convert too well, unless they are arrays of primitive types.
 *
 * <p><h4>Usage</h4>
 * <pre>
 * Converter cnv = new PropertyTreeConverter();
 * cnv.convert(object, PropertyTree.class, cnv);
 * </pre>
 *
 * @version 1.0 Fri Jun  9 2000
 * @author Matthew Watson (mattw)
 */
public class PropertyTreeConverter extends ConverterBase
{
    /* ------------------------------------------------------------ */
    protected Object doConvert(Object toConvert,
			       Class convertTo,
			       Converter context,
			       boolean safe)
    {
	return doConvert(null, null, toConvert);
    }
    /* ------------------------------------------------------------ */
    protected Object doConvert(PropertyTree to,
			       String prefix,
			       Object toConvert)
    {
	// Since the PropertyTreeConverter handles everything, we don't
	// really need to worry about other type converters, so we can call
	// ourself recursively...
	
	// Primitive type = OK their toString will do the right thing
	if (toConvert instanceof Boolean ||
	    toConvert instanceof Byte ||
	    toConvert instanceof Character ||
	    toConvert instanceof Number ||
	    toConvert instanceof String)
	    return toConvert.toString();
	// Array or Vector - convert the idividual objects, hoping they are
	// primitive types and turn them into a comma-seperated string
	if (toConvert.getClass().isArray()){
	    StringBuffer sb = new StringBuffer();
	    int length = Array.getLength(toConvert);
	    for (int i = 0; i < length; i++){
		sb.append(doConvert(null, null, Array.get(toConvert, i)));
		if (i + 1 != length)
		    sb.append(",");
	    }
	    return sb.toString();
	}
	// Object - return a PropertyTree whose keys are field names.
	if (to == null)
	    to = new PropertyTree();

	PropertyEnumeration enum = new
	    PropertyEnumeration(toConvert.getClass(), true, true);
	while (enum.hasMoreElements()){
	    String prop = enum.nextElement().toString();
	    try {
		Object val = PropertyEnumeration.get(toConvert, prop);
		String key = prefix == null ? prop : prefix + "." + prop;
		val = doConvert(to, key, val);
		if (val instanceof String)
		    to.setProperty(key, val.toString());
	    } catch (Exception ex){ Code.debug(ex); }
	}	    
	return to;
    }
    /* ------------------------------------------------------------ */
}
