// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.tools.converter;

import org.mortbay.util.Code;
import org.mortbay.tools.PropertyEnumeration;
import org.mortbay.tools.PropertyTree;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Enumeration;

/** Class to convert Dictionary's (including PropertyTrees) to complex object
 * types.
 * This class tries to convert a Dictionary (Hashtable or Properties etc) to
 * a complex Object. This is done by iterating over all the public settable
 * bean Properties and fields of an Object and trying to set them from values
 * in the Dictionary. Types in the dictionary will be converted using the
 * context as passed to this Converter, so the user should make sure that
 * this Converter is part of a ConverterSet capable of handling the required
 * conversions.
 */
public class ObjectConverter extends ConverterBase
{
    /* ------------------------------------------------------------ */
    protected Object doConvert(Object toConvert,
			       Class convertTo,
			       Converter context,
			       boolean safe)
    {
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
	    Converter converter = context == null ? this : context;
	    Dictionary dict = (Dictionary)toConvert;
	    ObjectConvertFail error = fillObject(obj, dict, converter, safe);
	    if (!safe && error != null) return error;
        } catch (ClassCastException ex) {
	    Code.debug("Not a Dictionary:", toConvert);
	    if (!safe){
		return new ObjectConvertFail
		    ("Object to convert is not a Dictionary!",
		     convertTo, toConvert);
	    }
            return null;
	} catch (Exception ex){
            Code.debug("While instantiating ", convertTo.getName(), ex);
	    if (!safe){
		return 
		    new ObjectConvertFail("Error instantiating type",
					  convertTo, toConvert);
	    }
            return null;
        }
        return obj;
    }
    /* ------------------------------------------------------------ */
    public static class ObjectConvertFail extends ConvertFail
    {
	private Hashtable errors = new Hashtable();
	public ObjectConvertFail(String desc, Class convertTo,
				 Object toConvert)
	{
	    super(desc, convertTo, toConvert);
	}
	public Hashtable getErrors(){
	    return errors;
	}
	void setError(String field, ConvertFail err){
	    if (err instanceof ObjectConvertFail){
		ObjectConvertFail suberrors = (ObjectConvertFail)err;
		for (Enumeration enum = suberrors.getErrors().keys();
		     enum.hasMoreElements();)
		{
		    String key = enum.nextElement().toString();
		    errors.put(field + "." + key,
			       suberrors.getErrors().get(key));
		}
	    } else
		errors.put(field, err);
	}
	public String toString(){
	    return super.toString() + ";Errors:"+errors;
	}
    }
    /* ------------------------------------------------------------ */
    public static ObjectConvertFail fillObject(Object toFill,
					       Dictionary from,
					       Converter converter,
					       boolean safe)
    {
	ObjectConvertFail errors = null;
	if (!safe) errors = new ObjectConvertFail("Error converting field(s)",
						  toFill.getClass(),
						  from);
        PropertyEnumeration enum =
            new PropertyEnumeration(toFill.getClass(), true, true);
        while (enum.hasMoreElements()){
            String field = enum.nextElement().toString();
            Object value = from.get(field);
            if (from instanceof PropertyTree){
                if (value != null)
		    value = convertValue(converter, value, enum.getType(),
					 errors, field);
                if (value == null){
                    PropertyTree tree = (PropertyTree)from;
                    value = tree.getTree(field);
		    // An empty property tree - nothing to convert here!
		    if (((PropertyTree)value).size() != 0)
			value = convertValue(converter, value, enum.getType(),
					     errors, field);
                }
            } else {
                if (value == null || value.toString().length()==0) continue;
		value = convertValue(converter, value, enum.getType(),
				     errors, field);
            }
            if (value == null) continue;
            try {
                boolean done = PropertyEnumeration.set(toFill, field, value);
            } catch (Exception ex){
                Code.debug("While setting value "+field+"="+value, ex);
            }
        }
	if (errors != null && errors.getErrors().size() != 0)
	    return errors;
	return null;
    }
    /* ------------------------------------------------------------ */
    private static Object convertValue(Converter converter,
				       Object value,
				       Class type,
				       ObjectConvertFail errors,
				       String field)
    {
	if (errors != null){
	    try {
		return converter.unsafeConvert(value, type, converter);
	    } catch (ConvertFail ex) {
		errors.setError(field, ex);
	    }
	} else {
	    return converter.convert(value, type, converter);
	}
	return null;
    }
    /* ------------------------------------------------------------ */
}
