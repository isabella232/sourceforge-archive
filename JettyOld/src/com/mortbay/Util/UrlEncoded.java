// =========================================================================== 
// $Id$
package com.mortbay.Util;

import com.mortbay.Base.*;

import java.io.*;
import java.util.*;
import java.net.*;

/* ------------------------------------------------------------ */
/** Handles coding of MIME  "x-www-form-urlencoded"
 *
 * <p><h4>Notes</h4>
 * The hashtable either contains String single values, vectors
 * of String or arrays of Strings.
 *
 * @see java.net.URLEncoder
 * @version 1.0 Fri Dec 12 1997
 * @author Greg Wilkins (gregw)
 */
public class UrlEncoded extends Hashtable
{
    /* -------------------------------------------------------------- */
    public static final String noValue="";
    
    /* ----------------------------------------------------------------- */
    public UrlEncoded()
    {}
    
    /* ----------------------------------------------------------------- */
    public UrlEncoded(String s)
	throws IOException
    {
	read(s);
    }
    
    /* ----------------------------------------------------------------- */
    public void read(String string_input)
	 throws IOException
    {
	addParamsTo(string_input,this);
    }

    
    /* ------------------------------------------------------------ */
    /** Get value
     * Converts multiple values into coma separated list
     * @param key The parameter name
     * @return value
     */
    public Object get(Object key)
    {
	Object o = super.get(key);
	if (o!=null)
	{
	    if (o instanceof Vector)
	    {
		Vector v=(Vector)o;
		if (v.size()>0)
		{
		    String value=v.elementAt(0).toString();
		    for (int i=1; i<v.size(); i++)		
			value += ','+v.elementAt(i).toString();
		    return value;
		}
		return null;
	    }
	    
	    if (o instanceof String[])
	    {
		String[] a=(String[])o;
		if (a.length>0)
		{
		    StringBuffer buf = new StringBuffer(a[0]);
		    for (int i=1; i<a.length; i++)
		    {
			buf.append(',');
			buf.append(a[i]);
		    }
		    return buf.toString();
		}
		return null;
	    }
	}
	return o;
    }

    /* ------------------------------------------------------------ */
    /** Get the value as an object 
     * @param key The parameter name
     * @return Either a String value or Vector of String values
     */
    public Object getObject(Object key)
    {
	return super.get(key);
    }
    
    /* ------------------------------------------------------------ */
    /** Get multiple values as an array.
     * Multiple values must be specified as "N=A&N=B"
     * @param key The parameter name
     * @return array of values or null
     */
    public String[] getValues(String key)
    {
	Object o = super.get(key);
	if (o==null)
	    return null;
	if (o instanceof String[])
	    return (String[])o;
	if (o instanceof Vector)
	{
	    Vector v = (Vector)o;
	    String[] a = new String[v.size()];
	    for (int i=v.size();i-->0;)
		a[i]=v.elementAt(i).toString();
	    return a;
	}

	String[] a = new String[1];
	a[0]=o.toString();
	return a;
    }
    
    
    
    /* ------------------------------------------------------------ */
    /** Set a multi valued paramter 
     * @param key The parameter name
     * @param values Array of string values
     */
    public void putValues(String key, String[] values)
    {
	super.put(key,values);
    }
    
    
    /* -------------------------------------------------------------- */
    /* Add encoded parameters to Dictionary.
     * @param content the string containing the encoded parameters
     * @param url The dictionary top add the parametes to
     */
    public static void addParamsTo(String content,UrlEncoded url)
    {
        String name;
        String value;

        StringTokenizer tokenizer = new StringTokenizer(content, "&", false);

	while ((tokenizer.hasMoreTokens()))
	{
	    // take the first token string, which should be an assignment statement
	    String substring = tokenizer.nextToken();
	    
	    // breaking it at the "=" sign
	    int i = substring.indexOf('=');
	    if (i<0)
	    {
		name=decode(substring);
		value=noValue;
	    }
	    else
	    {
		name  = decode(substring.substring(0,i++).trim());
		if (i>=substring.length())
		    value=noValue;
		else
		{
		    value =
			substring.substring(i,substring.length()).trim();
		    value = decode(value);
		    value = StringUtil.replace(value,"\015\n","\n");
		}
	    }
	    
	    if (name.length() > 0)
	    {
		Object o = url.getObject(name);
		if (o!=null)
		{
		    if (o instanceof Vector)
			((Vector)o).addElement(value);
		    else
		    {
			Vector v = new Vector();
			v.addElement(o);
			v.addElement(value);
			url.put(name,v);
		    }
		}
		else
		   url.put(name,value);
	    }
	}
    }
    
