// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Filter;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/* --------------------------------------------------------------------- */
/** A MethodTag is a string representing a call to a method on a class.
 * It uses reflection to determine the validity of the method and class
 * it
 * <H3>Notes</H3>
 * Arguement types supported include:<UL>
 * <LI>Quoted strings
 * <LI>Instances of Double
 * <LI>Named objects that are looked up against the dictionary
 *     passed at construction.
 * <LI>HttpRequest parameters looked up against the request passed
 *     at construction.
 * Due to limitations in the reflection classes, method overload
 * resolution has the following restrictions: <UL>
 * <LI>Primitive types are not yet supported.
 * <LI>The only Number supported is Double
 * <LI>The arg "null" is typed as a String for locating a method.
 * </UL>
 */
public class MethodTag
{
    /* ----------------------------------------------------------------- */
    String tag;
    Dictionary namedArgs;
    HttpRequest request;
    
    /* ----------------------------------------------------------------- */
    public String tagClassName;
    public Class tagClass;
    public String tagMethodName;
    public Method tagMethod;
    public String tagArgsString;
    public Object[] tagArgs;
    
    /* ----------------------------------------------------------------- */
    /** Contruct tag
     * With a pathentic parser, with little error handling.
     * @param tag String of the form "package.name.class.method(args,...)"
     *        Currently arguments can be named args (see below), longs,
     *        Strings or null.
     * @param namedArgs Dictionary of named arguements. Arg names in the tag
     *        string are looked up in this Dictionary. Can be null.
     * @param request A HttpRequest.  If the named arguments are not
     *        found in namedArgs, the request parameters are tried.
     *        Can be null
     */
    public MethodTag(String tag,
		     Dictionary namedArgs,
		     HttpRequest request)
	 throws ClassNotFoundException,NoSuchMethodException
    {
	this.tag=tag;
	this.namedArgs=namedArgs;
	this.request=request;
	
	parse();
	resolve();
    }

    /* ----------------------------------------------------------------- */
    Object invoke()
	 throws InvocationTargetException,IllegalAccessException
    {
	return tagMethod.invoke(null,tagArgs);
    }

    /* ----------------------------------------------------------------- */
    void parse()
    {
	try{
	    int i = tag.indexOf("(");
	    tagClassName = tag.substring(0,i).trim();
	    tagArgsString = tag.substring(i+1);
	    i=tagArgsString.lastIndexOf(")");
	    tagArgsString = tagArgsString.substring(0,i).trim();
	
	    i = tagClassName.lastIndexOf(".");
	    tagMethodName = tagClassName.substring(i+1).trim();
	    tagClassName  = tagClassName.substring(0,i).trim();

	    Code.debug("class = "+tagClassName);
	    Code.debug("method = "+tagMethodName);
	    Code.debug("args = "+tagArgsString);

	    StreamTokenizer tok =
		new StreamTokenizer(new StringReader(tagArgsString));
	    tok.quoteChar('"');
	    tok.whitespaceChars(',',',');

	    String nullArg = "null";
	    if (namedArgs!=null)
		namedArgs.put(nullArg,nullArg);
		
	    Vector tmpArgs = new Vector();
	    int ttype=0;
	    while ((ttype=tok.nextToken())!=StreamTokenizer.TT_EOF)
	    {
		switch(ttype)
		{
		  case StreamTokenizer.TT_NUMBER:
		      tmpArgs.addElement(new Double(tok.nval));
		      break;
		      
		  case StreamTokenizer.TT_WORD:
		      Object a = null;
		      if (namedArgs!=null)
			  a = namedArgs.get(tok.sval);
		      if (a==null && request!=null)
		      {
			  String[] aa = request.getParameterValues(tok.sval);
			  if (aa!=null && aa.length>=1)
			      // XXX - should handle long arrays
			      a=aa[0];
			  else
			      a="";
		      }
		      if (a==null && tok.sval.equals(nullArg))
		      {
			  a=nullArg;
		      }
		      
		      if (a==null)
			  a=tok.sval;
		      tmpArgs.addElement(a);
		      break;
		      
		  case '"':
		      tmpArgs.addElement(tok.sval);
		      break;

		  default:
		      Code.warning(tok.toString());
		}

		tagArgs = new Object[tmpArgs.size()];
		for (i=0;i<tagArgs.length;i++)
		{
		    Object a = tmpArgs.elementAt(i);
		    if (a==nullArg)
		    {
			tagArgs[i]=null;
		    }
		    else
			tagArgs[i]=a;
		}
	    }

	    if (tagArgs==null)
		tagArgs = new Object[0];
	}
	catch(IOException ioe){
	    Code.debug("Bad method format",ioe);
	    throw new RuntimeException("Bad method format");
	}	
    }
    
    /* ----------------------------------------------------------------- */
    void resolve()
	 throws ClassNotFoundException,NoSuchMethodException
    {
	tagClass = Class.forName(tagClassName);

	Method[] methods = tagClass.getMethods();
	for (int i=0; i<methods.length;i++)
	{
	    if (methods[i].getName().equals(tagMethodName))
	    {
		if (tagMethod==null)
		    tagMethod=methods[i];
		else
		{
		    // two methods, use params to resolve
		    
		    Class[] paramTypes = new Class[tagArgs.length];
		    for (int j=0;j<tagArgs.length;j++)
		    {
			Object a = tagArgs[j];
			if (a!=null)
			    paramTypes[j]=a.getClass();
			else
			    paramTypes[j]="null".getClass();
		    }

		    tagMethod = tagClass.getMethod(tagMethodName,
						   paramTypes);
		    break;
		}
	    }
	}

	Code.assert(Modifier.isStatic(tagMethod.getModifiers()),
		    "Can only call statics");
    }

    /* ----------------------------------------------------------------- */
    public static void test(String zero, Double one, String two, String three)
    {
	Code.debug("CALLED test("+zero+
		   ","+one+
		   ","+two+
		   ","+three+
		   ")");
    }

    /* ----------------------------------------------------------------- */
    public static void test()
    {
	Code.debug("CALLED test()");
    }

    /* ----------------------------------------------------------------- */
    public static void uniqTest(Hashtable zero,Double one,String two,String three)
    {
	Code.debug("CALLED uniqTest("+zero+
		   ","+one+
		   ","+two+
		   ","+three+
		   ")");
    }
    
    /* ----------------------------------------------------------------- */
    public static void main(String[] args)
    {
	try {    
	    Hashtable named = new Hashtable();
	    named.put("three","three");
	    new MethodTag
		("com.mortbay.HTTP.Filter.MethodTag.test(null ,  1,\"arg ,()\\\"2\\\"\",three);",
		 named,null).invoke();

	
	    new MethodTag("com.mortbay.HTTP.Filter.MethodTag.test();",
			  named,null).invoke();
	    
	    new MethodTag
		("com.mortbay.HTTP.Filter.MethodTag.uniqTest(null ,  1,\"arg ,()\\\"2\\\"\",three);",
		 named,null).invoke();
	}
	catch(Exception e){
	    Code.warning("Failure",e);
	}	
    }
}

