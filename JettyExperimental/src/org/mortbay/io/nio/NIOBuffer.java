package org.mortbay.io.nio;

import org.mortbay.io.AbstractBuffer;
import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
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
    private ByteArrayBuffer _subBuffer;
    private int _mark;
    private int _put;
    

    public NIOBuffer(int size, boolean direct)
    {
        super(false);
        _buf = direct
        	?ByteBuffer.allocateDirect(size)
        	:ByteBuffer.allocate(size);
        setPutIndex(0);
    }
    
    public byte[] array()
    {
        return _buf.array();
    }

    /* 
     * @see org.mortbay.io.Buffer#asNonVolatile()
     */
    public Buffer asNonVolatile()
    {
        if (!_volatile)
            return this;
        return new ByteArrayBuffer(array(), getIndex(), length(), isReadOnly());
    }

    /*
     * @see org.mortbay.util.Buffer#asReadOnlyBuffer()
     */
    public Buffer asReadOnlyBuffer()
    {
        if (!isReadOnly())
        {
            byte[] bytes= new byte[putIndex() - getIndex()];
            Portable.arraycopy(array(), getIndex(), bytes, 0, bytes.length);
            ByteArrayBuffer view=new ByteArrayBuffer(bytes, 0, length(), READONLY);
            return view;
        }
        else
            return this;
    }
    
    public int capacity()
    {
        return _buf.capacity();
    }
    
    public Buffer duplicate()
    {
        byte[] bytes= new byte[capacity()];
        if (markIndex() < 0)
            Portable.arraycopy(array(), getIndex(), bytes, getIndex(), length());
        else
            Portable.arraycopy(
                array(),
                markIndex(),
                bytes,
                markIndex(),
                putIndex() - markIndex());

        ByteArrayBuffer view=
            new ByteArrayBuffer(bytes, getIndex(), length(), !READONLY);
        view.setMarkIndex(markIndex());
        
        return view;
    }
    
    public byte get()
    {
      return _buf.get();
    }
    
    public int get(byte[] b, int offset, int length)
    {
        int l = length;
        if (l>length())
            l=length();
        if (l<=0)
            return -1;

        _buf.get(b,offset,length); 
        return l;
    }
    
    public boolean isVolatile()
    {
        return _volatile;
    }

    public byte peek(int position)
    {
        if (position < 0)
            Portable.throwIllegalArgument("position<0: " + position + "<0");
        if (position > capacity())
            Portable.throwIllegalArgument("position>capacity(): " + position + ">" + capacity());
        return _buf.get(position);
    }

    public Buffer peek(int position, int length)
    {
        if (position < 0)
            Portable.throwIllegalArgument("position<0: " + position + "<0");
        if (position + length > capacity())
            Portable.throwIllegalArgument(
                "position+length>capacity(): " + position + "+" + length + ">" + capacity());
        return subBuffer(position, length);
    }

    public void poke(int position, byte b)
    {
        if (isReadOnly())
            Portable.throwIllegalState("readOnly");
        if (position < 0)
            Portable.throwIllegalArgument("position<0: " + position + "<0");
        if (position > capacity())
            Portable.throwIllegalArgument("position>capacity(): " + position + ">" + capacity());

        _buf.put(position,b);
    }

    protected Buffer subBuffer(int position, int length)
    {
        if (_subBuffer == null)
        {
            _subBuffer= new ByteArrayBuffer(array(), position, length, READWRITE, Buffer.VOLATILE);
        }
        else
        {
            _subBuffer.setGetIndex(0);
            _subBuffer.setPutIndex(position + length);
            _subBuffer.setGetIndex(position);
        }
        return _subBuffer;
    }

    public String toString()
    {
        if (_string != null)
            return _string;
        if (isReadOnly() && !isVolatile())
        {
            _string= new String(array(), getIndex(), length());
            return _string;
        }

        return new String(array(), getIndex(), length());
    }

    public int getIndex()
    {
      return _buf.position();
    }

    public int markIndex()
    {
      return _mark;
    }

    public int putIndex()
    {
      return _put;
    }

    public void setGetIndex(int newGet)
    {
      _buf.position(newGet);
    }

    public void setMarkIndex(int newMark)
    {
      _mark = newMark;
    }

    public void setPutIndex(int newPutIndex)
    {
      _put=newPutIndex;
    }
    

}
