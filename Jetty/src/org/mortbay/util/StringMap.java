// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting, Sydney
// $Id$
// ========================================================================
package org.mortbay.util;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
 * @version 1.0 Thu Aug 16 2001
 * @author Greg Wilkins (gregw)
 */
public class StringMap extends AbstractMap
{
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
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
            _uchar=Character.toUpperCase(c);
            _lchar=Character.toLowerCase(c);
        }
        public Object getKey(){return _key;}
        public Object getValue(){return _value;}
        public Object setValue(Object o){Object old=_value;_value=(String)o;return old;}
        public String toString(){return "["+_char+":"+_key+"="+_value+"]";}
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class NullEntry implements Map.Entry
    {
        public Object getKey(){return null;}
        public Object getValue(){return _nullValue;}
        public Object setValue(Object o)
            {Object old=_nullValue;_nullValue=(String)o;return old;}
        public String toString(){return "[:null="+_nullValue+"]";}
    }
    
    /* ------------------------------------------------------------ */
    private Node _root;
    private boolean _ignoreCase=false;
    private NullEntry _nullEntry=null;
    private Object _nullValue=null;
    private HashSet _entrySet=new HashSet(3);
    private Set _umEntrySet=Collections.unmodifiableSet(_entrySet);
    
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
    /** Set the ignoreCase attribute.
     * @param ic If true, the map is case insensitive for keys.
     */
    public void setIgnoreCase(boolean ic){_ignoreCase=ic;}
    
    /* ------------------------------------------------------------ */
    public Object put(Object key, Object value)
    {
        if (key==null)
            return put((String)null,value);
        return put(key.toString(),value);
    }
        
    /* ------------------------------------------------------------ */
    public Object put(String key, Object value)
    {
        if (key==null)
        {
            Object oldValue=_nullValue;
            _nullValue=value;
            if (_nullEntry==null)
            {   
                _nullEntry=new NullEntry();
                _entrySet.add(_nullEntry);
            }
            return oldValue;
        }
        
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
            _entrySet.add(up);
            return old;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public Object get(Object key)
    {
        if (key==null)
            return _nullValue;
        if (key instanceof String)
            return get((String)key);
        return get(key.toString());
    }
    
    /* ------------------------------------------------------------ */
    public Object get(String key)
    {
        if (key==null)
            return _nullValue;
        
        Map.Entry entry = getEntry(key,0,key.length());
        if (entry==null)
            return null;
        return entry.getValue();
    }
    
    /* ------------------------------------------------------------ */
    /** Get a map entry by substring key.
     * @param key String containing the key
     * @param offset Offset of the key within the String.
     * @param length The length of the key 
     * @return The Map.Entry for the key or null if the key is not in
     * the map.
     */
    public Map.Entry getEntry(String key,int offset, int length)
    {
        if (key==null)
            return _nullEntry;
        
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
    /** Get a map entry by char array key.
     * @param key char array containing the key
     * @param offset Offset of the key within the array.
     * @param length The length of the key 
     * @return The Map.Entry for the key or null if the key is not in
     * the map.
     */
    public Map.Entry getEntry(char[] key,int offset, int length)
    {
        if (key==null)
            return _nullEntry;
        
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
    /** Get a map entry by byte array key.
     * @param key byte array containing the key. A simple ASCII byte
     * to char mapping is used.
     * @param offset Offset of the key within the array.
     * @param length The length of the key 
     * @return The Map.Entry for the key or null if the key is not in
     * the map.
     */
    public Map.Entry getEntry(byte[] key,int offset, int length)
    {
        if (key==null)
            return _nullEntry;
        
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
    public Object remove(Object key)
    {
        if (key==null)
            return remove((String)null);
        return remove(key.toString());
    }
    
    /* ------------------------------------------------------------ */
    public Object remove(String key)
    {
        if (key==null)
        {
            Object oldValue=_nullValue;
            if (_nullEntry!=null)
            {
                _entrySet.remove(_nullEntry);   
                _nullEntry=null;
                _nullValue=null;
            }
            return oldValue;
        }
        
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
        _entrySet.remove(up);
        up._value=null;
        up._key=null;
        
        return old;
    }

    /* ------------------------------------------------------------ */
    public Set entrySet()
    {
        return _umEntrySet;
    }
    
    /* ------------------------------------------------------------ */
    public int size()
    {
        return _entrySet.size();
    }

    /* ------------------------------------------------------------ */
    public boolean isEmpty()
    {
        return _entrySet.isEmpty();
    }

    /* ------------------------------------------------------------ */
    public boolean containsKey(Object key)
    {
        if (key==null)
            return _nullEntry!=null;
        return
            getEntry(key.toString(),0,key==null?0:key.toString().length())!=null;
    }
    
    /* ------------------------------------------------------------ */
    public void clear()
    {
        _root=null;
        _nullEntry=null;
        _nullValue=null;
        _entrySet.clear();
    }

}
