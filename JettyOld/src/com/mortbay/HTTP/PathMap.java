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
 * Paths ending with '$' must match the path absolutely,<BR>
 * Paths ending with '/' must match an exact path element,<BR>
 * Paths ending with '%' match either an absolute path or exact path element<BR>
 * Paths ending with '|' match either an absolute path or trailing '/'<BR>
 * Paths may include a * to separate prefix and suffix matches. Suffix
 * matches have precedence.<P>
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
 * <TR><TD>
 * *.xxx
 * </TD><TD>
 * .xxx<BR>
 * xxx.xxx<BR>
 * </TD><TD>
 * <BR>
 * .xxxx<BR>
 * xxxx.xxxx<BR>
 * .xx<BR>
 * xx.xx
 * </TD></TR>
 * <TR><TD>
 * /yyy/*.zzz
 * </TD><TD>
 * /yyy/.zzz<BR>
 * /yyy/xxx.zzz<BR>
 * </TD><TD>
 * <BR>
 * .zzz<BR>
 * /aaa/xxx.zzz<BR>
 * /yyy/xxx.zzzz
 * </TD></TR>
 * <P>
 * Path suffixes are also supported. For example for /aaa/bbb/ccc.c the
 * match priorities are:<UL>
 * <LI>An exact match /aaa/bbb/ccc.c
 * <LI>A path qualified wildcard /aaa/bbb/*.c
 * <LI>A null path wildcard *.c
 * <LI>A non wild pathspec e.g. /aaa/bbb%                                         
 * </TABLE>
 * 
 * @version $Id$
 * @author Greg Wilkins
*/
public class PathMap extends Dictionary
{
    /* --------------------------------------------------------------- */
    Hashtable pathMap=new Hashtable(27);
    Hashtable absoluteMap=new Hashtable(27);
    WildMap wildMap=new WildMap();
    
    /* --------------------------------------------------------------- */
    /** Construct empty PathMap
     */
    public PathMap()
    {}
    
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
     * @param object The object the path maps to
     */
    public Object put(String pathSpec, Object object)
    {
        Object o=pathMap.put(pathSpec,object);
        PathValue pv = new PathValue(pathSpec,object);
        if (pathSpec.length()==0)
            absoluteMap.put(pathSpec,pv);
        else
        {
            int last=pathSpec.length()-1;
            String path=pathSpec.substring(0,last);
            char c = pathSpec.charAt(last);
            switch(c)
            {
              case '$':
                  absoluteMap.put(path,pv);
                  break;
                  
              case '%':
                  absoluteMap.put(path,pv);
                  wildMap.put(path+"/*",pv);
                  break;
                  
              case '|':
                  absoluteMap.put(path,pv);
                  absoluteMap.put(path+"/",pv);
                  break;
                  
              case '*':
                  absoluteMap.put(path,pv);
                  wildMap.put(pathSpec,pv);
                  break;
                  
              default:
                  int s = pathSpec.indexOf("*");
                  if (s==0)
                  {
                      absoluteMap.put(pathSpec.substring(1),pv);
                      wildMap.put(pathSpec,pv);
                  }
                  else if (s>=0)
                  {
                      absoluteMap.put(pathSpec.substring(0,s)+
                                      pathSpec.substring(s+1),pv);
                      wildMap.put(pathSpec,pv);
                  }
                  else
                  {
                      absoluteMap.put(pathSpec,pv);
                      wildMap.put(pathSpec+"*",pv);
                  }
                  break;
            }
        }
        
        return o;
    }
    
    /* --------------------------------------------------------------- */
    /** Add a single path match to the PathMap
     * @param pathSpec The path specification.
     * @param object The object the path maps to
     */
    public Object put(Object pathSpec, Object object) 
    {
        return put((String)pathSpec,object);
    }
    
    /* --------------------------------------------------------------- */
    /** Get the object that is mapped by the longest path specification
     * that matches the path
     */
    public Object match(String path)
    {
        PathValue pv = (PathValue) absoluteMap.get(path);
        if (pv!=null)
            return pv.value;
        pv = (PathValue) wildMap.get(path);
        if (pv!=null)
            return pv.value;
        return null;
    }
    
