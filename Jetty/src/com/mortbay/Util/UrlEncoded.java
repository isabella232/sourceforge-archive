// =========================================================================== 
// $Id$
package com.mortbay.Util;
//import com.sun.java.util.collections.*; XXX-JDK1.1

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
                Map.Entry entry =
                    (Map.Entry)i.next();
                
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
        int len=encoded.length();
        byte[] bytes=new byte[len];
        char[] characters = encoded.toCharArray();
        int n=0;
        boolean noDecode=true;
        
        for (int i=0;i<len;i++)
        {
            char c = characters[i];
            if (c<0||c>0x7f)
                throw new IllegalArgumentException("Not encoded");
            
            byte b = (byte)(0x7f & c);
            
            if (c=='+')
            {
                noDecode=false;
                b=(byte)' ';
            }
            else if (c=='%' && (i+2)<len)
            {
                noDecode=false;
                b=(byte)(0xff&Integer.parseInt(encoded.substring(i+1,i+3),16));
                i+=2;
            }
            
            bytes[n++]=b;
        }

        if (noDecode)
            return encoded;

        try
        {    
            return new String(bytes,0,n,"ISO-8859-1");
        }
        catch(UnsupportedEncodingException e)
        {
            Code.warning(e);
            return new String(bytes,0,n);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Perform URL encoding.
     * Simply calls URLEncoder.encode
     * @param string 
     * @return encoded string.
     */
    public static String encodeString(String string)
    {
        byte[] bytes=null;
        try
        {
            bytes=string.getBytes("ISO-8859-1");
        }
        catch(UnsupportedEncodingException e)
        {
            Code.warning(e);
            bytes=string.getBytes();
        }
        
        int len=bytes.length;
        byte[] encoded= new byte[bytes.length*3];
        int n=0;
        boolean noEncode=true;
        
        for (int i=0;i<len;i++)
        {
            byte b = bytes[i];
            
            if (b==' ')
            {
                noEncode=false;
                encoded[n++]=(byte)'+';
            }
            else if (b>='a' && b<='z' ||
                     b>='A' && b<='Z' ||
                     b>='0' && b<='9')
            {
                encoded[n++]=b;
            }
            else
            {
                noEncode=false;
                encoded[n++]=(byte)'%';
                byte nibble= (byte) ((b&0xf0)>>4);
                if (nibble>=10)
                    encoded[n++]=(byte)('A'+nibble-10);
                else
                    encoded[n++]=(byte)('0'+nibble);
                nibble= (byte) (b&0xf);
                if (nibble>=10)
                    encoded[n++]=(byte)('A'+nibble-10);
                else
                    encoded[n++]=(byte)('0'+nibble);
            }
        }

        if (noEncode)
            return string;
        
        try
        {    
            return new String(encoded,0,n,"ISO-8859-1");
        }
        catch(UnsupportedEncodingException e)
        {
            Code.warning(e);
            return new String(encoded,0,n);
        }
    }
}
