/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 07-Apr-2003
 * $Id$
 * ============================================== */
 
package org.mortbay.util;

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
    public void add(String value, int index)
    {
    	Buffer buffer = new ByteArrayBuffer(value);
      	_map.put(buffer,new Integer(index));
    
    	while ((index-_index.size())>0)
    		_index.add(null);
    	
    	_index.add(index,buffer); 	
      	  
    } 
    
    public int lookupIndex(Buffer buffer)
    {
    	Integer index=(Integer)_map.get(buffer);
    	if (index==null)
    		return -1;
    	return index.intValue();
    }
    
	public Buffer lookupBuffer(int index)
	{
		if (index<0 || index>=_index.size())
			return null;
		return (Buffer)_index.get(index);
	}

	public Buffer lookupBuffer(Buffer buffer)
	{
		return lookupBuffer(lookupIndex(buffer));
	}
	
    public Buffer normalizeBuffer(Buffer buffer)
    {
    	Buffer b = lookupBuffer(lookupIndex(buffer));
    	if (b==null)
    		return buffer;
    	return b;
    }

	public String toString(Buffer buffer)
	{
		return normalizeBuffer(buffer).toString();
	}
    
}
