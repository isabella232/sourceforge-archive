// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;

/** Interface for converting classes */
public interface Converter
{
    /* ------------------------------------------------------------ */
    /** Try ande convert a value.
     * A good check for implementors of this class as a first line is:
     * <pre>
     * if (toConvert.getClass().equals(convertTo)) return toConvert;
     * </pre>
     * @param toConvert Value to convert
     * @param convertTo Type to convert to
     * @param context The context within which the converter was called.
     *		      If Converters use other Converters, this is passed as
     *                the outermost Converter so that recursive calls have
     *                access to all available Converters. Converter
     *                implementations should pass this if passed null.
     * @return The converted value, or null if not possible
     */
    public Object convert(Object toConvert, Class convertTo,
			  Converter context);
    /* ------------------------------------------------------------ */
};
