package org.mortbay.io;


/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class ByteArrayBuffer extends AbstractBuffer
{
    public ByteArrayBuffer(byte[] bytes)
    {
        this(bytes, 0, bytes.length, READWRITE);
    }

    public ByteArrayBuffer(byte[] bytes, int position, int length)
    {
        this(bytes, position, length, READWRITE);
    }

    public ByteArrayBuffer(byte[] bytes, int position, int length, boolean readonly)
    {
        super(READWRITE);
        _bytes= bytes;
        setPutIndex(position + length);
        setGetIndex(position);
        this._readOnly=readonly;
    }

    public ByteArrayBuffer(int size)
    {
        this(new byte[size], 0, size, READWRITE);
        setPutIndex(0);
    }

    public ByteArrayBuffer(String value)
    {
        super(READWRITE);
        _bytes= Portable.getBytes(value);
        setGetIndex(0);
        setPutIndex(_bytes.length);
        _readOnly= READONLY;
        _string= value;
    }
    
    public byte[] array()
    {
        return _bytes;
    }

    /* 
     * @see org.mortbay.io.Buffer#toArray()
     */
    public byte[] asArray()
    {
        byte[] bytes= new byte[putIndex() - getIndex()];
        Portable.arraycopy(array(), getIndex(), bytes, 0, bytes.length);
        return bytes;
    }

    /* 
     * @see org.mortbay.io.Buffer#asNonVolatile()
     */
    public Buffer asNonVolatile()
    {
        if (!_volatile)
            return this;
        return new ByteArrayBuffer(_bytes, getIndex(), length(), isReadOnly());
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
            ByteArrayBuffer view=
                new ByteArrayBuffer(array(), getIndex(), length(), READONLY);
            return view;
        }
        else
            return this;
    }
    
    public int capacity()
    {
        return _bytes.length;
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
            new ByteArrayBuffer(array(), getIndex(), length(), !READONLY);
        view.setMarkIndex(markIndex());
        return view;
    }
    
    public int get(byte[] b, int offset, int length)
    {
        int l = length;
        if (l>length())
            l=length();
        if (l<=0)
            return -1;
        Portable.arraycopy(_bytes, getIndex(), b, offset, l);
        setGetIndex(getIndex()+l);
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
        return _bytes[position];
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

        _bytes[position]= b;
    }

    protected Buffer subBuffer(int position, int length)
    {
        if (_subBuffer == null)
        {
            _subBuffer= new ByteArrayBuffer(array(), position, length, READWRITE);
            _subBuffer._volatile= true;
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
    
    private byte[] _bytes;
    private String _string;
    private ByteArrayBuffer _subBuffer;
    private boolean _volatile;

}
