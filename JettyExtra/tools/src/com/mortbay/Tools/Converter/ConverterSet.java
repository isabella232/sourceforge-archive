// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Tools.Converter;

import com.mortbay.Util.Code;
import java.util.Vector;
import java.util.Enumeration;
import java.math.BigInteger;
import java.math.BigDecimal;

/** Class to handle converting of types from one to another.
 * Other Converters can be registered with this ConverterSet and it will try
 * them  all in turn until it finds one that works.
 *
 * This class also provides a set of "standard" converters that handle the
 * primitive types, that can be installed by calling
 * registerPrimitiveConverters(). 
 */
public class ConverterSet extends  ConverterBase
{
    private Vector converters = null;
    /* ------------------------------------------------------------ */
    protected Object doConvert(Object toConvert,
			       Class convertTo,
			       Converter context,
			       boolean safe)
    {
	ConvertFail error = null;
        if (converters != null)
            for (Enumeration enum = converters.elements();
                 enum.hasMoreElements();){
                Converter converter = (Converter)enum.nextElement();
		Object retv = null;
		if (safe){
		    retv = converter.convert(toConvert, convertTo,
					     (context == null ?
					      this : context));
		} else {
		    try {
			retv = converter.unsafeConvert(toConvert, convertTo,
						       (context == null ?
							this : context));
		    } catch (ConvertFail ex){
			retv = null;
			error = ex;
		    }
		}
		if (retv != null) return retv;
            }
	if (error != null)
	    return error;
        return null;
    }
    /* ------------------------------------------------------------ */
    public void register(Converter converter){
        if (converters == null) converters = new Vector();
        converters.addElement(converter);
    }
    /* ------------------------------------------------------------ */
    public static class PrimitiveConverter extends ConverterBase
    {
        private static Class primitives[] = null;
        public static final Boolean aBoolean    = Boolean.TRUE;
        public static final Byte aByte          = new Byte(Byte.MIN_VALUE);
        public static final Character aCharacter= new Character(Character.
                                                                MIN_VALUE);
        public static final Double aDouble      = new Double(Double.MIN_VALUE);
        public static final Float aFloat        = new Float(Float.MIN_VALUE);
        public static final Integer aInteger    = new Integer(Integer.
                                                              MIN_VALUE);
        public static final Long aLong          = new Long(Long.MIN_VALUE);
        public static final Short aShort        = new Short(Short.MIN_VALUE);
	public static final BigInteger aBigInt  = BigInteger.valueOf(0L);
	public static final BigDecimal aBigDec  = new BigDecimal(0d);
        /* -------------------------------------------------------- */
        /** Return an Array of all the primitive class types */
        public static Class[] getPrimitives(){
            synchronized (aByte){
                if (primitives == null){
                    primitives = new Class[19];
                    primitives[ 0] = aBoolean.getClass();
                    primitives[ 1] = Boolean.TYPE;
                    primitives[ 2] = aByte.getClass();
                    primitives[ 3] = Byte.TYPE;
                    primitives[ 4] = aCharacter.getClass();
                    primitives[ 5] = Character.TYPE;
                    primitives[ 6] = aDouble.getClass();
                    primitives[ 7] = Double.TYPE;
                    primitives[ 8] = aFloat.getClass();
                    primitives[ 9] = Float.TYPE;
                    primitives[10] = aInteger.getClass();
                    primitives[11] = Integer.TYPE;
                    primitives[12] = aLong.getClass();
                    primitives[13] = Long.TYPE;
                    primitives[14] = aShort.getClass();
                    primitives[15] = Short.TYPE;
                    primitives[16] = "".getClass();
		    primitives[17] = aBigInt.getClass();
		    primitives[18] = aBigDec.getClass();
                }
            }
            return primitives;
        }
        /* -------------------------------------------------------- */
        protected Object doConvert(Object toConvert, Class convertTo,
				   Converter context, boolean safe)
	{
            Class primitives[] = getPrimitives();
            try {
                for (int i = 0; i < primitives.length; i++){
                    if (primitives[i].equals(convertTo)){
                        String value = toConvert.toString();
                        if (i == 0 || i == 1)
                            return Boolean.valueOf(value);
                        else if (i == 2 || i == 3)
                            return Byte.valueOf(value);
                        else if (i == 4 || i == 5)
                            return new Character(value.charAt(0));
                        else if (i == 6 || i == 7)
                            return Double.valueOf(value);
                        else if (i == 8 || i == 9)
                            return Float.valueOf(value);
                        else if (i == 10 || i == 11)
                            return Integer.valueOf(value);
                        else if (i == 12 || i == 13)
                            return Long.valueOf(value);
                        else if (i == 14 || i == 15)
                            return Short.valueOf(value);
                        else if (i == 17)
			    return new BigInteger(value);
			else if (i == 18)
			    return new BigDecimal(value);
			else
                            return value;
                    }
                }
            } catch (Exception ex){
                Code.debug("Cant Convert", ex);
		if (!safe)
		    return new ConvertFail(ex.toString(),
					   convertTo,
					   toConvert);
            }
            return null;
        }
        /* -------------------------------------------------------- */
    }
    /* ------------------------------------------------------------ */
    /** register the standard converters for the primitive types */
    public void registerPrimitiveConverters()
    {
        register(new PrimitiveConverter());
    }
    /* ------------------------------------------------------------ */
}
