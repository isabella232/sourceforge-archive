// ========================================================================
// Copyright 2000 (c) Mortbay Consulting Ltd.
// $Id$
// ========================================================================

package com.mortbay.Tools.Converter;

/** A Converter error occurred
 *
 * @see com.mortbay.Util.Converter
 * @version 1.0 Thu Jun  8 2000
 * @author Matthew Watson (mattw@mortbay.com)
 */
public class ConvertFail extends Exception
{
    /* ------------------------------------------------------------ */
    private Class convertTo ;
    private Object toConvert ;
    /* ------------------------------------------------------------ */
    /** 
     * @param desc Description of error
     * @param convertTo The type trying to convert to.
     * @param toConvert The object trying to convert.
     */
    public ConvertFail(String desc, Class convertTo, Object toConvert) {
	super(desc);
	this.convertTo = convertTo;
	this.toConvert = toConvert;
    }
    /* ------------------------------------------------------------ */
    public Class getConvertTo()
    {
	return convertTo;
    }
    public Object getToConvert()
    {
	return toConvert;
    }
    public String toString(){
	return super.toString() +
	    ";ConvertTo:"+convertTo+";ToConvert:"+toConvert;
    }
    /* ------------------------------------------------------------ */
}
