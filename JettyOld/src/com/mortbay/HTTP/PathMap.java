// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;

import com.mortbay.Base.*;
import java.util.*;
import java.net.MalformedURLException;


/* ------------------------------------------------------------------- */
/** URI Path map to object
 * <p> Implements a best match of paths to objects
 *
 * <p>Notes<br>
 * Paths ending with '$' must match the path absolutely,
 * Paths ending with '/' must match an exact path element,
 * Paths ending with '%' match either an absoulte path or exect path element
 * Paths ending with '|' match either an absoulte path or trailing '/'
 * All other paths only need to be a prefix to match.
 * <P>
 * <TABLE BORDER=1 CELLPADDING=5>
 * <TR><TH>Path Prefix</TH><TH>Match Examples</TH><TH>Miss Examples</TH>
 * <TR><TD>
 * /aaa/bbb
 * </TD><TD>
 * /aaa/bbb<BR>
 * /aaa/bbb/<BR>
 * /aaa/bbb/ccc<BR>
 * /aaa/bbbbbb
 * </TD><TD>
 * /aaa<BR>
 * /aaa/ccc
 * </TD></TR>
 * 
 * <TR><TD>
 * /aaa/bbb%
 * </TD><TD>
 * /aaa/bbb<BR>
 * /aaa/bbb/<BR>
 * /aaa/bbb/ccc
 * </TD><TD>
 * /aaa<BR>
 * /aaa/ccc<BR>
 * /aaa/bbbbbb
 * </TD></TR>
 * 
 * <TR><TD>
 * /aaa/bbb/
 * </TD><TD>
 * /aaa/bbb/<BR>
 * /aaa/bbb/ccc<BR>
 * </TD><TD>
 * /aaa<BR>
 * /aaa/bbb<BR>
 * /aaa/bbbbbb<BR>
 * /aaa/ccc
 * </TD></TR>

 * <TR><TD>
 * /aaa/bbb|
 * </TD><TD>
 * /aaa/bbb<BR>
 * /aaa/bbb/<BR>
 * </TD><TD>
 * /aaa<BR>
 * /aaa/ccc<BR>
 * /aaa/bbb/ccc<BR>
 * /aaa/bbbbbb
 * </TD></TR>
 * 
 * <TR><TD>
 * /aaa/bbb$
 * </TD><TD>
 * /aaa/bbb<BR>
 * </TD><TD>
 * /aaa<BR>
 * /aaa/ccc<BR>
 * /aaa/bbb/<BR>
 * /aaa/bbb/ccc<BR>
 * /aaa/bbbbbb
 * </TD></TR>
 * 
 * </TABLE>
 * 
 * @version $Id$
 * @author Greg Wilkins
*/
public class PathMap extends Dictionary
{
    /* --------------------------------------------------------------- */
    Vector paths=new Vector();
    Hashtable pathMap=new Hashtable();
    
    /* --------------------------------------------------------------- */
    /** Construct empty PathMap
     */
    public PathMap(){
    }
    
    /* --------------------------------------------------------------- */
    /** Construct from dictionary PathMap
     */
    public PathMap(Dictionary d)
    {
	add(d);
    }
    
    /* --------------------------------------------------------------- */
    /** Add contents of dictionary to PathMap
     */
    public void add(Dictionary d)
    {
	Enumeration e = d.keys();
	while (e.hasMoreElements())
	{
	    String k = (String)e.nextElement();
	    put(k,d.get(k));
	}
    }
    
    /* --------------------------------------------------------------- */
    /** Add a single path match to the PathMap
     * @param pathSpec The path specification.
     * @param object Th eobject the path maps to
     */
    public Object put(String pathSpec, Object object)
    {
	Object o = pathMap.put(pathSpec,object);
	if (o==null)
	{
	    int pathLen=pathSpec.length();
	    for (int i=paths.size();i>=0;i--)
	    {
		if (i==0)
		    paths.insertElementAt(pathSpec,0);
		else if (paths.elementAt(i-1).toString().length()<=pathLen)
		{
		    paths.insertElementAt(pathSpec,i);
		    break;
		}
	    }
	}
	
	return o;
    }
    
    /* --------------------------------------------------------------- */
    public Enumeration elements()
    {
	return pathMap.elements();
    }
    
    /* --------------------------------------------------------------- */
    /** Get object by path specification
     */
    public Object get(Object pathSpec)
    {
	return pathMap.get(pathSpec);
    }
    
    /* --------------------------------------------------------------- */
    /** Get the object that is mapped by the longest path specification
     * that matches the path
     */
    public Object getLongestMatch(String path)
    {
	Object lkey = longestMatch(path);
	if (lkey==null)
	    return null;
	return pathMap.get(longestMatch(path));
    }
    
    /* --------------------------------------------------------------- */
    public boolean isEmpty()
    {
	return pathMap.isEmpty();
    }
    
    /* --------------------------------------------------------------- */
    public Enumeration keys()
    {
	return paths.elements();
    }
    
    /* --------------------------------------------------------------- */
    /** Add a single path match to the PathMap
     * @param pathSpec The path specification (must be a String)
     * @param object Th eobject the path maps to
     */
    public Object put(Object  pathSpec, Object  object) 
    {
	return put((String)pathSpec,object);
    }
    
