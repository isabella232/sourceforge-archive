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
	private ByteArrayBuffer _view;

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

	private Buffer view(int offset, int length)
	{
		if (_view==null)
			_view=new ByteArrayBuffer(_bytes,offset,length,__MUTABLE);
		else
		{
			_view._offset=offset;
			_view._limit=offset+length;
		}
		return _view;
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
            throw new RuntimeException();

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
            throw new RuntimeException();
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
		if (offset>=capacity())
			Portable.throwIllegalArgument("offset>=capacity");
		return _bytes[offset];
	}
	
	/**
	 * @see org.mortbay.util.Buffer#get(int, int)
	 */
	public Buffer get(int offset, int length) 
	{
		if (offset+length>capacity())
			Portable.throwIllegalArgument("offset+length>capacity");
		return view(offset,length);
	}

	/** 
	 * @see org.mortbay.util.Buffer#put(int, byte)
	 */
	public void put(int offset, byte b) 
	{
		if (offset>=capacity())
			Portable.throwIllegalArgument("offset>=capacity");
		if (!_mutable)
			Portable.throwIllegalState("immutable");
		_bytes[offset]=b;
	}
	
	/** 
	 * @see org.mortbay.util.Buffer#put(int, org.mortbay.util.Buffer)
	 */
	public void put(int offset, Buffer src) 
	{
		if (!_mutable)
			Portable.throwIllegalState("immutable");
		if (offset+src.length()>capacity())
			Portable.throwIllegalArgument("offset+length>capacity");
		Portable.arraycopy(src.array(),src.offset(),_bytes,offset,src.length());
	}


	/**
	 * @see org.mortbay.util.Buffer#move(int, int, int)
	 */
	public void move(int offset, int newOffset, int length) 
	{
		if (!_mutable)
			Portable.throwIllegalState("immutable");
		if (offset>=capacity())
			Portable.throwIllegalArgument("offset>=capacity");
		if (newOffset+length>capacity())
			Portable.throwIllegalArgument("newOffset+length>capacity");
		if (length>0)
			Portable.arraycopy(_bytes,offset,_bytes,newOffset,length);
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

            ByteArrayBuffer view=
                new ByteArrayBuffer(_bytes, _offset, length(), !__MUTABLE);
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
		return new String(array(),offset(),length());
	}


}
