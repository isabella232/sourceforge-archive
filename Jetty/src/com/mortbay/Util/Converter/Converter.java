// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util.Converter;

import com.mortbay.Util.Code;

/** Interface for converting classes
 *
 * <p><h4>Notes</h4>
 * <p> A good check for implementors of this class as a first line is:
 * <pre>
 * if (toConvert.getClass().equals(convertTo)) return toConvert;
 * </pre>
 *
 * @see com.mortbay.Util.ConverterSet
 * @version 1.0 Thu Jun  8 2000
 * @author Matthew Watson (mattw)
 */
public interface Converter
{
    /* ------------------------------------------------------------ */
    /** Try to convert a value.
     * @param toConvert Value to convert
     * @param convertTo Type to convert to
     * @param context The context within which the converter was called.
     *                If Converters use other Converters, this is passed as
     *                the outermost Converter so that recursive calls have
     *                access to all available Converters. Converter
     *                implementations should pass this if passed null.
     * @return The converted value, or null if not possible
     */
    Object convert(Object toConvert,
		   Class convertTo,
		   Converter context);
    /* ------------------------------------------------------------ */
    /** Try to convert a value and report errors if conversion not totally
     * successful.
     * @param toConvert Value to convert
     * @param convertTo Type to convert to
     * @param context The context within which the converter was called.
     *                If Converters use other Converters, this is passed as
     *                the outermost Converter so that recursive calls have
     *                access to all available Converters. Converter
     *                implementations should pass this if passed null.
     * @return The converted value.
     * @exception ConvertFail If the conversion is not totally successful.
     */
    Object unsafeConvert(Object toConvert,
			 Class convertTo,
			 Converter context)
	throws ConvertFail;
    /* ------------------------------------------------------------ */
}
