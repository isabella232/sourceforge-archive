// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================
package org.mortbay.util;

import java.util.Map;

/* ------------------------------------------------------------ */
/** Map like class of Strings to Objects.
 * This String Map has been optimized for mapping small sets of
 * Strings where the most frequently accessed Strings have been put to
 * the map first.
 *
 * It also has the benefit that it can look up entries by substring or
 * sections of char and byte arrays.  This can prevent many String
 * objects from being created just to look up in the map.
 * 
 * This Map does not implement the java.util.Map interface as it has
 * a very specific role and should not be used as a general map unless
 * the developer is very familiar with the implementation.
 *
 * @version 1.0 Thu Aug 16 2001
 * @author Greg Wilkins (gregw)
 */
public class StringMap
{
    private class Node implements Map.Entry
    {
        char _char;
        char _uchar;
        char _lchar;
        Node _next;
        Node _children;
        String _key;
        Object _value;
        
        Node(char c)
        {
            _char=c;
            if (c>='A'&&c<='Z')
            {
                _uchar=c;
                _lchar=(char)(c+'a'-'A');
            }
            else if (c>='a'&&c<='z')
            {
                _uchar=(char)(c+'A'-'a');
                _lchar=c;
            }
            else
            {
                _uchar=c;
                _lchar=c;
            }
        }
        public Object getKey(){return _key;}
        public Object getValue(){return _value;}
        public Object setValue(Object o){Object old=_value;_value=(String)o;return old;}
        public String toString(){return "["+_char+":"+_key+"="+_value+"]";}
    }

    private Node _root;
    private boolean _ignoreCase=false;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public StringMap()
    {}
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param ignoreCase 
     */
    public StringMap(boolean ignoreCase)
    {_ignoreCase=ignoreCase;}
    
    /* ------------------------------------------------------------ */
    /** 
     * @param ic 
     */
    public void setIgnoreCase(boolean ic){_ignoreCase=ic;}
    
    /* ------------------------------------------------------------ */
    /** 
     * @param key 
     * @param value 
     * @return 
     */
    public Object put(String key, Object value)
    {
        Node node = _root;
        Node last = null;
        Node up = null;

        // look for best match
    charLoop:
        for (int i=0;i<key.length();i++)
        {
            char c=key.charAt(i);
            
            // While we have a node to try
            while (node!=null) 
            {
                // If it is a matching node, goto next char
                if (node._char==c || _ignoreCase&&(node._lchar==c||node._uchar==c))
                {
                    last=null;
                    up=node;
                    node=node._children;
                    continue charLoop;
                }
                last=node;
                node=node._next;                
            }

            // We have run out of nodes, so as this is a put, make one
            node = new Node(c);
            if (last!=null)
                last._next=node;
            else if (up!=null)
                up._children=node;
            else
                _root=node;
            up=node;
            last=null;
            node=null;
        }
        if (up!=null)
        {
            Object old = up._value;
            up._key=key;
            up._value=value;
            return old;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param key 
     * @return 
     */
    public Object get(String key)
    {
        Map.Entry entry = getEntry(key,0,key.length());
        if (entry==null)
            return null;
        return entry.getValue();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param key 
     * @param offset 
     * @param length 
     * @return 
     */
    public Map.Entry getEntry(String key,int offset, int length)
    {
        Node node = _root;
        Node up = null;

        // look for best match
    charLoop:
        for (int i=0;i<length;i++)
        {
            char c=key.charAt(offset+i);

            // While we have a node to try
            while (node!=null) 
            {
                // If it is a matching node, goto next char
                if (node._char==c || _ignoreCase&&(node._lchar==c||node._uchar==c))
                {
                    up=node;
                    node=node._children;
                    continue charLoop;
                }
                node=node._next;                
            }
            return null;
        }

        if (up!=null && up._key==null)
            return null;
        return up;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param key 
     * @param offset 
     * @param length 
     * @return 
     */
    public Map.Entry getEntry(char[] key,int offset, int length)
    {
        Node node = _root;
        Node up = null;

        // look for best match
    charLoop:
        for (int i=0;i<length;i++)
        {
            char c=key[offset+i];

            // While we have a node to try
            while (node!=null) 
            {
                // If it is a matching node, goto next char
                if (node._char==c || _ignoreCase&&(node._lchar==c||node._uchar==c))
                {
                    up=node;
                    node=node._children;
                    continue charLoop;
                }
                node=node._next;                
            }
            return null;
        }
        
        if (up!=null && up._key==null)
            return null;
        return up;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param key 
     * @param offset 
     * @param length 
     * @return 
     */
    public Map.Entry getEntry(byte[] key,int offset, int length)
    {
        Node node = _root;
        Node up = null;

        // look for best match
    charLoop:
        for (int i=0;i<length;i++)
        {
            char c=(char)(key[offset+i]);

            // While we have a node to try
            while (node!=null) 
            {
                // If it is a matching node, goto next char
                if (node._char==c || _ignoreCase&&(node._lchar==c||node._uchar==c))
                {
                    up=node;
                    node=node._children;
                    continue charLoop;
                }
                node=node._next;                
            }
            return null;
        }
        
        if (up!=null && up._key==null)
            return null;
        return up;
    }
    
    /* ------------------------------------------------------------ */
    public Object remove(String key)
    {
        Node node = _root;
        Node up = null;

        // look for best match
    charLoop:
        for (int i=0;i<key.length();i++)
        {
            char c=key.charAt(i);

            // While we have a node to try
            while (node!=null) 
            {
                // If it is a matching node, goto next char
                if (node._char==c || _ignoreCase&&(node._lchar==c||node._uchar==c))
                {
                    up=node;
                    node=node._children;
                    continue charLoop;
                }
                node=node._next;                
            }
            return null;
        }

        if (up!=null && up._key==null)
            return null;
        
        Object old = up._value;
        up._value=null;
        
        return old;
    }
}
