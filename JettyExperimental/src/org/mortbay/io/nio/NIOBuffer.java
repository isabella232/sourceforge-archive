// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.io.nio;

import org.mortbay.io.AbstractBuffer;
import org.mortbay.io.Buffer;
import org.mortbay.io.Portable;
import java.nio.ByteBuffer;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class NIOBuffer extends AbstractBuffer
{
  	public final static boolean 
  		DIRECT=true,
  		INDIRECT=false;
  	
    private String _string;
    private boolean _volatile;
    private ByteBuffer _buf;

    public NIOBuffer(int size, boolean direct)
    {
        super(READWRITE,NON_VOLATILE);
        _buf = direct
        	?ByteBuffer.allocateDirect(size)
        	:ByteBuffer.allocate(size);
        _buf.position(0);
        _buf.limit(_buf.capacity());

    }
    
    public byte[] array()
    {
        if (!_buf.hasArray())
            return null;
        return _buf.array();
    }
    
    public int capacity()
    {
        return _buf.capacity();
    }

    public byte peek(int position)
    {
        return _buf.get(position);
    }

    public int peek(int index, byte[] b, int offset, int length)
    {
        int l = length;
        if (index+l > capacity())
            l=capacity()-index;
        if (l <= 0) 
            return -1;
        try
        {
            _buf.position(index);
            _buf.get(b,offset,length);
        }
        finally
        {
            _buf.position(0);
        }
        
        return l;
    }

    public void poke(int position, byte b)
    {
        if (isReadOnly()) Portable.throwIllegalState(__READONLY);
        _buf.put(position,b);
    }

    public void poke(int index, Buffer src)
    {
        if (isReadOnly()) Portable.throwIllegalState(__READONLY);
        
        byte[] array=src.array();
        if (array!=null)
        {
            poke(index,array,src.getIndex(),src.length());
        }
        else
        {
            Buffer buf=src.buffer();
            if (buf instanceof NIOBuffer)
            {
                ByteBuffer b = ((NIOBuffer)buf)._buf;
                if (b==_buf)
                    b=_buf.duplicate();
                try
                {
                    _buf.position(index);
                    b.limit(src.putIndex());
                    b.position(src.getIndex());
                    _buf.put(b);
                }
                finally
                {
                    _buf.position(0);
                    b.limit(b.capacity());
                    b.position(0);
                }
            }
            else
                super.poke(index,src);
        }
    }
    

    public void poke(int index, byte[] b, int offset, int length)
    {
        if (isReadOnly()) Portable.throwIllegalState(__READONLY);
        try
        {
            _buf.position(index);
            _buf.put(b,offset,length);
        }
        finally
        {
            _buf.position(0);
        }
    }
}
