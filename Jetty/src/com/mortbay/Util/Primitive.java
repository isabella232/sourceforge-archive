// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import java.util.HashMap;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/* ------------------------------------------------------------ */
/** Utility handling of primitive types 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class Primitive
{

    /* ------------------------------------------------------------ */
    private static final HashMap name2Class=new HashMap();
    static
    {
        name2Class.put("boolean",java.lang.Boolean.TYPE);
        name2Class.put("byte",java.lang.Byte.TYPE);
        name2Class.put("char",java.lang.Character.TYPE);
        name2Class.put("double",java.lang.Double.TYPE);
        name2Class.put("float",java.lang.Float.TYPE);
        name2Class.put("int",java.lang.Integer.TYPE);
        name2Class.put("long",java.lang.Long.TYPE);
        name2Class.put("short",java.lang.Short.TYPE);
        name2Class.put("void",java.lang.Void.TYPE);
        name2Class.put("java.lang.Boolean.TYPE",java.lang.Boolean.TYPE);
        name2Class.put("java.lang.Byte.TYPE",java.lang.Byte.TYPE);
        name2Class.put("java.lang.Character.TYPE",java.lang.Character.TYPE);
        name2Class.put("java.lang.Double.TYPE",java.lang.Double.TYPE);
        name2Class.put("java.lang.Float.TYPE",java.lang.Float.TYPE);
        name2Class.put("java.lang.Integer.TYPE",java.lang.Integer.TYPE);
        name2Class.put("java.lang.Long.TYPE",java.lang.Long.TYPE);
        name2Class.put("java.lang.Short.TYPE",java.lang.Short.TYPE);
        name2Class.put("java.lang.Void.TYPE",java.lang.Void.TYPE);
        name2Class.put(null,java.lang.Void.TYPE);
        name2Class.put("string",java.lang.String.class);
        name2Class.put("String",java.lang.String.class);
        name2Class.put("java.lang.String",java.lang.String.class);
    }
    
    /* ------------------------------------------------------------ */
    private static final HashMap class2Name=new HashMap();
    static
    {
        class2Name.put(java.lang.Boolean.TYPE,"boolean");
        class2Name.put(java.lang.Byte.TYPE,"byte");
        class2Name.put(java.lang.Character.TYPE,"char");
        class2Name.put(java.lang.Double.TYPE,"double");
        class2Name.put(java.lang.Float.TYPE,"float");
        class2Name.put(java.lang.Integer.TYPE,"int");
        class2Name.put(java.lang.Long.TYPE,"long");
        class2Name.put(java.lang.Short.TYPE,"short");
        class2Name.put(java.lang.Void.TYPE,"void");
        class2Name.put(null,"void");
        name2Class.put(java.lang.String.class,"java.lang.String");
    }
    
    /* ------------------------------------------------------------ */
    private static final HashMap class2Value=new HashMap();
    static
    {
        try
        {
            Class[] s ={java.lang.String.class};
            
            class2Value.put(java.lang.Boolean.TYPE,
                           java.lang.Boolean.class.getMethod("valueOf",s));
            class2Value.put(java.lang.Byte.TYPE,
                           java.lang.Byte.class.getMethod("valueOf",s));
            class2Value.put(java.lang.Double.TYPE,
                           java.lang.Double.class.getMethod("valueOf",s));
            class2Value.put(java.lang.Float.TYPE,
                           java.lang.Float.class.getMethod("valueOf",s));
            class2Value.put(java.lang.Integer.TYPE,
                           java.lang.Integer.class.getMethod("valueOf",s));
            class2Value.put(java.lang.Long.TYPE,
                           java.lang.Long.class.getMethod("valueOf",s));
            class2Value.put(java.lang.Short.TYPE,
                           java.lang.Short.class.getMethod("valueOf",s));
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
    }

    /* ------------------------------------------------------------ */
    public static Class fromName(String name)
    {
        return (Class)name2Class.get(name);
    }
    
    /* ------------------------------------------------------------ */
    public static String toName(Class primitive)
    {
        return (String)class2Name.get(primitive);
    }
    
    /* ------------------------------------------------------------ */
    public static Object valueOf(Class type, String value)
    {
        try
        {
            if (type.equals(java.lang.Character.TYPE))
                return new Character(value.charAt(0));
            
            if (type.equals(java.lang.String.class))
                return value;

            Method m = (Method)class2Value.get(type);
            if (m!=null)
                return m.invoke(null,new Object[] {value});
        }
        catch(IllegalAccessException e)
        {
            Code.ignore(e);
        }
        catch(InvocationTargetException e)
        {
            if (e.getTargetException() instanceof Error)
                throw (Error)(e.getTargetException());
            Code.ignore(e);
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public static Object valueOf(String type, String value)
    {
        return valueOf(fromName(type),value);
    }


    

}

        
