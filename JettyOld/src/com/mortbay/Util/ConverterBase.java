// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;

/** Class to add support for Converter writers.
 * <p> This class adds some support functions for Converters. It implements
 * the Converter interface and instead provides the doConvert() function,
 * that must be overridden. This class checks whether the object to be
 * converted is null or already of the correct type and handles those cases.
 *
 * <p> The doConvert() function takes a boolean flag to indicate whether this
 * is a safe conversion (has no error reporting) or an unsafe conversion
 * (error reporting). If unsafe, then an error should be reported if the
 * Converter handles the type to convert to but was unable to convert the
 * value. Errors are reported by returning a ConvertFail exception. This is
 * then thrown from the unsafeConvert() function. Note the implementor is
 * expected to call the correct conversion function if the context is used.
 *
 * <p> Converters for more complicated types (Arrays, complex objects, etc)
 * should report ConvertMultiFail exceptions and attempt to convert all the
 * elements of the type they are converting to so all errors are reported at
 * once.
 *
 * <p><h4>Notes</h4>
 * <p> Converters can operate under a different paradigm, I call
 * tranformation. Transformers derive from this class also, but must call
 * setTransformMode(true) in their constructors (and should only be placed in
 * ConverterSets where setTransformMode(true) has been called). Transformers
 * don't convert TO a type, but rather, look at the type they have to convert
 * FROM and decide if they want to transform it. Since the mechanism is
 * identical, I am using the Converter framework, but calling the relevant
 * classes Transformers.
 *
 * @see com.mortbay.Util.Converter
 * @version 1.0 Thu Jun  8 2000
 * @author Matthew Watson (mattw)
 */
public abstract class ConverterBase implements Converter
{
    /* ------------------------------------------------------------ */
    private boolean transform = false;
    /* ------------------------------------------------------------ */
    public Object convert(Object toConvert, Class convertTo, Converter context)
    {
	if (toConvert == null) return null;
        if (!transform && toConvert.getClass().equals(convertTo))
            // Already correct type!
            return toConvert;
	return doConvert(toConvert, convertTo, context, true);
    }
    /* ------------------------------------------------------------ */
    public Object unsafeConvert(Object toConvert,
				Class convertTo,
				Converter context)
	throws ConvertFail
    {
	if (toConvert == null) return null;
        if (!transform && toConvert.getClass().equals(convertTo))
            // Already correct type!
            return toConvert;
	Object obj = doConvert(toConvert, convertTo, context, false);
	if (obj instanceof ConvertFail)
	    throw (ConvertFail)obj;
	return obj;
    }
    /* ------------------------------------------------------------ */
    /** Set this ConverterSet into Transform mode. In this mode, the type of
     * the Object to be converted is paramount, rather than the type to be
     * converted to. This must be set if transforming objects where the value
     * of the parameter convertTo in the convert call is irrelevant,
     * otherwise the ConverterSet performs certain optimisations that will
     * cause Transformers not to function.
     * @param on 
     */
    public void setTransformMode(boolean on){
	transform = on;
    }
    /* ------------------------------------------------------------ */
    /** Convert an Object to another type.
     * @param toConvert Value to convert
     * @param convertTo Type to convert to
     * @param context The context within which the converter was called.
     *                If Converters use other Converters, this is passed as
     *                the outermost Converter so that recursive calls have
     *                access to all available Converters. Converter
     *                implementations should pass this if passed null.
     * @param safe If false, errors should be returned.
     * @return null if this converter doesn't handle this type to convertTo,
     * or a ConvertFail exception if there was an error and safe is false.
     */
    protected abstract Object doConvert(Object toConvert,
					Class convertTo,
					Converter context,
					boolean safe);
    /* ------------------------------------------------------------ */
}
