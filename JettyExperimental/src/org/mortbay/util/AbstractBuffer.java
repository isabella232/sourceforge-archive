package org.mortbay.util;

/**
 * @author gregw
 *
 */
public abstract class AbstractBuffer implements Buffer
{
    protected final static boolean __MUTABLE= true;

    private int _mark;
    private int _offset;
    private int _limit;
    private boolean _mutable;

    private ByteArrayBuffer _subBuffer;

    /**
     * Constructor for BufferView
     */
    public AbstractBuffer(boolean mutable)
    {
        _mark= -1;
        _mutable=mutable;
    }

    public int offset()
    {
        return _offset;
    }

    public void offset(int newOffset)
    {
        if (!_mutable)
            Portable.throwIllegalState("immutable");
        if (newOffset < 0)
            Portable.throwIllegalArgument("newoffset<0: " + newOffset + "<0");
        if (newOffset > limit())
            Portable.throwIllegalArgument("newoffset>limit: " + newOffset + ">" + limit());
        _offset= newOffset;
    }

    /**
     * @see org.mortbay.util.Buffer#limit()
     */
    public int limit()
    {
        return _limit;
    }

    /**
     * @see org.mortbay.util.Buffer#limit(int)
     */
    public void limit(int newLimit)
    {
        if (!_mutable)
            Portable.throwIllegalState("immutable");
        if (newLimit > capacity())
            Portable.throwIllegalArgument("newLimit>capacity: " + newLimit + ">" + capacity());
        if (offset() > newLimit)
            Portable.throwIllegalArgument("offset>newLimit: " + offset() + ">" + newLimit);
        _limit= newLimit;
    }

    public int length()
    {
        return limit() - offset();
    }

    public int available()
    {
        return length();
    }

    public void compact()
    {
        int s= mark() >= 0 ? mark() : offset();
        if (s > 0)
        {
            Portable.arraycopy(array(), s, array(), 0, length());

            if (mark() > 0)
                mark(mark()-s);
            offset(offset()-s);
            limit(limit()-s);
        }
    }

    public byte peek()
    {
        return peek(_offset);
    }

    public abstract byte peek(int offset);
    public abstract Buffer peek(int offset, int length);

    public abstract void poke(int offset, byte b);
    public abstract void poke(int offset, Buffer src);

    public byte get()
    {
        byte b= peek(_offset);
        _offset++;
        return b;
    }

    public Buffer get(int length)
    {
        Buffer view= peek(_offset, length);
        _offset += length;
        return view;
    }

    public void put(byte b)
    {
        poke(_offset, b);
        _offset++;
    }

    public void put(Buffer src)
    {
        poke(_offset, src);
        _offset += src.length();
    }

    public int mark()
    {
        return _mark;
    }

    public void mark(int newMark)
    {
        _mark= newMark;
    }

    public void markOffset()
    {
        _mark= _offset;
    }

    public void markOffset(int offset)
    {
        _mark= _offset + offset;
    }

    public Buffer marked()
    {
        return marked(_offset - _mark);
    }

    public Buffer marked(int length)
    {
        if (_mark < 0)
            return null;
        Buffer view= peek(_mark, length);
        _mark= -1;
        return view;
    }

    public void rewind()
    {
        _offset= 0;
        _mark= -1;
    }


	/**
	 * @see org.mortbay.util.Buffer#immutable()
	 */
	public Buffer immutable()
	{
		if (_mutable)
		{
			byte[] bytes= new byte[limit() - offset()];
			Portable.arraycopy(array(), offset(), bytes, 0, bytes.length);
			ByteArrayBuffer view= new ByteArrayBuffer(array(), _offset, length(), !__MUTABLE);
			return view;
		}
		else
			return this;
	}

	/**
	 * @see org.mortbay.util.Buffer#isMutable()
	 */
	public boolean isMutable()
	{
		return _mutable;
	}

    protected Buffer subBuffer(int offset, int length)
    {
        if (_subBuffer == null)
            _subBuffer= new ByteArrayBuffer(array(), offset, length, __MUTABLE);
        else
        {
			_subBuffer.limit(offset + length);
            _subBuffer.offset(offset);
        }
        return _subBuffer;
    }

	public String toString()
	{
		return new String(array(), offset(), length());
	}
}
