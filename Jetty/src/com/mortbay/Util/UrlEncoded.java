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
public class UrlEncoded extends MultiMap
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

                // encode single values and extract multi values
                if (value==null)
                {
                    result.append(separator);
                    separator="&";
                    result.append(URLEncoder.encode(key));
                    if (equalsForNullValue)
                        result.append('=');
                }
                else if (value instanceof List)
                {
                    // encode multi values
                    List values=(List)value;
                    for (int v=0; v<values.size();v++)
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
                else
                {
                    // Encode single item
                    result.append(separator);
                    separator="&";
                    result.append(URLEncoder.encode(key));
                    result.append('=');
                    result.append(URLEncoder.encode(value.toString()));
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
    public static void decodeTo(String content,MultiMap map)
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

                // Add value to the map
                if (name.length() > 0)
                    map.add(name,value);
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
         int marker = encoded.indexOf('%', 0);

         // if no encoded characters return the original
         if (marker == -1)
             return encoded;

         String result = encoded;
         try {
             int encodedLength = encoded.length();
             ByteArrayOutputStream out=new ByteArrayOutputStream(encodedLength);
             synchronized(out)
             {
                 do
                 {
                     // Write the part before the %
                     out.write(encoded.substring(index,marker).getBytes("ISO8859_1"));
                     
                     try
                     {
                         // convert the 2 hex chars following the % into a byte
                         out.write((byte)(Integer.parseInt(encoded.substring(marker+1,marker+3),16)));
                         index = marker+3;
                     }
                     catch (NumberFormatException e)
                     {
                         //conversion failed so pass through this %
                         if (Code.verbose()) Code.warning(e);
                         out.write('%');
                         index = marker+1;
                     }
                 }
                 while (((marker = encoded.indexOf('%', index)) != -1));

                 // if there is some at the end then copy it in
                 if (index < encodedLength)
                     out.write(encoded.substring(index,encodedLength).getBytes("ISO8859_1"));
                 
                 result = out.toString();
             }
         }
         catch (Exception e) {
             Code.warning(e);
         }
         
         return result;
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





