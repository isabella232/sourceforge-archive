// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================
package org.mortbay.util;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* ------------------------------------------------------------ */
/** URI Holder.
 * This class assists with the decoding and encoding or HTTP URI's.
 * It differs from the java.net.URL class as it does not provide
 * communications ability, but it does assist with query string
 * formatting.
 * @see UrlEncoded
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class URI
    implements Cloneable
{
    /* ------------------------------------------------------------ */
    private String _uri;
    private String _scheme;
    private String _host;
    private int _port;
    private String _path;
    private String _query;
    private UrlEncoded _parameters = new UrlEncoded();
    private boolean _dirty;
    
    /* ------------------------------------------------------------ */
    /** Copy Constructor .
     * @param uri
     */
    public URI(URI uri)
        throws IllegalArgumentException
    {
        _uri=uri.toString();
        _scheme=uri._scheme;
        _host=uri._host;
        _port=uri._port;
        _path=uri._path;
        _query=uri._query;
        _parameters=(UrlEncoded)uri._parameters.clone();
        _dirty=false;
    }
    
    /* ------------------------------------------------------------ */
    /** Construct from a String.
     * The string must contain a URI path, but optionaly may contain a
     * scheme, host, port and query string.
     * 
     * @param uri [scheme://host[:port]]/path[?query]
     */
    public URI(String uri)
        throws IllegalArgumentException
    {
        try
        {    
            _uri=uri;
            
            // Scan _uri for host, port, path & query
            int maxi=uri.length()-1;
            int mark=0;
            int state=0;
            for (int i=0;i<=maxi;i++)
            {
                char c=uri.charAt(i);
                switch(state)
                {
                  case 0: // looking for scheme or path
                      if (c==':' &&
                          uri.charAt(i+1)=='/' &&
                          uri.charAt(i+2)=='/')
                      {
                          // found end of scheme & start of host
                          _scheme=uri.substring(mark,i);
                          i+=2;
                          mark=i+1;
                          state=1;
                      }
                      else if (i==0 && c=='/')
                      {
                          // Found path
                          state=3;
                      }
                      else if (i==0 && c=='*')
                      {
                          state=5;
                          _path="*";
                          break;
                      }
                      continue;

                  case 1: // Get host & look for port or path
                      if (c==':')
                      {
                          // found port
                          _host=uri.substring(mark,i);
                          mark=i+1;
                          state=2;
                      }
                      else if (c=='/')
                      {
                          // found path
                          _host=uri.substring(mark,i);
                          mark=i;
                          state=3;
                      }
                      continue;

                  case 2: // Get port & look for path
                      if (c=='/')
                      {
                          _port=Integer.parseInt(uri.substring(mark,i));
                          mark=i;
                          state=3;
                      }
                      continue;
                      
                  case 3: // Get path & look for query
                      if (c=='?')
                      {
                          // Found query
                          _path=decodePath(uri.substring(mark,i));
                          mark=i+1;
                          state=4;
                          break;
                      }
                      continue;
                }
            }

            // complete last state
            switch(state)
            {
              case 0:
                  _dirty=false;
                  _path=_uri;
                  break;
                  
              case 1:
                  _dirty=true;
                  _path="/";
                  _host=uri.substring(mark);
                  break;
                  
              case 2:
                  _dirty=true;
                  _path="/";
                  _port=Integer.parseInt(uri.substring(mark));
                  break;
              case 3:
                  _dirty=(mark==maxi);
                  _path=decodePath(uri.substring(mark));
                  break;
                  
              case 4:
                  _dirty=false; 
                  if (mark<=maxi)
                      _query=uri.substring(mark);
                  break;
                  
              case 5:
                  _dirty=false; 
            }
        
            if (_query!=null && _query.length()>0)
                _parameters.decode(_query);
            else
                _query=null;           
        }
        catch (Exception e)
        {
            Code.ignore(e);
            throw new IllegalArgumentException("Malformed URI '"+uri+
                                               "' : "+e.toString());
        }        
    }

    /* ------------------------------------------------------------ */
    /** Is the URI an absolute URL? 
     * @return True if the URI has a scheme or host
     */
    public boolean isAbsolute()
    {
        return _scheme!=null || _host!=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri scheme.
     * @return the URI scheme
     */
    public String getScheme()
    {
        return _scheme;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the uri scheme.
     * @param scheme the uri scheme
     */
    public void setScheme(String scheme)
    {
        _scheme=scheme;
        _dirty=true;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri host.
     * @return the URI host
     */
    public String getHost()
    {
        return _host;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the uri host.
     * @param host the uri host
     */
    public void setHost(String host)
    {
        _host=host;
        _dirty=true;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri port.
     * @return the URI port
     */
    public int getPort()
    {
        return _port;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the uri port.
     * A port of 0 implies use the default port.
     * @param port the uri port
     */
    public void setPort(int port)
    {
        _port=port;
        _dirty=true;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri path.
     * @return the URI path
     */
    public String getPath()
    {
        return _path;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the uri path.
     * @param path the URI path
     */
    public void setPath(String path)
    {
        _path=path;
        _dirty=true;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Get the uri query String.
     * @return the URI query string
     */
    public String getQuery()
    {
        if (_dirty)
        {
            _query = _parameters.encode();
            if (_query!=null && _query.length()==0)
                _query=null;
        }
        return _query;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the uri query String.
     * @param query the URI query string
     */
    public void setQuery(String query)
    {
        _dirty=true;
        _query=query;
        _parameters.clear();
        _parameters.decode(query);
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri query _parameters names.
     * @return  Unmodifiable set of URI query _parameters names
     */
    public Set getParameterNames()
    {
        return _parameters.keySet();
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri query _parameters.
     * @return the URI query _parameters
     */
    public MultiMap getParameters()
    {
        _dirty=true;
        return _parameters;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri query _parameters.
     * @return the URI query _parameters
     */
    public MultiMap cloneParameters()
    {
        return (MultiMap)_parameters.clone();
    }
    
    /* ------------------------------------------------------------ */
    /** Get the uri query _parameters.
     * @return the URI query _parameters in an unmodifiable map.
     */
    public Map getUnmodifiableParameters()
    {
        return Collections.unmodifiableMap(_parameters);
    }
    
    /* ------------------------------------------------------------ */
    /** Clear the URI _parameters.
     */
    public void clearParameters()
    {
        _dirty=true;
        _parameters.clear();
    }
    
    /* ------------------------------------------------------------ */
    /** Add encoded _parameters.
     * @param encoded A HTTP encoded string of _parameters: e.g.. "a=1&b=2"
     */
    public void put(String encoded)
    {
        UrlEncoded params = new UrlEncoded(encoded);
        put(params);
    }
    
    /* ------------------------------------------------------------ */
    /** Add name value pair to the uri query _parameters.
     * @param name name of value
     * @param value The value, which may be a multi valued list or
     * String array.
     */
    public Object put(Object name, Object value)
    {
        _dirty=true;
        return _parameters.put(name,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Add dictionary to the uri query _parameters.
     */
    public void put(Map values)
    {
        _dirty=true;
        _parameters.putAll(values);
    }

    /* ------------------------------------------------------------ */
    /** Get named value 
     */
    public String get(String name)
    {
        return (String)_parameters.get(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Get named multiple values.
     * @param name The parameter name
     * @return Umodifiable list of values or null
     */
    public List getValues(String name)
    {
        return _parameters.getValues(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Remove named value 
     */
    public void remove(String name)
    {
        _dirty=true;
        _parameters.remove(name);
    }
    
    /* ------------------------------------------------------------ */
    /** @return the URI string encoded.
     */
    public String toString()
    {
        if (_dirty)
        {
            getQuery();
            StringBuffer buf = new StringBuffer(_uri.length()*2);
            synchronized(buf)
            {
                if (_scheme!=null)
                {
                    buf.append(_scheme);
                    buf.append("://");
                    buf.append(_host);
                    if (_port>0)
                    {
                        buf.append(':');
                        buf.append(_port);
                    }
                }
                encodePath(buf,_path);

                if (_query!=null && _query.length()>0)
                {
                    buf.append('?');
                    buf.append(_query);
                }
                _uri=buf.toString();
                _dirty=false;
            }
        }
        return _uri;
    }

    /* ------------------------------------------------------------ */
    /* Encode a URI path.
     * This is the same encoding offered by URLEncoder, except that
     * the '/' character is not encoded.
     * @param path The path the encode
     * @return The encoded path
     */
    public static String encodePath(String path)
    {
        if (path==null || path.length()==0)
            return path;
        
        StringBuffer buf = new StringBuffer(path.length()<<1);
        encodePath(buf,path);
        return buf.toString();
    }
        
    /* ------------------------------------------------------------ */
    /* Encode a URI path.
     * @param path The path the encode
     * @param buf StringBuffer to encode path into
     */
    public static void encodePath(StringBuffer buf, String path)
    {
        synchronized(buf)
        {
            for (int i=0;i<path.length();i++)
            {
                char c=path.charAt(i);
                switch(c)
                {
                  case '%':
                      buf.append("%25");
                      continue;
                  case '?':
                      buf.append("%3F");
                      continue;
                  case ';':
                      buf.append("%3B");
                      continue;
                  case '#':
                      buf.append("%23");
                      continue;
                  case ' ':
                      buf.append("%20");
                      continue;
                  default:
                      buf.append(c);
                      continue;
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /* Decode a URI path.
     * @param path The path the encode
     * @param buf StringBuffer to encode path into
     */
    public static String decodePath(String path)
    {
        int len=path.length();
        byte[] bytes=null;
        int n=0;
        boolean noDecode=true;
        
        for (int i=0;i<len;i++)
        {
            char c = path.charAt(i);
            if (c<0||c>0xff)
                throw new IllegalArgumentException("Not decoded");
            
            byte b = (byte)(0xff & c);

            if (c=='%' && (i+2)<len)
            {
                noDecode=false;
                b=(byte)(0xff&Integer.parseInt(path.substring(i+1,i+3),16));
                i+=2;
            }
            else if (bytes==null)
            {
                n++;
                continue;
            }
            
            if (bytes==null)
            {
                noDecode=false;
                bytes=new byte[len];
                for (int j=0;j<n;j++)
                    bytes[j]=(byte)(0xff & path.charAt(j));                
            }
            
            bytes[n++]=b;
        }

        if (noDecode)
            return path;

        try
        {    
            return new String(bytes,0,n,StringUtil.__ISO_8859_1);
        }
        catch(UnsupportedEncodingException e)
        {
            Code.warning(e);
            return new String(bytes,0,n);
        }
    }

    /* ------------------------------------------------------------ */
    /** Clone URI.
     * @return cloned URI
     */
    public Object clone()
    {
        return new URI(this);
    }


    /* ------------------------------------------------------------ */
    /** Add two URI path segments.
     * Handles null and empty paths, path and query params (eg ?a=b or
     * ;JSESSIONID=xxx) and avoids duplicate '/'
     * @param p1 URI path segment 
     * @param p2 URI path segment
     * @return Legally combined path segments.
     */
    public static String addPaths(String p1, String p2)
    {
        if (p1==null || p1.length()==0)
        {
            if (p2==null || p2.length()==0)
                return p1;
            return p2;
        }
        if (p2==null || p2.length()==0)
            return p1;

        String p3=null;
        
        int split=p1.indexOf(';');
        if (split<0)
            split=p1.indexOf('?');
        if (split==0)
            return p2+p1;
        if (split<0)
            split=p1.length();

        StringBuffer buf = new StringBuffer(p1.length()+p2.length()+2);
        buf.append(p1);
        
        if (buf.charAt(split-1)=='/')
        {
            if (p2.startsWith("/"))
            {
                buf.deleteCharAt(split-1);
                buf.insert(split-1,p2);
            }
            else
                buf.insert(split,p2);
        }
        else
        {
            if (p2.startsWith("/"))
                buf.insert(split,p2);
            else
            {
                buf.insert(split,'/');
                buf.insert(split+1,p2);
            }
        }

        return buf.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Return the parent Path.
     * Treat a URI like a directory path and return the parent directory.
     * @param p 
     * @return 
     */
    public static String parentPath(String p)
    {
        if (p==null || "/".equals(p))
            return null;
        int slash=p.lastIndexOf('/',p.length()-2);
        if (slash>=0)
            return p.substring(0,slash+1);
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /** Strip parameters from a path.
     * Return path upto any semicolon parameters.
     * @param path 
     * @return 
     */
    public static String stripPath(String path)
    {
        if (path==null)
            return null;
        int semi=path.indexOf(';');
        if (semi<0)
            return path;
        return path.substring(0,semi);
    }
    

    /* ------------------------------------------------------------ */
    /** Convert a path to a cananonical form.
     * All instances of "//", "." and ".." are factored out.  Null is returned
     * if the path tries to .. above it's root.
     * @param path 
     * @return path or null.
     */
    public static String oldCanonicalPath(String path)
    {
        if (path==null || path.length()==0)
            return path;

        int last=-1;
        int slash=path.indexOf('/');
        if (slash<0)
            slash=path.length();

    search:
        while (last<slash)
        {
            switch(slash-last)
            {
              case 1: // double slash
                  if (last<0 || slash==path.length())
                      break;
                  break search;
              case 2: // possible single dot
                  if (path.charAt(last+1)!='.')
                      break;
                  break search;
              case 3: // possible double dot
                  if (path.charAt(last+1)!='.' || path.charAt(last+2)!='.')
                      break;
                  break search;
            }
            
            last=slash;
            slash=path.indexOf('/',last+1);
            if (slash<0)
                slash=path.length();
        }

        // If we have checked the entire string
        if (last>=slash)
            return path;
        
        StringBuffer buf = new StringBuffer(path);

        while (last<slash)
        {
            switch(slash-last)
            {
              case 1: // double slash
                  if (last<0 || slash==buf.length())
                      break;
                  buf.deleteCharAt(last);
                  slash=last;
                  
                  break;
              case 2: // possible single dot
                  if (buf.charAt(last+1)!='.')
                      break;
                  while(slash<buf.length() && buf.charAt(slash)=='/')
                      slash++;
                  if (last<0)
                      buf.delete(0,slash);
                  else
                      buf.delete(last+1,slash);
                  slash=last;
                  break;
              case 3: // possible double dot
                  if (buf.charAt(last+1)!='.' || buf.charAt(last+2)!='.')
                      break;
                  if (last<=0)
                      return null;
                  int i=last-1;
                  while(i>0 && buf.charAt(i)!='/')
                      i--;
                  buf.delete(buf.charAt(i)=='/'?(i+1):i,
                             (slash==buf.length()||buf.charAt(i)=='/')?slash:(slash+1));
                  if (i<0 || buf.length()==0)
                      slash=last=i;
                  else 
                      slash=last=buf.charAt(i)=='/'?i:(i-1);
                  break;
            }            
            
            last=slash;
            if (slash<buf.length())
                slash++;
            while(slash<buf.length() && buf.charAt(slash)!='/')
                slash++;
        }

        return buf.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Convert a path to a cananonical form.
     * All instances of "//", "." and ".." are factored out.  Null is returned
     * if the path tries to .. above it's root.
     * @param path 
     * @return path or null.
     */
    public static String canonicalPath(String path)
    {
        if (path==null || path.length()==0)
            return path;

        int end=path.length();
        int start=path.lastIndexOf('/',end);

    search:
        while (end>=0)
        {
            switch(end-start)
            {
              case 1: // double slash
                  if (start<0 || end==path.length())
                      break;
                  break search;
              case 2: // possible single dot
                  if (path.charAt(start+1)!='.')
                      break;
                  break search;
              case 3: // possible double dot
                  if (path.charAt(start+1)!='.' || path.charAt(start+2)!='.')
                      break;
                  break search;
            }
            
            end=start;
            start=path.lastIndexOf('/',end-1);
        }

        // If we have checked the entire string
        if (start>=end)
            return path;
        
        StringBuffer buf = new StringBuffer(path);
        int delStart=-1;
        int delEnd=-1;
        int skip=0;
        
        while (end>=0)
        {
            switch(end-start)
            {
              case 1: // double slash
                  if (start<0 || end==path.length())
                      break;
                  delStart=start;
                  if (delEnd<0)
                      delEnd=end;
                  if(delStart==0 && delEnd==buf.length())
                  {
                      delStart++;
                      break;
                  }
                          
                  end=start--;
                  while (start>=0 && buf.charAt(start)!='/')
                      start--;
                  continue;
                  
              case 2: // possible single dot
                  if (buf.charAt(start+1)!='.')
                  {
                      if (--skip==0)
                          delStart=start>=0?start:0;
                      break;
                  }
                  
                  if(delEnd<0)
                      delEnd=end;
                  delStart=start;
                  if (delStart<0 || delStart==0&&buf.charAt(delStart)=='/')
                  {
                      delStart++;
                      if (delEnd<buf.length() && buf.charAt(delEnd)=='/')
                          delEnd++;
                      break;
                  }
                  end=start--;
                  while (start>=0 && buf.charAt(start)!='/')
                      start--;
                  continue;
                  
              case 3: // possible double dot
                  if (buf.charAt(start+1)!='.' || buf.charAt(start+2)!='.')
                  {
                      if (--skip==0)
                          delStart=start>=0?start:0;
                      break;
                  }
                  
                  delStart=start;
                  if (delEnd<0)
                      delEnd=end;

                  skip++;
                  
                  end=start--;
                  while (start>=0 && buf.charAt(start)!='/')
                      start--;
                  continue;

              default:
                  if (--skip==0)
                      delStart=start>=0?start:0;
            }            

            // Do the delete
            if (skip<=0 && delStart>=0 && delStart>=0)
            {
                buf.delete(delStart,delEnd);
                delStart=delEnd=-1;
                if (skip>0)
                    delEnd=end;
            }
            
            end=start--;
            while (start>=0 && buf.charAt(start)!='/')
                start--;
        }      

        // Too many ..
        if (skip>0)
            return null;
        
        // Do the delete
        if (delEnd>=0)
            buf.delete(delStart,delEnd);

        // If it was a dir, keep it a dir
        if (path.endsWith(".") && buf.length()>0 && buf.charAt(buf.length()-1)!='/')
            buf.append('/');

        return buf.toString();
    }
    
}



