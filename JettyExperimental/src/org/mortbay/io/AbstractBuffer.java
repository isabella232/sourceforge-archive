package org.mortbay.io;

/**
 * @author gregw
 *  
 */
public abstract class AbstractBuffer implements Buffer
{
  protected boolean _caseSensitive;
  private int _hash;
  protected boolean _readOnly;

  /**
   * Constructor for BufferView
   */
  public AbstractBuffer(boolean readOnly)
  {
    setMarkIndex(-1);
    _readOnly = readOnly;
  }

  /*
   * @see org.mortbay.io.Buffer#toArray()
   */
  public byte[] asArray()
  {
    byte[] bytes = new byte[putIndex() - getIndex()];
    byte[] array = array();
    if (array!=null)
      Portable.arraycopy(array, getIndex(), bytes, 0, bytes.length);
    else
    {
      int p=0;
      for (int i=getIndex() ; i < putIndex() ; i++ )
        bytes[p++]=peek(i);
    }
    return bytes;
  }

  public void clear()
  {
    setGetIndex(0);
    setPutIndex(0);
  }

  public void compact()
  {
    int s = markIndex() >= 0 ? markIndex() : getIndex();
    if (s > 0)
    {
      byte array[] = array();
      int length = putIndex() - s;
      if (length > 0)
      {
        if (array != null)
          Portable.arraycopy(array(), s, array(), 0, putIndex() - s);
        else
        {
          for (int i = length; i-- > 0;)
            poke(i, peek(s + i));
        }
      }
      if (markIndex() > 0) 
      setMarkIndex(markIndex() - s);
      setGetIndex(getIndex() - s);
      setPutIndex(putIndex() - s);
    }
  }

  public boolean equals(Object obj)
  {
    // reject non buffers;
    if (obj == null || !(obj instanceof Buffer)) 
    return false;
    Buffer b = (Buffer) obj;

    // reject different lengths
    if (b.length() != length()) 
    return false;

    // reject AbstractBuffer with different hash value
    if (_hash != 0 && obj instanceof AbstractBuffer)
    {
      AbstractBuffer ab = (AbstractBuffer) obj;
      if (ab._hash != 0 && _hash != ab._hash) 
      return false;
    }

    // Nothing for it but to do the hard grind.
    for (int i = length(); i-- > 0;)
    {
      byte b1 = peek(getIndex() + i);
      byte b2 = b.peek(b.getIndex() + i);
      if (b1 != b2)
      {
        if (isCaseSensitive() && b.isCaseSensitive()) 
        return false;
        if ('a' <= b1 && b1 <= 'z') 
        b1 = (byte) (b1 - 'a' + 'A');
        if ('a' <= b2 && b2 <= 'z') 
        b2 = (byte) (b2 - 'a' + 'A');
        if (b1 != b2) 
        return false;
      }
    }
    return true;
  }

  public byte get()
  {
    int gi=getIndex();
    byte b = peek(gi);
    setGetIndex(gi+1);
    return b;
  }

  public Buffer get(int length)
  {
    int gi=getIndex();
    Buffer view = peek(gi, length);
    setGetIndex(gi+length);
    return view;
  }


  public boolean hasContent()
  {
    return putIndex() > getIndex();
  }

  private int hash()
  {
    int hash = 0;
    for (int i = putIndex(); i-- > getIndex();)
    {
      byte b = peek(i);
      if (!isCaseSensitive() && 'a' >= b && b <= 'z') 
      b = (byte) (b - 'a' + 'A');
      hash = 31 * hash + b;
    }
    if (hash == 0) 
    hash = -1;
    return hash;
  }

  public int hashCode()
  {
    if (!isReadOnly()) 
    return hash();
    if (_hash == 0) 
    _hash = hash();
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
    return putIndex() - getIndex();
  }

  public void mark()
  {
    setMarkIndex(getIndex()-1);
  }

  public void mark(int offset)
  {
    setMarkIndex(getIndex()+offset);
  }

  public byte peek()
  {
    return peek(getIndex());
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
    byte[] src_array = src.array();
    byte[] dst_array = array();
    if (src_array != null && dst_array != null)
      Portable.arraycopy(src_array, src.getIndex(), dst_array, index, src.length());
    else if (src_array != null)
    {
      for (int i = src.getIndex(); i < src.putIndex(); i++)
        poke(index++, src_array[i]);
    }
    else if (dst_array != null)
    {
      for (int i = src.getIndex(); i < src.putIndex(); i++)
        dst_array[index++] = src.peek(i);
    }
    else
    {
      for (int i = src.getIndex(); i < src.putIndex(); i++)
        poke(index++, src.peek(i));
    }
  }

  public void poke(int index, byte[] b, int offset, int length)
  {
    if (isReadOnly()) 
    Portable.throwIllegalState("read only");
    if (index < 0) 
    Portable.throwIllegalArgument("index<0: " + index + "<0");
    if (index + length > capacity()) 
    Portable.throwIllegalArgument(
    "index+length>capacity(): "
    + index
    + "+"
    + length
    + ">"
    + capacity());
    byte[] dst_array = array();
    if (dst_array != null)
      Portable.arraycopy(b, offset, dst_array, index, length);
    else
    {
      for (int i = 0; i < length; i++)
        poke(index++, b[offset + i]);
    }
  }

  public void put(Buffer src)
  {
    int pi=putIndex();
    poke(pi, src);
    setPutIndex(pi+src.length());
  }

  public void put(byte b)
  {
    int pi=putIndex();
    poke(pi,b);
    setPutIndex(pi+1);
  }

  public void put(byte[] b, int offset, int length)
  {
    int pi=putIndex();
    poke(pi, b, offset, length);
    setPutIndex(pi+length);
  }

  public void reset()
  {
    if (markIndex()>=0)
      setGetIndex(markIndex());
  }

  public void rewind()
  {
    setGetIndex(0);
    setMarkIndex(-1);
  }

  public void setCaseSensitive(boolean c)
  {
    _caseSensitive = c;
  }


  public int skip(int n)
  {
    if (length() < n) 
    n = length();
    setGetIndex(getIndex()+n);
    return n;
  }

  public Buffer slice()
  {
    return peek(getIndex(), length());
  }

  public Buffer sliceFromMark()
  {
    return sliceFromMark(getIndex() - markIndex() - 1);
  }

  public Buffer sliceFromMark(int length)
  {
    if (markIndex() < 0) 
      return null;
    Buffer view = peek(markIndex(), length);
    setMarkIndex(-1);
    return view;
  }

  public int space()
  {
    return capacity() - putIndex();
  }

  public String toDetailString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    buf.append(super.hashCode());
    buf.append(",");
    buf.append(this.array().hashCode());
    buf.append(",m=");
    buf.append(markIndex());
    buf.append(",g=");
    buf.append(getIndex());
    buf.append(",p=");
    buf.append(putIndex());
    buf.append(",c=");
    buf.append(capacity());
    buf.append("]={");
    if (markIndex() >= 0)
    {
      for (int i = markIndex(); i < getIndex(); i++)
      {
        char c = (char) peek(i);
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
    int count = 0;
    for (int i = getIndex(); i < putIndex(); i++)
    {
      char c = (char) peek(i);
      if (Character.isISOControl(c))
      {
        buf.append(c < 16 ? "\\0" : "\\");
        buf.append(Integer.toString(c, 16));
      }
      else
        buf.append(c);
      if (count++ == 50)
      {
        if (putIndex() - i > 20)
        {
          buf.append(" ... ");
          i = putIndex() - 20;
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
}
