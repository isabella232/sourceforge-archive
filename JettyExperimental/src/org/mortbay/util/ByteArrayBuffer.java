package org.mortbay.util;

/**
 * @author gregw
 *
 */
public class ByteArrayBuffer implements Buffer
{
    protected final static boolean __MUTABLE= true;

    private byte[] _bytes;
    private int _offset;
    private int _limit;
    private boolean _mutable;
    private ByteArrayBuffer _subBuffer;

    public ByteArrayBuffer(byte[] bytes)
    {
        this(bytes, 0, bytes.length);
    }

    public ByteArrayBuffer(byte[] bytes, int offset, int length)
    {
        _bytes= bytes;
        _offset= offset;
        _limit= offset + length;
        _mutable= true;
    }

    public ByteArrayBuffer(byte[] bytes, int offset, int length, boolean mutable)
    {
        this(bytes, offset, length);
        _mutable= mutable;
    }

    private Buffer subBuffer(int offset, int length)
    {
        if (_subBuffer == null)
            _subBuffer= new ByteArrayBuffer(_bytes, offset, length, __MUTABLE);
        else
        {
            _subBuffer._offset= offset;
            _subBuffer._limit= offset + length;
        }
        return _subBuffer;
    }

    /**
     * @see org.mortbay.util.Buffer#array()
     */
    public byte[] array()
    {
        return _bytes;
    }

    /**
     * @see org.mortbay.util.Buffer#offset()
     */
    public int offset()
    {
        return _offset;
    }

    /**
     * @see org.mortbay.util.Buffer#offset(int)
     */
    public void offset(int newOffset)
    {
		if (!_mutable)
			Portable.throwIllegalState("immutable");
		if (newOffset < 0)
			Portable.throwIllegalArgument("newOffset<0 " + newOffset);
		if (newOffset>limit())
			Portable.throwIllegalArgument("newOffset>limit(): " + newOffset + ">" + limit());	

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
			Portable.throwIllegalArgument("newLimit>capacity(): " + newLimit + ">" + capacity());
		if (offset()>newLimit)
			Portable.throwIllegalArgument("offset()>newLimit: " + offset() + ">" + newLimit);	
        _limit= newLimit;
    }

    /**
     * @see org.mortbay.util.Buffer#length()
     */
    public int length()
    {
        return _limit - _offset;
    }

    /**
     * @see org.mortbay.util.Buffer#capacity()
     */
    public int capacity()
    {
        return _bytes.length;
    }

    /**
     * @see org.mortbay.util.Buffer#get(int)
     */
    public byte get(int offset)
    {
    	if (offset < offset())
			Portable.throwIllegalArgument("offset<offset(): " + offset + "<" + offset());
        if (offset >= limit())
            Portable.throwIllegalArgument("offset>=limit: " + offset + ">=" + limit());
        return _bytes[offset];
    }

    /**
     * @see org.mortbay.util.Buffer#get(int, int)
     */
    public Buffer get(int offset, int length)
    {
		if (offset < offset())
			Portable.throwIllegalArgument("offset<offset(): " + offset + "<" + offset());
        if (offset + length > limit())
            Portable.throwIllegalArgument("offset+length>limit: " + offset + "+" + length + ">" + limit());
        return subBuffer(offset, length);
    }

    /** 
     * @see org.mortbay.util.Buffer#put(int, byte)
     */
    public void put(int offset, byte b)
    {
		if (offset < offset())
			Portable.throwIllegalArgument("offset<offset(): " + offset + "<" + offset());
        if (offset >= limit())
            Portable.throwIllegalArgument("offset>=limit: " + offset + ">=" + limit());
        if (!_mutable)
            Portable.throwIllegalState("immutable");
        _bytes[offset]= b;
    }

    /** 
     * @see org.mortbay.util.Buffer#put(int, org.mortbay.util.Buffer)
     */
    public void put(int offset, Buffer src)
    {
        if (!_mutable)
            Portable.throwIllegalState("immutable");
		if (offset < offset())
			Portable.throwIllegalArgument("offset<offset(): " + offset + "<" + offset());
        if (offset + src.length() > limit())
            Portable.throwIllegalArgument("offset+length>limit: " + offset + "+" + src.length() + ">" + limit());
        Portable.arraycopy(src.array(), src.offset(), _bytes, offset, src.length());
    }

	/**
	 * @see org.mortbay.util.Buffer#clear()
	 */
	public void clear()
	{
		_offset=0;
		_limit=0;
	}

	/**
	 * @see org.mortbay.util.Buffer#compact()
	 */
	public void compact()
	{
		if (_offset>0)
		{
			Portable.arraycopy(_bytes, _offset, _bytes, 0, length());
			_limit-=_offset;
			_offset=0;
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
	
    /**
     * @see org.mortbay.util.Buffer#immutable()
     */
    public Buffer immutable()
    {
        if (_mutable)
        {
            byte[] bytes= new byte[_limit - _offset];
            Portable.arraycopy(_bytes, _offset, bytes, 0, bytes.length);

            ByteArrayBuffer view= new ByteArrayBuffer(_bytes, _offset, length(), !__MUTABLE);
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

    public String toString()
    {
        return new String(array(), offset(), length());
    }

}
