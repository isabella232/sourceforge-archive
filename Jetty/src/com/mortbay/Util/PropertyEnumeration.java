// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.IntrospectionException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

/** Provide an Enumeration over a Classes Properties
 * Optionally, this class allows the user to enumerate over a Classes public
 * fields.
 * Utility methods allow properties or public fields to be set transparently
 */
public class PropertyEnumeration implements Enumeration
{
    /* ------------------------------------------------------------ */
    Hashtable types = new Hashtable();
    Enumeration enum = null;
    String current = null;
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param class_ The Class to Enumerate over
     * @param settable Include only properties that are settable
     * @param useFields Whether to include public fields or not.
     */
    public PropertyEnumeration(Class class_,
                               boolean settable, boolean useFields)
    {
        Vector names = new Vector();
        try {
            BeanInfo beanInf = Introspector.getBeanInfo(class_);
            PropertyDescriptor props[] = beanInf.getPropertyDescriptors();
            for (int i = 0; i < props.length; i++){
                if (settable && props[i].getWriteMethod() == null)
                    continue;
                String name = props[i].getName();
                Class type = props[i].getPropertyType();
                if (type != null){
                    names.addElement(name);
                    types.put(name, type);
                }
            }
        } catch (Exception ex){
            Code.debug("While BeanIntrospecting", ex);
        }
        if (useFields){
            Field fields[] = class_.getFields();
            for (int i = 0; i < fields.length; i++){
                int mod = fields[i].getModifiers();
                if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod)){
                    String name = fields[i].getName();
                    Class type = fields[i].getType();
                    names.addElement(name);
                    types.put(name, type);
                }
            }
        }
        enum = names.elements();
    }
    /* ------------------------------------------------------------ */
    public boolean hasMoreElements() {
        return enum.hasMoreElements();
    }
    /* ------------------------------------------------------------ */
    public Object nextElement() {
        return (current = enum.nextElement().toString());
    }
    /* ------------------------------------------------------------ */
    /** Get the type of the current property/field
     */
    public Class getType(){
        return (Class)types.get(current);
    }
    /* ------------------------------------------------------------ */
    /** Get the type of the named property/field
     */
    public Class getType(String name){
        return (Class)types.get(name);
    }
    /* ------------------------------------------------------------ */
    /** utility method for transparently setting a property or field
     * @param obj The object to set the value on
     * @param name The name of the property or field
     * @param value The value to set
     * @return true if it found the property/field and set it
     * @exception IllegalArgumentException If the value is of the wrong type
     * @exception InvocationTargetException If the set throws an Exception
     * @exception IllegalAccessException If the field is not public
     */
    public static boolean set(Object obj, String name, Object value)
        throws IllegalArgumentException,
               InvocationTargetException,
               IllegalAccessException
    {
        Code.debug("Set "+name+" on "+obj+"="+value+"("+value.getClass().getName()+")");
        try {
            BeanInfo beanInf = Introspector.getBeanInfo(obj.getClass());
            PropertyDescriptor props[] = beanInf.getPropertyDescriptors();
            for (int i = 0; i < props.length; i++){
                if (!name.equals(props[i].getName()))
                    continue;
                Method method = props[i].getWriteMethod();
                if (method == null) return false; // no set method
                Object params[] = new Object[1];
                params[0] = value;
                method.invoke(obj, params);
                return true;
            }
        } catch (IntrospectionException ex){
            Code.debug("While BeanIntrospecting", ex);
        }
        Field field = null;
        try {
            field = obj.getClass().getField(name);
        } catch (Exception ex){
            Code.debug("Looking up field:"+name, ex);
            return false;
        }
        int mod = field.getModifiers();
        if (!Modifier.isStatic(mod) && !Modifier.isFinal(mod)){
            field.set(obj, value);
            return true;
        } else
            Code.debug("Field "+name+" static or final");
        return false;
    }
    /* ------------------------------------------------------------ */
};