    /* --------------------------------------------------------------- */  
    public Object remove(Object  key)
    {
	paths.removeElement(key);
	return pathMap.remove(key);
    }
    
    /* --------------------------------------------------------------- */
    public int size()
    {
	return pathMap.size();
    }

    /* --------------------------------------------------------------- */
    public void clear()
    {
	paths.removeAllElements();
	pathMap.clear();
    }
    

    /* --------------------------------------------------------------- */
    /** Get the longest matching path specification
     */
    public String longestMatch(String path)
    {
	int tl=path.length();
	
	for (int i = paths.size();i-->0;)
	{
	    String tryPath = (String) paths.elementAt(i);
	    int tpl = tryPath.length();
	    if (tpl==0)
		return tryPath;
	    
	    switch (tryPath.charAt(tpl-1))
	    {
	      case '|':
		  if (tl==tpl && path.charAt(tpl-1)=='/' &&
		      path.startsWith(tryPath.substring(0,tpl-1)))
		      return tryPath;
		  if (tl==tpl-1 &&
		      path.equals(tryPath.substring(0,tpl-1)))
		      return tryPath;
		  break;
		  
	      case '%':
		  if (tl>=tpl && path.charAt(tpl-1)=='/' &&
		      path.startsWith(tryPath.substring(0,tpl-1)))
		      return tryPath;
		  
	      case '$':
		if(tryPath.length()==(tl+1) &&
		   tryPath.startsWith(path))
		    return tryPath;
		  break;
	      default:
		  if (tpl<=tl && path.startsWith(tryPath))
		  {
		      //Code.debug("Matched "+tryPath);
		      return tryPath;
		  }
		  break;
	    }
	       
	}
	return null;
    }
    
    /* --------------------------------------------------------------- */
    /** Return the portion of a path that is after a path spec (with %$|/ etc.)
     * @return The path info string
     */
    public static String pathInfo(String pathSpec, String path)
	throws MalformedURLException
    {
	switch (pathSpec.charAt(pathSpec.length()-1))
	{
	  case '|':
	  case '%':
	  case '$':
	      pathSpec=pathSpec.substring(0,pathSpec.length()-1);
	      break;
	  default:
	      break;
	}
	
	if (!path.startsWith(pathSpec))
	    throw new MalformedURLException("Bad PathSpec '"+
					    pathSpec+"' for "+path);

	return path.substring(pathSpec.length());
    }
    
    /* --------------------------------------------------------------- */
    /** Return the portion of a path that matches a path spec (with %$|/ etc.)
     * @return null if no match at all.
     */
    public static String match(String pathSpec, String path)
    {
	int psl = pathSpec.length();
	int pl = path.length();

	if (psl==0)
	    return pathSpec;
	
	switch (pathSpec.charAt(psl-1))
	{
	  case '|':
	      if (psl==pl &&
		  path.charAt(psl-1)=='/' && psl>2 &&
		  path.startsWith(pathSpec.substring(0,psl-1)))
		  return path;
	      if (psl==pl+1 &&
		  pathSpec.startsWith(path))
		  return path;
	      return null;
	      
	  case '%':
	      if (psl<=pl &&
		  path.charAt(psl-1)=='/' && psl>2 &&
		  path.startsWith(pathSpec.substring(0,psl-1)))
		  return path.substring(0,psl);
	      if (psl==pl+1 &&
		  pathSpec.startsWith(path))
		  return path;
	      return null;
	  case '$':
	      if (psl==pl+1 &&
		  pathSpec.startsWith(path))
		  return path;
	      return null;
	      
	  default:
	      if (psl<=pl &&
		  path.startsWith(pathSpec))
		  return pathSpec;
	      break;
	}
	return null;
    }


    /* --------------------------------------------------------------- */
    public String toString()
    {
	StringBuffer buf = new StringBuffer();
	buf.append("{\n");
	Enumeration e = paths.elements();
	while (e.hasMoreElements())
	{
	    String pathSpec= (String)e.nextElement();
	    buf.append("    ");
	    buf.append(pathSpec);
	    buf.append(" = ");
	    buf.append(pathMap.get(pathSpec).toString());
	    buf.append("\n");
	}
	buf.append("}");

	return buf.toString();
    }


    /* ------------------------------------------------------------ */
    /** Return a translated address.
     * The part of the address that matches the path is replaced
     * with the translation string.
     * @param oldPath A URL path to be translated
     * @param pathSpec The PathMap path string that matched (may include
     *             special %|$ etc characters).
     * @param newPath The string to replace the path with.
     * @return translated address.
     */
    public static String translate(String oldPath,
				   String pathSpec,
				   String newPath)
    {
	String result = null;
	
	String match=match(pathSpec,oldPath);
	Code.assert(match!=null,"translate non matching address");
	
	if (pathSpec.endsWith("%") && match.endsWith("/") &&
	    ! newPath.endsWith("/"))
	    newPath += "/";
	
	if (match.length()==oldPath.length())    
	    result= newPath;

	result = newPath+oldPath.substring(match.length());
	
	Code.debug("Translated '",match,
		   "' part of '",oldPath,
		   "' to '",newPath,
		   "' resulted with ",result);
	return result;
    }
    
}


