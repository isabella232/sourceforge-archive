// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================
package com.mortbay.Util;

import com.mortbay.Base.*;
import java.io.*;
import java.util.*;

/* ------------------------------------------------------------ */
/** URI wrapper
 * Wrapper for the results of
 * javax.servlet.http.HttpServletRequest.getRequestURI()
 * <p><h4>Notes</h4>
 *
 * @see javax.servlet.http.HTTPServletRequest.getRequestURI
 * @version 1.0 Sun Dec 14 1997
 * @author Greg Wilkins (gregw)
 */
public class URI
{
    /* ------------------------------------------------------------ */
    private String path;
    private String query;
    private UrlEncoded parameters = new UrlEncoded();
    private boolean modified=false;
    private boolean encodeNulls=false;
    
    /* ------------------------------------------------------------ */
    /** Construct from a String can contain both a path and
     * encoded query parameters.
     * @param uri The uri path and optional encoded query parameters.
     */
    public URI(String uri)
    {
	path = uri;
	int q;
	if ((q=uri.indexOf('?'))>=0)
	{
	    if ((q+1)<uri.length())
	    {
		try{
		    query=uri.substring(q+1);
		    parameters.read(uri.substring(q+1));
		    path=uri.substring(0,q);
		}
		catch(IOException e){
		    Code.ignore(e);
		}
	    }
	    else
		path=uri.substring(0,q);
	}
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri path
     * @return the URI path
     */
    public String getPath()
    {
	return path;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri path
     * @return the URI path
     */
    public void setPath(String path)
    {
	this.path=path;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri path
     * @deprecated Use getPath
     * @return the URI path
     */
    public String path()
    {
	return path;
    }

    /* ------------------------------------------------------------ */
    /** Get the uri path
     * @deprecated Use setPath
     * @return the URI path
     */
    public void path(String path)
    {
	this.path=path;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Get the uri query String
     * @return the URI query string
     */
    public String getQuery()
    {
	if (modified)
	    query = parameters.encode(encodeNulls);
	return query;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri query String
     * @deprecated Use getQuery
     * @return the URI query string
     */
    public String query()
    {
	if (modified)
	    query = parameters.encode(encodeNulls);
	return query;
    }
    
    /* ------------------------------------------------------------ */
    /** Set if this URI should encode nulls as an empty = clause
     * @param b If true then encode nulls
     */
    public void encodeNulls(boolean b)
    {
	this.encodeNulls=b;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Get the uri query parameters
     * @return the URI query parameters
     * @deprecated use getParameters
     */
    public Dictionary queryContent()
    {
	return parameters;
    }

    /* ------------------------------------------------------------ */
    /** Get the uri query parameters
     * @return the URI query parameters
     * @deprecated use getParameters
     */
    public Dictionary parameters()
    {
	modified=true;
	return parameters;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri query parameters names
     * @return the URI query parameters names
     */
    public Enumeration getParameterNames()
    {
	return parameters.keys();
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri query parameters
     * @return the URI query parameters
     */
    public Dictionary getParameters()
    {
	modified=true;
	return parameters;
    }
    
    /* ------------------------------------------------------------ */
    /** Clear the URI parameters
     */
    public void clearParameters()
    {
	modified=true;
	parameters.clear();
    }
    
    /* ------------------------------------------------------------ */
    /** Add encoded parameters
     * @param encoded A HTTP encoded string of parameters: e.g.. "a=1&b=2"
     */
    public void put(String encoded)
    {
	try{
	    UrlEncoded params = new UrlEncoded(encoded);
	    put(params);
	}
	catch(IOException e){
	    Code.ignore(e);
	}
    }
    
    /* ------------------------------------------------------------ */
    /** Add name value pair to the uri query parameters
     * @param name name of value
     * @param value value
     */
    public void put(String name, String value)
    {
	modified=true;
	if (name!=null && value!=null)
	    parameters.put(name,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Add named multi values to the uri query parameters
     * @param name name of value
     * @param value value
     */
    public void put(String name, String[] values)
    {
	modified=true;
	if (name!=null && values!=null)
	    parameters.putValues(name,values);
    }
    
    /* ------------------------------------------------------------ */
    /** Add dictionary to the uri query parameters
     */
    public void put(Dictionary values)
    {
	modified=true;
	Enumeration keys= values.keys();
	while(keys.hasMoreElements())
	{
	    Object key = keys.nextElement();
	    parameters.put(key,values.get(key));
	}
    }

    /* ------------------------------------------------------------ */
    /** Get named value 
     */
    public String get(String name)
    {
	return (String)parameters.get(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Get named multiple values
     */
    public String[] getValues(String name)
    {
	return parameters.getValues(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Remove named value 
     */
    public void remove(String name)
    {
	modified=true;
	parameters.remove(name);
    }
    
    /* ------------------------------------------------------------ */
    /** @return the URI string encoded.
     */
    public String toString()
    {
	String result = path;
	if (modified)
	    query();
	if (query!=null && query.length()>0)
	    result+="?"+query;
	return result;
    }

    /* ------------------------------------------------------------ */
    public static void test()
    {
	Test test = new Test("com.mortbay.Util.URI");
	try
	{
	    URI uri;

	    uri = new URI("/Test/URI");
	    test.checkEquals(uri.toString(),"/Test/URI","no params");
    
	    uri = new URI("/Test/URI?a=1");
	    test.checkEquals(uri.toString(),"/Test/URI?a=1","one param");
	
	    uri = new URI("/Test/URI");
	    uri.put("b","2 !");
	    test.checkEquals(uri.toString(),"/Test/URI?b=2+%21","add param");
	
	    uri = new URI("/Test/URI?c=1&d=2");
	    uri.put("e","3");
	    String s = uri.toString();
	    test.check(s.startsWith("/Test/URI?"),"merge params path");
	    test.check(s.indexOf("c=1")>0,"merge params c1");
	    test.check(s.indexOf("d=2")>0,"merge params d2");
	    test.check(s.indexOf("e=3")>0,"merge params e3");

	    uri = new URI("/Test/URI?a=");
	    test.checkEquals(uri.toString(),"/Test/URI?a=","null param");
	    uri.parameters();
	    test.checkEquals(uri.toString(),"/Test/URI?a","null param");
	    uri.encodeNulls(true);
	    test.checkEquals(uri.toString(),"/Test/URI?a=","null= param");
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








