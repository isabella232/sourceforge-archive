/*
  GNUJSP - a free JSP1.0 implementation
  Copyright (C) 1999, Yaroslav Faybishenko <yaroslav@cs.berkeley.edu>

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, =
  USA.
*/
package org.gjt.jsp;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletException;

/**
 * This contains utility functions formerly in HttpJspPageImpl.
 * They are independant of the object itself, so they
 * are separated to reduce dependancies on HttpJspPageImpl.
 */

public class HttpJspPageUtil
    implements JspMsg
{
    final static public Object getProperty (Object o, String name, String beanName) 
	throws Exception {
	if(o == null)
	    throw new ServletException(
	        JspConfig.getLocalizedMsg(ERR_sp10_2_13_3_bean_is_null)
		+": "+beanName);

	BeanInfo             info = Introspector.getBeanInfo (o.getClass ());
	PropertyDescriptor[] pd   = info.getPropertyDescriptors ();
	Object               val  = null;

	for (int i = 0;  pd != null && i < pd.length;  i++)
	   
	    if (name.equals (pd[i].getName ())) {
		return pd[i].getReadMethod ().invoke (o, new Object[] { });
	    }
	throw new ServletException(
	    JspConfig.getLocalizedMsg(ERR_sp10_2_13_3_property_not_found)
	    +": " + name);
    }

    final static public Object objectify(boolean b) { return new Boolean(b); }
    final static public Object objectify(byte b) { return new Byte(b); }
    final static public Object objectify(char c) { return new Character(c); }
    final static public Object objectify(double d) { return new Double(d); }
    final static public Object objectify(float f) { return new Float(f); }
    final static public Object objectify(int i) { return new Integer(i); }
    final static public Object objectify(long l) { return new Long(l); }
    final static public Object objectify(short s) { return new Short(s); }
    final static public Object objectify(Object o) { return o; }

    final static public void setProperty (Object o, String name, Object val)
	throws Exception
    {
	BeanInfo             info = Introspector.getBeanInfo (o.getClass ());
	PropertyDescriptor[] pd   = info.getPropertyDescriptors ();
	boolean done=false;

	for (int i = 0;  pd != null && i < pd.length;  i++)
	   
	    if (name.equals (pd[i].getName ())) {
		set (o, pd[i], val);
		done=true;
		break;
	    }

	if(!done) {
	    throw new ServletException(
	       JspConfig.getLocalizedMsg(ERR_sp10_2_13_2_property_not_found)
		+ ": " + name);
	}

    }
   
    final static public void setProperties (Object o, HttpServletRequest req)
	throws Exception
    {
	Enumeration e = req.getParameterNames ();
	BeanInfo info = Introspector.getBeanInfo (o.getClass ());
	PropertyDescriptor[] pd = info.getPropertyDescriptors ();
	String name = null;
	String[] values = null;

	if (pd != null) {

	    while (e.hasMoreElements ()) {
		name  = (String) e.nextElement ();
	
		for (int i = 0; i < pd.length; i++) {
		   
		    if (name.equals (pd[i].getName ())) {
		
			if ( (values = req.getParameterValues (name)) != null ) {
			    // If property is String[] type, use that.
			    if (pd[i].getPropertyType().equals(String[].class)) {
				set(o, pd[i], values);
			    }
			    for (int j = 0; j < values.length; j++) {
				String s = values[j];
				// 2.13.2 If parameter has a value of "" the
				// corresponding property is not modified.
				if(s != null && !"".equals(s))
				    set (o, pd[i], s);
			    }
			}
		    }
		}
	    }
	}
    }


    final static void set (Object o, PropertyDescriptor pd, Object val) 
	throws Exception {
	Method setter = pd.getWriteMethod ();
	Class  type   = null;

	if(setter == null) {
	    throw new ServletException(
		  JspConfig.getLocalizedMsg(ERR_sp10_2_13_2_setter_not_found)
		  +" '"+pd.getName()+"'");
	}
	type = setter.getParameterTypes()[0];

	// If we're setting a non-array type from a String[] value,
	// use only the first element of the array.
	if (!type.isArray() && val != null && val instanceof String[]) {
	    String[] saVal = (String[]) val;
	    val = (saVal.length == 0) ? null : saVal[0];
	}

	// FIXME: should we also convert String[] to arrays of other types?
	// For example, request parameters x=1&x=2, trying to set an int[]
	// parameter?  Spec is unclear. (Wes 15 Oct 1999)

	// FIXME: deal with indexed properties correctly. (is this wrong?)

	if (val instanceof String)
	    setter.invoke (o, new Object[] { convert ((String) val, type,
						      pd.getName()) });
	else
	    setter.invoke (o, new Object[] { val } );
    }

    final static Object convert (String sval, Class t, String attrName) 
	throws Exception
    {
	String name = t.getName ();
	Object val  = null;

	if (t.equals (String.class))
	    val = sval;
	else if (t.equals (Boolean.class) || name.equals ("boolean"))
	    val = Boolean.valueOf (sval);
	else if (t.equals (Integer.class) || name.equals ("int"))
	    val = Integer.valueOf (sval);
	else if (t.equals (Byte.class) || name.equals ("byte"))
	    val = Byte.valueOf (sval);
	else if (t.equals (Character.class) || name.equals ("char")) 
	    val = new Character (sval.charAt (0));
	else if (t.equals (Double.class) || name.equals ("double"))
	    val = Double.valueOf (sval);
	else if (t.equals (Float.class) || name.equals ("float"))
	    val = Float.valueOf (sval);
	else if (t.equals (Long.class) || name.equals ("long"))
	    val = Long.valueOf (sval);
	else if (t.equals (Short.class) || name.equals ("short"))
	    val = Short.valueOf (sval);
	// FIXME: introduce another type of Exception (JspRuntime?)
	else 
	    throw new ServletException(
	        JspConfig.getLocalizedMsg(ERR_sp10_2_13_2_impossible_string_conversion)
		+ ": " + attrName);

	return val;
    }
}
