package org.mortbay.io;

/**
 * @author gregw
 *
 */
public abstract class AbstractBuffer implements Buffer
{

    /**
     * Constructor for BufferView
     */
    public AbstractBuffer(boolean readOnly)
    {
        _mark= -1;
        _readOnly= readOnly;
    }

    public void compact()
    {
        int s= markIndex() >= 0 ? markIndex() : getIndex();
        if (s > 0)
        {
            byte array[]=array();
            int length=putIndex()-s;
            
            if (length>0)
            {
                if (array!=null)
                    Portable.arraycopy(array(), s, array(), 0, putIndex() - s);
                else
                {
                    for (int i=length; i-->0;)
                        poke(i,peek(s+i));
                }
            }
            
            if (markIndex() > 0)
                setMarkIndex(markIndex() - s);
            setGetIndex(getIndex() - s);
            setPutIndex(putIndex() - s);
        }
    }
    
    public void clear()
    {
        setGetIndex(0);
        setPutIndex(0);
    }
    
    public boolean equals(Object obj)
    {
        // reject non buffers;
        if (obj == null || !(obj instanceof Buffer))
            return false;
        Buffer b= (Buffer)obj;
        
        // reject different lengths
        if (b.length() != length())
            return false;
        
        // reject AbstractBuffer with different hash value
        if (_hash!=0 && obj instanceof AbstractBuffer)
        {
            AbstractBuffer ab = (AbstractBuffer)obj;
            if (ab._hash!=0 && _hash!=ab._hash)
                return false;
        }
            
        // Nothing for it but to do the hard grind.
        for (int i= length(); i-- > 0;)
        {
            byte b1= peek(getIndex() + i);
            byte b2= b.peek(b.getIndex() + i);
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

    public byte get()
    {
        byte b= peek(_get);
        _get++;
        return b;
    }

    public Buffer get(int length)
    {
        Buffer view= peek(_get, length);
        _get += length;
        return view;
    }

    public int getIndex()
    {
        return _get;
    }
    
    public boolean hasContent()
    {
        return _put > _get;
    }
    
    private int hash()
    {
        int hash= 0;
        for (int i= putIndex(); i--> getIndex();)
        {
            byte b= peek(i);
            if (!isCaseSensitive() && 'a' >= b && b <= 'z')
                b= (byte) (b - 'a' + 'A');
            hash= 31 * hash + b;
        }
        if (hash == 0)
            hash= -1;
        return hash;
    }
    
    public int hashCode()
    {
        if (!isReadOnly())
            return hash();
        
        if (_hash == 0)
            _hash=hash();
            
        return _hash;
    }

	public boolean isCaseSensitive()
	{
		return _caseSensitive;
	}
	
    public boolean isReadOnly()
    {
        return _readOnly;
    }

    public int length()
    {
        return _put - _get;
    }

    public void mark()
    {
        _mark= _get - 1;
    }

    public void mark(int offset)
    {
        _mark= _get + offset;
    }

    public int markIndex()
    {
        return _mark;
    }

    public byte peek()
    {
        return peek(_get);
    }

    public void poke(int index, Buffer src)
    {
        if (isReadOnly())
            Portable.throwIllegalState("read only");
        if (index < 0)
            Portable.throwIllegalArgument("index<0: " + index + "<0");
        if (index + src.length() > capacity())
            Portable.throwIllegalArgument(
                "index+length>capacity(): "
                    + index
                    + "+"
                    + src.length()
                    + ">"
                    + capacity());
        
        byte src_array[]=src.array();
        byte dst_array[]=array();
        if (src_array!=null && dst_array!=null)
            Portable.arraycopy(src_array, src.getIndex(), dst_array, index, src.length());
        else if (src_array!=null)
        {
            for (int i= src.getIndex(); i < src.putIndex(); i++)
                poke(index++,src_array[i]);
        }
        else if (dst_array!=null)
        {
            for (int i= src.getIndex(); i < src.putIndex(); i++)
                dst_array[index++]=src.peek(i);
        }
        else
        {
            for (int i= src.getIndex(); i < src.putIndex(); i++)
                poke(index++,src.peek(i));
        }
    }   

    public void put(Buffer src)
    {
        poke(_put, src);
        _put += src.length();
    }

    public void put(byte b)
    {
        poke(_put, b);
        _put++;
    }

    public int putIndex()
    {
        return _put;
    }

    public void reset()
    {
        if (_mark>=0)
            _get= _mark;
    }

    public void rewind()
    {
        _get= 0;
        _mark= -1;
    }

	public void setCaseSensitive(boolean c)
	{
		_caseSensitive=c;
	}

    public void setGetIndex(int newPosition)
    {
        if (_readOnly)
            Portable.throwIllegalState("read only");
        if (newPosition < 0)
            Portable.throwIllegalArgument("newposition<0: " + newPosition + "<0");
        if (newPosition > putIndex())
            Portable.throwIllegalArgument("newposition>limit: " + newPosition + ">" + putIndex());
        _get= newPosition;
    }

    public void setMarkIndex(int newMark)
    {
        _mark= newMark;
    }

    /**
     * @see org.mortbay.util.Buffer#limit(int)
     */
    public void setPutIndex(int newLimit)
    {
        if (_readOnly)
            Portable.throwIllegalState("read only");
        if (newLimit > capacity())
            Portable.throwIllegalArgument("newLimit>capacity: " + newLimit + ">" + capacity());
        if (getIndex() > newLimit)
            Portable.throwIllegalArgument("position>newLimit: " + getIndex() + ">" + newLimit);
        _put= newLimit;
    }
    
    public int skip(int n)
    {
        if (length()<n)
            n=length();
        _get+=n;
        return n;
    }

    public Buffer slice()
    {
        return peek(_get, _put - _get);
    }

    public Buffer sliceFromMark()
    {
        return sliceFromMark(_get - _mark - 1);
    }

    public Buffer sliceFromMark(int length)
    {
        if (_mark < 0)
            return null;
        Buffer view= peek(_mark, length);
        _mark= -1;
        return view;
    }

    public String toDetailString()
    {
        StringBuffer buf= new StringBuffer();
        buf.append("[m=");
        buf.append(_mark);
        buf.append(",g=");
        buf.append(_get);
        buf.append(",p=");
        buf.append(_put);
        buf.append(",c=");
        buf.append(capacity());
        buf.append("]={");
        if (_mark >= 0)
        {
            for (int i= _mark; i < _get; i++)
            {
                char c= (char)peek(i);
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

        int count=0;
        for (int i= _get; i < _put; i++)
        {
            char c= (char)peek(i);
            if (Character.isISOControl(c))
            {
                buf.append(c < 16 ? "\\0" : "\\");
                buf.append(Integer.toString(c, 16));
            }
            else
                buf.append(c);
            
            if (count++ == 32)
            {
                if (_put-i>8)
                {
                    buf.append(" ... ");
                    i=_put-8;
                }
            }
        }
        buf.append('}');
        return buf.toString();
    }

    public String toString()
    {
        return new String(asArray(), 0, length());
    }
    
    
    protected boolean _caseSensitive;
    private int _get;
    private int _hash;
    private int _mark;
    private int _put;
    protected boolean _readOnly;
}
