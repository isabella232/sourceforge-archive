// =========================================================================== 
// $Id$
package com.mortbay.Util;

import com.sun.java.util.collections.*;
import java.io.*;
import java.util.*;
import java.net.*;

/* ------------------------------------------------------------ */
/** Handles coding of MIME  "x-www-form-urlencoded".
 * This class handles the encoding and decoding for either
 * the query string of a URL or the content of a POST HTTP request.
 *
 * <p><h4>Notes</h4>
 * The hashtable either contains String single values, vectors
 * of String or arrays of Strings.
 *
 * This class is only partially synchronised.  In particular, simple
 * get operations are not protected from concurrent updates.
 *
 * @see java.net.URLEncoder
 * @version 1.0 Fri Dec 12 1997
 * @author Greg Wilkins (gregw)
 */
public class UrlEncoded extends HashMap
{
    /* ----------------------------------------------------------------- */
    public UrlEncoded(UrlEncoded url)
    {
        super(url);
    }
    
    /* ----------------------------------------------------------------- */
    public UrlEncoded()
    {
        super(10);
    }
    
    /* ----------------------------------------------------------------- */
    public UrlEncoded(String s)
    {
        super(10);
        decode(s);
    }
    
    /* ----------------------------------------------------------------- */
    public void decode(String query)
    {
        decodeTo(query,this);
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
            if (o instanceof List)
            {
                List v=(List)o;
                if (v.size()>0)
                {
                    StringBuffer values=new StringBuffer(128);
                    synchronized(values)
                    {
                        values.append(v.get(0).toString());
                        for (int i=1; i<v.size(); i++)              
                        {
                            values.append(',');
                            values.append(v.get(i).toString());
                        }   
                        return values.toString();
                    }
                }
                return null;
            }
            
            if (o instanceof String[])
            {
                String[] a=(String[])o;
                if (a.length>0)
                {
                    StringBuffer values =new StringBuffer(128);
                    synchronized(values)
                    {
                        values.append(a[0]);
                        for (int i=1; i<a.length; i++)
                        {
                            values.append(',');
                            values.append(a[i]);
                        }
                        return values.toString();
                    }
                }
                return null;
            }
        }
        return o;
    }
    
    /* ------------------------------------------------------------ */
    private Object getObject(Object key)
    {
        return super.get(key);
    }
    
    /* ------------------------------------------------------------ */
    /** Get multiple values as an array.
     * Multiple values must be specified as "N=A&N=B"
     * @param key The parameter name
     * @return array of values or null
     */
    public List getValues(String key)
    {
        Object o = super.get(key);
        if (o==null)
            return null;
        if (o instanceof String[])
            return Collections.unmodifiableList(Arrays.asList((String[])o));
        if (o instanceof List)
            return Collections.unmodifiableList((List)o);

        String[] a = {o.toString()};
        return Collections.unmodifiableList(Arrays.asList(a));
    }
    
    
    /* ------------------------------------------------------------ */
    /** Put a parameter.
     * If the paramter is multi valued (List, vector or array of Strings),
     * it is included as a multivalued parameter. Otherwise the value
     * is put as the result of toString() call.
     * @param key The parameter name
     * @param values Array of string values
     */
    public synchronized Object put(Object key, Object value)
    {
        if (value instanceof java.util.Vector)
        {
            java.util.Vector v = (java.util.Vector)value;
            ArrayList l = new ArrayList(v.size());
            for (int i=0;i<v.size();i++)
                l.add(v.elementAt(i));
            value=l;
        }
        
        return super.put(key,value);
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
     * for parameters without a value. e.g. "blah?a=&b=&c=".
     */
    public synchronized String encode(boolean equalsForNullValue)
    {        
        StringBuffer result = new StringBuffer(128);
        synchronized(result)
        {
            Iterator i = entrySet().iterator();
            String separator="";
            while(i.hasNext())
            {
                com.sun.java.util.collections.Map.Entry entry =
                    (com.sun.java.util.collections.Map.Entry)i.next();
                
                String key = entry.getKey().toString();
                Object value = entry.getValue();
                List values = null;

                // encode single values and extract multi values
                if (value==null)
                {
                    result.append(separator);
                    separator="&";
                    result.append(URLEncoder.encode(key));
                    if (equalsForNullValue)
                        result.append('=');
                }
                else if (value instanceof String[])
                    values=Arrays.asList((String[])value);
                else if (value instanceof List)
                    values=(List)value;
                else
                {
                    result.append(separator);
                    separator="&";
                    result.append(URLEncoder.encode(key));
                    result.append('=');
                    result.append(URLEncoder.encode(value.toString()));
                }

                // encode multi values
                for (int v=0; values!=null && v<values.size();v++)
                {
                    result.append(separator);
                    separator="&";
                    result.append(URLEncoder.encode(key));
                    value=values.get(v);
                    if (value!=null)
                    {
                        result.append('=');
                        result.append(URLEncoder.encode(value.toString()));
                    }
                    else if (equalsForNullValue)
                        result.append('=');
                }
            }
            return result.toString();
        }
    }

    
    /* -------------------------------------------------------------- */
    /* Decoded parameters to Map.
     * @param content the string containing the encoded parameters
     * @param url The dictionary to add the parameters to
     */
    public static void decodeTo(String content,Map map)
    {
        synchronized(map)
        {
            String token;
            String name;
            String value;

            StringTokenizer tokenizer =
                new StringTokenizer(content, "&", false);

            while ((tokenizer.hasMoreTokens()))
            {
                token = tokenizer.nextToken();
            
                // breaking it at the "=" sign
                int i = token.indexOf('=');
                if (i<0)
                {
                    name=decodeString(token);
                    value=null;
                }
                else
                {
                    name=decodeString(token.substring(0,i++));
                    if (i>=token.length())
                        value=null;
                    else
                        value = decodeString(token.substring(i));
                }

                // Set value in the map
                if (name.length() > 0)
                {
                    Object o;
                    if (map instanceof UrlEncoded)
                        o=((UrlEncoded)map).getObject(name);
                    else
                        o=map.get(name);
                    if (o!=null)
                    {
                        if (o instanceof List)
                            ((List)o).add(value);
                        else
                        {
                            ArrayList l = new ArrayList(8);
                            l.add(o);
                            l.add(value);
                            map.put(name,l);
                        }
                    }
                    else
                        map.put(name,value);
                }
            }
        }
    }
    
    /* -------------------------------------------------------------- */
    /** Decode String with % encoding
     */
    public static String decodeString(String encoded)
    {
        encoded = encoded.replace('+',' ');
        int index = 0;
        int marker = 0;   

        // there is at least one encoding
        boolean decoded=false;
        StringBuffer result=new StringBuffer(encoded.length());
        synchronized(result)
        {
            while (((marker = encoded.indexOf('%', index)) != -1)
                   &&(index < encoded.length()))
            {
                decoded=true;
                result.append(encoded.substring (index, marker));
                
                try
                {
                    // convert the 2 hex chars following the % into a byte,
                    // which will be a character
                    result.append((char)(Integer.parseInt
                                         (encoded.substring(marker+1,marker+3),16)));
                    index = marker+3;  
                }
                catch (Exception e)
                {
                    //conversion failed so ignore this %
                    if (Code.verbose()) Code.warning(e);
                    result.append ('%');
                    index = marker+1;
                }
            }

            // if no encoded characters return the original
            if (!decoded)
                return encoded;

            // if there is some at the end then copy it in
            if (index < encoded.length())
                result.append(encoded.substring(index, encoded.length()));
        }
        
        return result.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Perform URL encoding.
     * Simply calls URLEncoder.encode
     * @param string 
     * @return encoded string.
     */
    public static String encodeString(String string)
    {
        return URLEncoder.encode(string);    
    }
}





