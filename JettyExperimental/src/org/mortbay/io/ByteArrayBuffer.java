package org.mortbay.io;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class ByteArrayBuffer extends AbstractBuffer
{
    private byte[] _bytes;
    private ByteArrayBuffer _subBuffer;
    private boolean _volatile;
    private String _string;

    public ByteArrayBuffer(byte[] bytes, int position, int length, boolean readonly)
    {
        super(readonly);
        _bytes= bytes;
        limit(position + length);
        position(position);
    }

    public ByteArrayBuffer(byte[] bytes)
    {
        this(bytes, 0, bytes.length, READWRITE);
    }

    public ByteArrayBuffer(byte[] bytes, int position, int length)
    {
        this(bytes, position, length, READWRITE);
    }

    public ByteArrayBuffer(String value)
    {
        super(READWRITE);
        _bytes= Portable.getBytes(value);
        position(0);
        limit(_bytes.length);
        _readOnly= READONLY;
        _string= value;
    }

    public ByteArrayBuffer(int size)
    {
        this(new byte[size], 0, size, READWRITE);
    }
    
    /**
     * 
     */
    public byte[] getByteArray()
    {
        return _bytes;
    }

    /**
     * @see org.mortbay.util.Buffer#capacity()
     */
    public int capacity()
    {
        return _bytes.length;
    }

    /**
     * @see org.mortbay.io.Buffer#get(byte[], int, int)
     */
    public int get(byte[] b, int offset, int length)
    {
        int l = length;
        if (l>remaining())
            l=remaining();
        if (l<=0)
            return -1;
        Portable.arraycopy(_bytes, position(), b, offset, l);
        position(position()+l);
        return l;
    }

    /**
     * @see org.mortbay.util.Buffer#get(int)
     */
    public byte get(int position)
    {
        if (position < 0)
            Portable.throwIllegalArgument("position<0: " + position + "<0");
        if (position > capacity())
            Portable.throwIllegalArgument("position>capacity(): " + position + ">" + capacity());
        return _bytes[position];
    }

    /**
     * @see org.mortbay.util.Buffer#get(int, int)
     */
    public Buffer getBuffer(int position, int length)
    {
        if (position < 0)
            Portable.throwIllegalArgument("position<0: " + position + "<0");
        if (position + length > capacity())
            Portable.throwIllegalArgument(
                "position+length>capacity(): " + position + "+" + length + ">" + capacity());
        return subBuffer(position, length);
    }

    /** 
     * @see org.mortbay.util.Buffer#put(int, byte)
     */
    public void put(int position, byte b)
    {
        if (isReadOnly())
            Portable.throwIllegalState("readOnly");

        if (position < 0)
            Portable.throwIllegalArgument("position<0: " + position + "<0");
        if (position > capacity())
            Portable.throwIllegalArgument("position>capacity(): " + position + ">" + capacity());

        _bytes[position]= b;
    }

    /** 
     * @see org.mortbay.util.Buffer#put(int, org.mortbay.util.Buffer)
     */
    public void put(int position, Buffer src)
    {
        if (src instanceof ByteArrayBuffer)
        {
            if (isReadOnly())
                Portable.throwIllegalState("read only");
            if (position < 0)
                Portable.throwIllegalArgument("position<0: " + position + "<0");
            if (position + src.remaining() > capacity())
                Portable.throwIllegalArgument(
                    "position+length>capacity(): "
                        + position
                        + "+"
                        + src.remaining()
                        + ">"
                        + capacity());

            Portable.arraycopy(
                ((ByteArrayBuffer)src).getByteArray(),
                src.position(),
                _bytes,
                position,
                src.remaining());
        }
        else
            super.put(position, src);
    }

    public void compact()
    {
        int s= markValue() >= 0 ? markValue() : position();
        if (s > 0)
        {
            Portable.arraycopy(getByteArray(), s, getByteArray(), 0, limit() - s);
            if (markValue() > 0)
                markValue(markValue() - s);
            position(position() - s);
            limit(limit() - s);
        }
    }

    /**
     * @see org.mortbay.util.Buffer#fill()
     */
    public int fill()
    {
        return -1;
    }

    /**
     * @see org.mortbay.util.Buffer#flush()
     */
    public int flush()
    {
        return -1;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.Buffer#duplicate()
     */
    public Buffer duplicate()
    {
        byte[] bytes= new byte[capacity()];
        if (markValue() < 0)
            Portable.arraycopy(getByteArray(), position(), bytes, position(), remaining());
        else
            Portable.arraycopy(
                getByteArray(),
                markValue(),
                bytes,
                markValue(),
                limit() - markValue());

        ByteArrayBuffer view=
            new ByteArrayBuffer(getByteArray(), position(), remaining(), !READONLY);
        view.markValue(markValue());
        return view;
    }

    /*
     * @see org.mortbay.util.Buffer#asReadOnlyBuffer()
     */
    public Buffer asReadOnlyBuffer()
    {
        if (!isReadOnly())
        {
            byte[] bytes= new byte[limit() - position()];
            Portable.arraycopy(getByteArray(), position(), bytes, 0, bytes.length);
            ByteArrayBuffer view=
                new ByteArrayBuffer(getByteArray(), position(), remaining(), READONLY);
            return view;
        }
        else
            return this;
    }

    /* 
     * @see org.mortbay.io.Buffer#toArray()
     */
    public byte[] asArray()
    {
        byte[] bytes= new byte[limit() - position()];
        Portable.arraycopy(getByteArray(), position(), bytes, 0, bytes.length);
        return bytes;
    }

    /* 
     * @see org.mortbay.io.Buffer#isVolatile()
     */
    public boolean isVolatile()
    {
        return _volatile;
    }

    /* 
     * @see org.mortbay.io.Buffer#asNonVolatile()
     */
    public Buffer asNonVolatile()
    {
        if (!_volatile)
            return this;
        return new ByteArrayBuffer(_bytes, position(), remaining(), isReadOnly());
    }

    protected Buffer subBuffer(int position, int length)
    {
        if (_subBuffer == null)
        {
            _subBuffer= new ByteArrayBuffer(getByteArray(), position, length, READWRITE);
            _subBuffer._volatile= true;
        }
        else
        {
            _subBuffer.position(0);
            _subBuffer.limit(position + length);
            _subBuffer.position(position);
        }
        return _subBuffer;
    }

    public String toString()
    {
        if (_string != null)
            return _string;
        if (isReadOnly() && !isVolatile())
        {
            _string= new String(getByteArray(), position(), remaining());
            return _string;
        }

        return new String(getByteArray(), position(), remaining());
    }

}
