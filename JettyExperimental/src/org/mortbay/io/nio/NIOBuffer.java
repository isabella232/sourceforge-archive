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

import java.nio.ByteBuffer;

import org.mortbay.io.AbstractBuffer;
import org.mortbay.io.Buffer;
import org.mortbay.io.Portable;

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

    public int poke(int index, Buffer src)
    {
        if (isReadOnly()) Portable.throwIllegalState(__READONLY);
        
        byte[] array=src.array();
        if (array!=null)
        {
            int length = poke(index,array,src.getIndex(),src.length());
            if (!src.isImmutable())
                src.setGetIndex(src.getIndex()+length);
            return length;
        }
        else
        {
            Buffer src_buf=src.buffer();
            if (src_buf instanceof NIOBuffer)
            {
                ByteBuffer src_bytebuf = ((NIOBuffer)src_buf)._buf;
                if (src_bytebuf==_buf)
                    src_bytebuf=_buf.duplicate();
                try
                {
                    int length=src.length();
                    if (length>space())    
                        length=space();
                    
                    src_bytebuf.position(src.getIndex());
                    src_bytebuf.limit(src.getIndex()+length);
                    
                    _buf.position(index);
                    _buf.put(src_bytebuf);
                    if (!src.isImmutable())
                        src.setGetIndex(src.getIndex()+length);
                    return length;
                }
                finally
                {
                    _buf.position(0);
                    src_bytebuf.limit(src_bytebuf.capacity());
                    src_bytebuf.position(0);
                }
            }
            else
                return super.poke(index,src);
        }
    }
    

    public int poke(int index, byte[] b, int offset, int length)
    {
        if (isReadOnly()) Portable.throwIllegalState(__READONLY);
        try
        {
            if (index+length>capacity())
                length=capacity()-index;
            _buf.position(index);
            _buf.put(b,offset,length);
            return length;
        }
        finally
        {
            _buf.position(0);
        }
    }
    
    ByteBuffer getByteBuffer()
    {
        return _buf;
    }
}