    /* -------------------------------------------------------------- */
    /** Decode String with % encoding
     */
    public static String decode(String encoded)
    {
	encoded = encoded.replace('+',' ');
	int index = 0;
	int marker = 0;   

	// there is at least one encoding
	StringBuffer result=null;
	while (((marker = encoded.indexOf('%', index)) != -1)
	       &&(index < encoded.length()))
	{
	    if(result==null)
		result=new StringBuffer();
	    
	    result.append(encoded.substring (index, marker));
	    
	    // convert the 2 hex chars following the % into a byte,
	    // which will be a character
	    result.append((char)(Integer.parseInt
				 (encoded.substring(marker+1,marker+3),16)));
	    index = marker+3;  
	}

	// if no encoded characters return the original
	if (result==null)
	    return encoded;

	// if there is some at the end then copy it in
	if (index < encoded.length())
            result.append(encoded.substring(index, encoded.length()));
   
        return result.toString();
    }
    
    /* -------------------------------------------------------------- */
    /** Encode Hashtable with % encoding
     */
    public String encode()
    {
	return encode(false);
    }
    
    /* -------------------------------------------------------------- */
    /** Encode Hashtable with % encoding
     * @param equalsForNullValue if True, then an '=' is always used, even
     * for paramaters without a value. eg. "blah?a=&b=&c=".
     */
    public String encode(boolean equalsForNullValue)
    {
	Enumeration keys=keys();
	String separator="";
	StringBuffer result = new StringBuffer();
	while(keys.hasMoreElements())
	{
	    String key = keys.nextElement().toString();
	    String[] values = getValues(key);

	    for (int v=0; v<values.length;v++)
	    {
		result.append(separator);
		result.append(URLEncoder.encode(key));
	    
		if (values[v].length()>0)
		{
		    result.append('=');
		    result.append(URLEncoder.encode(values[v]));
		}
		else if (equalsForNullValue)
		    result.append('=');
	    
		separator="&";
	    }
	}
	return result.toString();
    }
    
    /* -------------------------------------------------------------- */
    public static void test()
    {
	Test test = new Test("com.mortbay.Util.UrlEncoded");

	try{
		
	    UrlEncoded code = new UrlEncoded();
	    test.checkEquals(code.size(),0,"Empty");

	    code.clear();
	    code.read("Name1=Value1");
	    test.checkEquals(code.size(),1,"simple param size");
	    test.checkEquals(code.encode(),"Name1=Value1","simple encode");
	    test.checkEquals(code.get("Name1"),"Value1","simple get");

	    code.clear();
	    code.read("Name2=");
	    test.checkEquals(code.size(),1,"dangling param size");
	    test.checkEquals(code.encode(),"Name2","dangling encode");
	    test.checkEquals(code.get("Name2"),noValue,"dangling get");
	
	    code.clear();
	    code.read("Name3");
	    test.checkEquals(code.size(),1,"noValue param size");
	    test.checkEquals(code.encode(),"Name3","noValue encode");
	    test.checkEquals(code.get("Name3"),noValue,"noValue get");
	
	    code.clear();
	    code.read("Name4=Value+4%21");
	    test.checkEquals(code.size(),1,"encoded param size");
	    test.checkEquals(code.encode(),"Name4=Value+4%21","encoded encode");
	    test.checkEquals(code.get("Name4"),"Value 4!","encoded get");

	    code.clear();
	    code.read("Name5=aaa&Name6=bbb");
	    test.checkEquals(code.size(),2,"multi param size");
	    test.check(code.encode().equals("Name5=aaa&Name6=bbb") ||
		       code.encode().equals("Name6=bbb&Name5=aaa"),
		       "multi encode");
	    test.checkEquals(code.get("Name5"),"aaa","multi get");
	    test.checkEquals(code.get("Name6"),"bbb","multi get");
	
	    code.clear();
	    code.read("Name7=aaa&Name7=b%2Cb&Name7=ccc");
	    test.checkEquals(code.encode(),
			     "Name7=aaa&Name7=b%2Cb&Name7=ccc",
			     "multi encode");
	    test.checkEquals(code.get("Name7"),"aaa,b,b,ccc","list get all");
	    test.checkEquals(code.getValues("Name7")[0],"aaa","list get");
	    test.checkEquals(code.getValues("Name7")[1],"b,b","list get");
	    test.checkEquals(code.getValues("Name7")[2],"ccc","list get");
	}
	catch(Exception e){
	    test.check(false,e.toString());
	}
    }
    
    /* -------------------------------------------------------------- */
    public static void main(String[] args)
    {
	    
	test();
	Test.report(); 
    }
}
