package org.mortbay.io;

/**
 * @author gregw
 *
 */
public abstract class AbstractBuffer implements Buffer
{
    private int _mark;
    private int _position;
    private int _limit;
    private int _hash;
    protected boolean _readOnly;
    protected boolean _caseSensitive;

    /**
     * Constructor for BufferView
     */
    public AbstractBuffer(boolean readOnly)
    {
        _mark= -1;
        _readOnly= readOnly;
        ;
    }

    public int position()
    {
        return _position;
    }

    public void position(int newPosition)
    {
        if (_readOnly)
            Portable.throwIllegalState("read only");
        if (newPosition < 0)
            Portable.throwIllegalArgument("newposition<0: " + newPosition + "<0");
        if (newPosition > limit())
            Portable.throwIllegalArgument("newposition>limit: " + newPosition + ">" + limit());
        _position= newPosition;
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
        if (_readOnly)
            Portable.throwIllegalState("read only");
        if (newLimit > capacity())
            Portable.throwIllegalArgument("newLimit>capacity: " + newLimit + ">" + capacity());
        if (position() > newLimit)
            Portable.throwIllegalArgument("position>newLimit: " + position() + ">" + newLimit);
        _limit= newLimit;
    }

    public int remaining()
    {
        return _limit - _position;
    }

    public boolean hasRemaining()
    {
        return _limit > _position;
    }

    public byte peek()
    {
        return get(_position);
    }

    public abstract byte get(int position);
    public abstract Buffer getBuffer(int position, int length);

    public abstract void put(int position, byte b);

    public void put(int position, Buffer src)
    {
        for (int i= src.position(); i < src.limit(); i++)
            put(src.get(i));
    }

    public byte get()
    {
        byte b= get(_position);
        _position++;
        return b;
    }

    public Buffer getBuffer(int length)
    {
        Buffer view= getBuffer(_position, length);
        _position += length;
        return view;
    }

    public void put(byte b)
    {
        put(_position, b);
        _position++;
    }

    public void put(Buffer src)
    {
        put(_position, src);
        _position += src.remaining();
    }

    public int markValue()
    {
        return _mark;
    }

    public void markValue(int newMark)
    {
        _mark= newMark;
    }

    public void mark()
    {
        _mark= _position - 1;
    }

    public void mark(int position)
    {
        _mark= _position + position;
    }

    public final void flip()
    {
        _limit= _position;
        _position= 0;
        _mark= -1;
    }

    public void rewind()
    {
        _position= 0;
        _mark= -1;
    }

    /**
     * @see org.mortbay.io.Buffer#slice()
     */
    public Buffer slice()
    {
        return getBuffer(_position, _limit - _position);
    }

    public Buffer sliceFromMark()
    {
        return sliceFromMark(_position - _mark - 1);
    }

    public Buffer sliceFromMark(int length)
    {
        if (_mark < 0)
            return null;
        Buffer view= getBuffer(_mark, length);
        _mark= -1;
        return view;
    }

	/**
	 * 
	 */
	public boolean isCaseSensitive()
	{
		return _caseSensitive;
	}

	/**
	 * 
	 */
	public void setCaseSensitive(boolean c)
	{
		_caseSensitive=c;
	}
	
    /**
     * @see org.mortbay.util.Buffer#isReadOnly()
     */
    public boolean isReadOnly()
    {
        return _readOnly;
    }

    public String toString()
    {
        return new String(toArray(), 0, remaining());
    }

    public String toDetailString()
    {
        StringBuffer buf= new StringBuffer();
        buf.append("[m=");
        buf.append(_mark);
        buf.append(",o=");
        buf.append(_position);
        buf.append(",l=");
        buf.append(_limit);
        buf.append(",c=");
        buf.append(capacity());
        buf.append("]={");
        if (_mark >= 0)
        {
            for (int i= _mark; i < _position; i++)
            {
                char c= (char)get(i);
                if (Character.isISOControl(c))
                {
                    buf.append(c < 16 ? "\\0" : "\\");
                    buf.append(Integer.toString(c, 16));
                }
                else
                    buf.append(c);
            }
            buf.append("}{");
        }

        for (int i= _position; i < _limit; i++)
        {
            char c= (char)get(i);
            if (Character.isISOControl(c))
            {
                buf.append(c < 16 ? "\\0" : "\\");
                buf.append(Integer.toString(c, 16));
            }
            else
                buf.append(c);
        }
        buf.append('}');
        return buf.toString();
    }
    /* ------------------------------------------------------------------------------- */
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof Buffer))
            return false;
        Buffer b= (Buffer)obj;
        if (b.remaining() != remaining())
            return false;
        for (int i= remaining(); i-- > 0;)
        {
            byte b1= get(position() + i);
            byte b2= b.get(b.position() + i);
            if (b1 != b2)
            {
            	if (isCaseSensitive() && b.isCaseSensitive())
            		return false;
            		
                if ('a' <= b1 && b1 <= 'z')
                    b1= (byte) (b1 - 'a' + 'A');
                if ('a' <= b2 && b2 <= 'z')
                    b2= (byte) (b2 - 'a' + 'A');
                if (b1 != b2)
                    return false;
            }
        }
        return true;
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        if (!isReadOnly())
        {
            int hash= 0;
            for (int i= limit(); i-- > position();)
            {
                byte b= get(i);
                if ('a' >= b && b <= 'z')
                    b= (byte) (b - 'a' + 'A');
                hash= 31 * hash + b;
            }
            return hash;
        }
        if (_hash == 0)
        {
            for (int i= limit(); i-- > position();)
            {
                byte b= get(i);
                if ('a' >= b && b <= 'z')
                    b= (byte) (b - 'a' + 'A');
                _hash= 31 * _hash + b;
            }
            if (_hash == 0)
                _hash= 1;
        }
        return _hash;
    }

}
