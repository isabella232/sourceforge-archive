// ========================================================================
// Copyright 2000 (c) Mortbay Consulting
// $Id$
// ========================================================================

package com.mortbay.Util.Converter;

import java.util.Hashtable;
import java.util.Dictionary;
import java.util.Vector;
import java.util.Enumeration;
import java.lang.reflect.Array;

/** Class to transform leaves in trees of containers
 * <p> Objects of this class are not Thread-safe.
 *
 * <p> This class is a Transformer that handles Transformation of the leaves
 * of Container object types (specifically, Vectors and Dictionaries) - it
 * iterates over the containers and recursively tries to transform the leaf
 * elements. Any leaf elements that are transformed are replaced in place in
 * the container.
 *
 * <p> Array elements can also be transformed - see the <a
 * href=#note>note</a> below.
 *
 * <p> The user can install this Transformer in a ConverterSet and add
 * Transformers to handle the types they would like transformed to the same
 * set.
 *
 * <p><h4><a name=note>Note</a></h4>
 * <p> Arrays are set types in java and thus not suitable for conversion or
 * transformation unless being handled as Objects. In this case it is better,
 * however, to use Vectors instead - they are more generic and dynamically
 * sized. If you need to transform or convert objects in an array to a
 * different type, use the constructor for handling arrays, but make sure
 * your transformers will convert only to types your Array can handle. The
 * base type of the Array will be passed to the Transformer as the convertTo
 * parameter when transforming array elements.
 *
 * <p><h4>Usage</h4>
 * <pre>
 * ConverterSet cs = new ConverterSet();
 * cs.setTransformMode(true);
 * cs.register(new XXXTransformer());
 * cs.register(new ContainerIteratorTransformer());
 * cs.convert(hashtable, cs.getClass(), cs);
 * </pre>
 * <code>Note</code> that it is required for the user to setTransformMode on
 * the ConverterSet which, by default, operates in Converter mode.
 *
 * @see com.mortbay.Util.ConverterSet
 * @version 1.0 Fri Jun  9 2000
 * @author Matthew Watson (mattw)
 */
public class ContainerIteratorTransformer extends ConverterBase
{
    /* ------------------------------------------------------------ */
    private Object baseObject = null;
    private boolean arrays = false;
    /* ------------------------------------------------------------ */
    public ContainerIteratorTransformer(){
	setTransformMode(true);
    }
    /* ------------------------------------------------------------ */
    /** Constructor for handling array element transformation
     * @param arrays Set to true for array element transformation
     */
    public ContainerIteratorTransformer(boolean arrays){
	setTransformMode(true);
	this.arrays = arrays;
    }
    /* ------------------------------------------------------------ */
    /** Return the root object being transformed by this Transformer. This
     * will be the object passed into the outermost invokation of convert().
     * @return The root object being transformed by this Transformer.
     */
    public Object getBaseObject(){
	return baseObject;
    }
    /* ------------------------------------------------------------ */
    protected Object doConvert(Object toConvert,
			       Class convertTo,
			       Converter context,
			       boolean safe)
    {
	boolean setBase = (baseObject == null);
	if (setBase) baseObject = toConvert;
	try {
	    ContainerTransformFail errors = null;
	    if (arrays && toConvert.getClass().isArray()){
		int length = Array.getLength(toConvert);
		for (int i = 0; i < length; i++){
		    Object elem = Array.get(toConvert, i);
		    Object newVal = null;
		    Class type = toConvert.getClass().getComponentType();
		    try {
			newVal =
			    (safe ?
			     context.convert(elem, type, context) :
			     context.unsafeConvert(elem, type, context));
			if (newVal != elem && newVal != null)
			    Array.set(toConvert, i, newVal);
		    } catch (ConvertFail cf){
			if (errors == null) errors = new
			    ContainerTransformFail("Transforming Element(s)",
						   toConvert);
			errors.setError(Integer.toString(i), cf);
		    } catch (IllegalArgumentException ia){
			if (errors == null) errors = new
			  ContainerTransformFail("Array element type mismatch",
						 toConvert);
			ConvertFail err = new
			    ConvertFail("WrongType:"+
					newVal.getClass().getName(),
					type, elem);
			errors.setError(Integer.toString(i), err);
		    }
		}
		if (errors != null) return errors;
		return toConvert;
	    } else if (toConvert instanceof Vector){
		int i = 0;
		Vector v = (Vector)toConvert;
		for (Enumeration enum = v.elements();
		     enum.hasMoreElements(); i++)
		{
		    Object elem = enum.nextElement();
		    try {
			Object newVal =
			    (safe ?
			     context.convert(elem, convertTo, context) :
			     context.unsafeConvert(elem, convertTo, context));
			if (newVal != elem && newVal != null)
			    v.setElementAt(newVal, i);
		    } catch (ConvertFail cf){
			if (errors == null) errors = new
			    ContainerTransformFail("Transforming Element(s)",
						   toConvert);
			errors.setError(Integer.toString(i), cf);
		    }
		}
		if (errors != null) return errors;
		return v;
	    } else if (toConvert instanceof Dictionary){
		Dictionary dict = (Dictionary)toConvert;
		for (Enumeration enum = dict.keys(); enum.hasMoreElements();){
		    Object elem = enum.nextElement();
		    Object newKey = null;
		    Object newVal = null;
		    Object oldVal = dict.get(elem);
		    try {
			newKey = (safe ?
				  context.convert(elem, convertTo, context) :
				  context.unsafeConvert(elem, convertTo,
							context));
		    } catch (ConvertFail cf){
			if (errors == null) errors = new
			    ContainerTransformFail("Transforming Element(s)",
						   toConvert);
			errors.setError(elem + "(key)", cf);
		    }
		    try {
			newVal = (safe ?
				  context.convert(oldVal, convertTo, context) :
				  context.unsafeConvert(oldVal, convertTo,
							context));
		    } catch (ConvertFail cf){
			if (errors == null) errors = new
			    ContainerTransformFail("Transforming Element(s)",
						   toConvert);
			errors.setError(elem.toString(), cf);
		    }
		    if (newKey != null && elem != newKey){
			dict.remove(elem);
			dict.put(newKey, (newVal != null ? newVal : oldVal));
		    } else if (newVal != null && oldVal != newVal){
			dict.put(elem, newVal);
		    }
		}
		if (errors != null) return errors;
		return dict;
	    } else
		return null;
	} finally {
	    if (setBase) baseObject = null;
	}
    }
    /* ------------------------------------------------------------ */
    public static class ContainerTransformFail extends ConvertFail
    {
	private Hashtable errors = new Hashtable();
	public ContainerTransformFail(String desc, Object toConvert)
	{
	    super(desc, null, toConvert);
	}
	public Hashtable getErrors(){
	    return errors;
	}
	void setError(String field, ConvertFail err){
	    if (err instanceof ContainerTransformFail){
		ContainerTransformFail suberrors = (ContainerTransformFail)err;
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
}
