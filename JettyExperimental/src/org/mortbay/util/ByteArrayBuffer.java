package org.mortbay.util;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class ByteArrayBuffer extends AbstractBuffer
{

    private byte[] _bytes;

	public ByteArrayBuffer(byte[] bytes, int offset, int length, boolean mutable)
	{
		super(mutable);
		_bytes= bytes;
		offset(offset);
		limit(offset+length);
	}
	
    public ByteArrayBuffer(byte[] bytes)
    {
        this(bytes, 0, bytes.length,MUTABLE);
    }

    public ByteArrayBuffer(byte[] bytes, int offset, int length)
    {
    	this(bytes,offset,length,MUTABLE);
    }

	/* ------------------------------------------------------------------------------- */
	/** Constructor.
	 * @param value
	 */
	public ByteArrayBuffer(String value)
	{
		super(!MUTABLE);
		_bytes = Portable.getBytes(value);
		offset(0);
		limit(_bytes.length);
	}

	public void mimic(Buffer buffer)
	{
		offset(0);
		_bytes=buffer.array();
		limit(buffer.limit());
		offset(buffer.offset());
		mark(buffer.mark());
	}

    /**
     * @see org.mortbay.util.Buffer#array()
     */
    public byte[] array()
    {
        return _bytes;
    }

    /**
     * @see org.mortbay.util.Buffer#capacity()
     */
    public int capacity()
    {
    	if (_bytes==null)
    	   return 0;	
        return _bytes.length;
    }

    /**
     * @see org.mortbay.util.Buffer#get(int)
     */
    public byte peek(int offset)
    {
		if (offset < 0)
			Portable.throwIllegalArgument("offset<0: " + offset + "<0");
		if (offset > capacity())
			Portable.throwIllegalArgument("offset>capacity(): " + offset + ">" + capacity());
        return _bytes[offset];
    }

    /**
     * @see org.mortbay.util.Buffer#get(int, int)
     */
    public Buffer peek(int offset, int length)
    {
		if (offset < 0)
			Portable.throwIllegalArgument("offset<0: " + offset + "<0");
        if (offset + length > capacity())
            Portable.throwIllegalArgument("offset+length>capacity(): " + offset + "+" + length + ">" + capacity());
        return subBuffer(offset, length);
    }

    /** 
     * @see org.mortbay.util.Buffer#put(int, byte)
     */
    public void poke(int offset, byte b)
    {
		if (!isMutable())
			Portable.throwIllegalState("immutable");
			
		if (offset < 0)
			Portable.throwIllegalArgument("offset<0: " + offset + "<0");
		if (offset > capacity())
			Portable.throwIllegalArgument("offset>capacity(): " + offset + ">" + capacity());
			
        _bytes[offset]= b;
    }

    /** 
     * @see org.mortbay.util.Buffer#put(int, org.mortbay.util.Buffer)
     */
    public void poke(int offset, Buffer src)
    {
        if (!isMutable())
            Portable.throwIllegalState("immutable");
		if (offset < 0)
			Portable.throwIllegalArgument("offset<0: " + offset + "<0");
		if (offset + src.length() > capacity())
			Portable.throwIllegalArgument("offset+length>capacity(): " + offset + "+" + src.length() + ">" + capacity());
        Portable.arraycopy(src.array(), src.offset(), _bytes, offset, src.length());
    }

	/**
	 * @see org.mortbay.util.Buffer#clear()
	 */
	public void clear()
	{
		offset(0);
		limit(0);
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
	

}
