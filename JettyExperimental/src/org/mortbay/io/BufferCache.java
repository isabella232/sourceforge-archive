/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 07-Apr-2003
 * $Id$
 * ============================================== */
 
package org.mortbay.io;

import java.util.ArrayList;
import java.util.HashMap;


/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class BufferCache
{
	private HashMap _map=new HashMap();
	private ArrayList _index=new ArrayList();
	
    /* ------------------------------------------------------------------------------- */
    /** add.
     * @param GET
     * @param GET_METHOD
     */
    public void add(String value, int ordinal)
    {
    	Buffer buffer = new CachedBuffer(value,ordinal);
      	_map.put(buffer,buffer);
    	while ((ordinal-_index.size())>0)
    		_index.add(null);
    	_index.add(ordinal,buffer); 	
    } 
    
	public CachedBuffer lookup(int ordinal)
	{
		if (ordinal<0 || ordinal>=_index.size())
			return null;
		return (CachedBuffer)_index.get(ordinal);
	}

	public CachedBuffer lookup(Buffer buffer)
	{
		return (CachedBuffer)_map.get(buffer);
	}
	
    public Buffer normalize(Buffer buffer)
    {
    	Buffer b = lookup(buffer);
    	if (b==null)
    		return buffer;
    	return b;
    }

	public String toString(Buffer buffer)
	{
		return normalize(buffer).toString();
	}
	
	public class CachedBuffer extends ByteArrayBuffer
	{
		private int _ordinal;
        public CachedBuffer(String value,int ordinal)
        {
            super(value);
            _ordinal=ordinal;
        }
        
        public int getOrdinal()
        {
        	return _ordinal;
        }
	}
}
