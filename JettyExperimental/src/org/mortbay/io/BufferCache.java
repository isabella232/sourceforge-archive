/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 07-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.io;

import java.util.ArrayList;
import java.util.HashMap;

import org.mortbay.util.StringMap;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class BufferCache
{
    private HashMap _bufferMap=new HashMap();
    private StringMap _stringMap=new StringMap(StringMap.CASE_INSENSTIVE);
    private ArrayList _index= new ArrayList();

    /* ------------------------------------------------------------------------------- */
    /** add.
     * @param GET
     * @param GET_METHOD
     */
    public CachedBuffer add(String value, int ordinal)
    {
        CachedBuffer buffer= new CachedBuffer(value, ordinal);
        _bufferMap.put(buffer, buffer);
        _stringMap.put(value, buffer);
        while ((ordinal - _index.size()) > 0)
            _index.add(null);
        _index.add(ordinal, buffer);
        return buffer;
    }

    public CachedBuffer get(int ordinal)
    {
        if (ordinal < 0 || ordinal >= _index.size())
            return null;
        return (CachedBuffer)_index.get(ordinal);
    }

    public CachedBuffer get(Buffer buffer)
    {
        return (CachedBuffer)_bufferMap.get(buffer);
    }

    public CachedBuffer get(String value)
    {
        return (CachedBuffer)_stringMap.get(value);
    }

    public Buffer lookup(Buffer buffer)
    {
        Buffer b= get(buffer);
        if (b == null)
            return buffer;
        return b;
    }

    public Buffer lookup(String value)
    {
        Buffer b= get(value);
        if (b == null)
            return new CachedBuffer(value,-1);
        return b;
    }

    public String toString(Buffer buffer)
    {
        return lookup(buffer).toString();
    }

    public static int getOrdinal(Buffer buffer)
    {
        if (buffer instanceof CachedBuffer)
            return ((CachedBuffer)buffer).getOrdinal();
        return -1;
    }
    
    public class CachedBuffer extends ByteArrayBuffer
    {
        private int _ordinal;
        public CachedBuffer(String value, int ordinal)
        {
            super(value);
            _ordinal= ordinal;
        }

        public int getOrdinal()
        {
            return _ordinal;
        }
    }
}