    /* --------------------------------------------------------------- */
    /** Get the pathSpec that is mapped by the longest path specification
     * that matches the path
     */
    public String matchSpec(String path)
    {
        PathValue pv = (PathValue) absoluteMap.get(path);
        if (pv!=null)
            return pv.pathSpec;
        pv = (PathValue) wildMap.get(path);
        if (pv!=null)
            return pv.pathSpec;
        return null;
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
    /** Get the longest matching path specification
     * @deprecated use matchSpec
     */
    public String longestMatch(String path)
    {
        return matchSpec(path);
    }
    
    /* --------------------------------------------------------------- */
    /** Get the object that is mapped by the longest path specification
     * that matches the path
     * @deprecated use match
     */
    public Object getLongestMatch(String path)
    {
        return match(path);
    }
    
    /* --------------------------------------------------------------- */
    public boolean isEmpty()
    {
        return pathMap.isEmpty();
    }
    
    /* --------------------------------------------------------------- */
    public Enumeration keys()
    {
        return pathMap.keys();
    }
    
    /* --------------------------------------------------------------- */  
    public Object remove(Object pathSpec)
    {
        return remove((String)pathSpec);
    }
    
    /* --------------------------------------------------------------- */  
    public Object remove(String pathSpec)
    {
        Object o=pathMap.remove(pathSpec);
        
        if (pathSpec.length()==0)
            absoluteMap.remove(pathSpec);
        else
        {
            int last=pathSpec.length()-1;
            String path=pathSpec.substring(0,last);
            char c = pathSpec.charAt(last);
            switch(c)
            {
              case '$':
                  absoluteMap.remove(path);
                  break;
                  
              case '%':
                  absoluteMap.remove(path);
                  wildMap.remove(path+"/*");
                  break;
                  
              case '|':
                  absoluteMap.remove(path);
                  absoluteMap.remove(path+"/");
                  break;
                  
              case '*':
                  absoluteMap.remove(path);
                  wildMap.remove(pathSpec);
                  break;
                  
              default:
                  int s = pathSpec.indexOf("*");
                  if (s==0)
                  {
                      absoluteMap.remove(pathSpec.substring(1));
                      wildMap.remove(pathSpec);
                  }
                  else if (s>=0)
                  {
                      absoluteMap.remove(pathSpec.substring(0,s)+
                                      pathSpec.substring(s+1));
                      wildMap.remove(pathSpec);
                  }
                  else
                  {
                      absoluteMap.remove(pathSpec);
                      wildMap.remove(pathSpec+"*");
                  }
                  break;
            }
        }
        
        return o;       
    }
    
    /* --------------------------------------------------------------- */
    public int size()
    {
        return pathMap.size();
    }

    /* --------------------------------------------------------------- */
    public void clear()
    {
        pathMap.clear();
        absoluteMap.clear();
        wildMap.holders.removeAllElements();
        wildMap.cache=null;
    }
    
    /* --------------------------------------------------------------- */
    /** Return the portion of a path that is after a path spec (with %$|/ etc.)
     * @return The path info string
     */
    public static String pathInfo(String pathSpec, String path)
        throws MalformedURLException
    {
        int s = pathSpec.indexOf("*");
        if (s>=0)
            pathSpec=pathSpec.substring(0,s);
        if (pathSpec.length()==0)
            return path;
        
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
        int s = pathSpec.indexOf("*");
        if (s>=0)
            pathSpec=pathSpec.substring(0,s);
        if (pathSpec.length()==0)
            return pathSpec;
        
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
        StringBuffer buf = new StringBuffer(512);
        buf.append("{\n");
        Enumeration e = pathMap.keys();
        while (e.hasMoreElements())
        {
            String pathSpec= (String)e.nextElement();
            buf.append("    ");
            buf.append(pathSpec);
            buf.append(" = ");
            buf.append(pathMap.get(pathSpec));
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
        int s = pathSpec.indexOf("*");
        if (s>=0)
            pathSpec=pathSpec.substring(0,s);
        if (pathSpec.length()==0)
            return newPath+oldPath;
        
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


    /* =============================================================== */
    private static class PathValue
    {
        String pathSpec;
        Object value;
        PathValue(String pathSpec,Object value)
        {
            this.pathSpec=pathSpec;
            this.value=value;
        }
    }
    
    /* =============================================================== */
    static class Holder
    {
        Object value;
        String prefix="";
        int pl=0;
        String suffix="";
        int sl=0;
        int tl=0;

        Holder(String wild, Object value)
        {
            this.value=value;
            int star;
            if ((star=wild.indexOf('*'))>=0)
            {
                prefix=wild.substring(0,star);
                suffix=wild.substring(star+1);
                pl=prefix.length();
                sl=suffix.length();
                tl=pl+sl;
            }
            else
            {
                prefix=wild;
                pl=wild.length();
                tl=pl;
            }
        }

        public boolean equals(Object o)
        {
            if (o instanceof Holder)
            {
                Holder h=(Holder)o;
                return h==this ||
                    (prefix.equals(h.prefix) &&
                     suffix.equals(h.suffix));
            }
            return false;
        }

        public int hashCode()
        {
            return prefix.hashCode()+suffix.hashCode();
        }

        public String toString()
        {
            return prefix+"*"+suffix;
        }
    }

    /* =============================================================== */
    static class WildMap
    {
        Vector holders = new Vector();
        Holder[] cache = null;

        public Holder put(String wildPath, Object v)
        {
            Holder holder=new Holder(wildPath,v);
            cache=null;
            
            for (int i=holders.size();i-->0;)
            {
                Holder h=(Holder)holders.elementAt(i);
                if (h.equals(holder))
                {
                    holders.setElementAt(holder,i);
                    return h;
                }
                else
                {
                    if (h.sl>holder.sl)
                        continue;
                    if (h.sl==holder.sl && h.pl >holder.pl)
                        continue;
                    holders.insertElementAt(holder,i+1);
                    return null;
                }
            }
            holders.insertElementAt(holder,0);
            return null;
        }
        
        public Object get(String path)
        {
            if (cache==null)
            {
                cache=new Holder[holders.size()];
                holders.copyInto(cache);
            }
            
            for (int i=cache.length;i-->0;)
            {
                if (path.endsWith(cache[i].suffix) &&
                    path.startsWith(cache[i].prefix) &&
                    path.length()>=(cache[i].suffix.length()+
                                    cache[i].prefix.length()))
                    return cache[i].value;
            }
            return null;
        }
        
        public void remove(String path)
        {
            cache=null;
            Holder holder=new Holder(path,null);
            holders.removeElement(holder);
        }
    }
}






