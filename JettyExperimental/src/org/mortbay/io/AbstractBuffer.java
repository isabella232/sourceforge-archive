// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.io;

/**
 * @author gregw
 *  
 */
public abstract class AbstractBuffer implements Buffer
{

    protected final static String __IMMUTABLE = "IMMUTABLE", __READONLY = "READONLY",
            __READWRITE = "READWRITE", __VOLATILE = "VOLATILE";

    protected int _access;
    protected boolean _volatile;

    private boolean _caseSensitive;
    private int _get;
    private int _hash;
    private int _mark;
    private int _put;
    protected String _string;
    private View _view;

    /**
     * Constructor for BufferView
     * 
     * @param access 0==IMMUTABLE, 1==READONLY, 2==READWRITE
     */
    public AbstractBuffer(int access, boolean isVolatile)
    {
        if (access == IMMUTABLE && isVolatile)
                Portable.throwIllegalArgument("IMMUTABLE && VOLATILE");
        setMarkIndex(-1);
        _access = access;
        _volatile = isVolatile;
    }

    /*
     * @see org.mortbay.io.Buffer#toArray()
     */
    public byte[] asArray()
    {
        byte[] bytes = new byte[length()];
        byte[] array = array();
        if (array != null)
            Portable.arraycopy(array, getIndex(), bytes, 0, bytes.length);
        else
            peek(getIndex(), bytes, 0, length());
        return bytes;
    }

    /*
     * @see org.mortbay.io.Buffer#asNonVolatile()
     */
    public Buffer asNonVolatileBuffer()
    {
        if (!isVolatile()) return this;
        return new ByteArrayBuffer(asArray(), 0, length(), _access, NON_VOLATILE);
    }

    public Buffer asImmutableBuffer()
    {
        if (isImmutable()) return this;
        return new ByteArrayBuffer(asArray(), 0, length(), IMMUTABLE);
    }

    /*
     * @see org.mortbay.util.Buffer#asReadOnlyBuffer()
     */
    public Buffer asReadOnlyBuffer()
    {
        if (isReadOnly()) return this;
        return new View(this, markIndex(), getIndex(), putIndex(), READONLY);
    }

    public Buffer asMutableBuffer()
    {
        if (!isImmutable()) return this;
        
        Buffer b=this.buffer();
        if (b.isReadOnly())
            return new ByteArrayBuffer(asArray(), 0, length(), READWRITE);
        return new View(b, markIndex(), getIndex(), putIndex(), _access);
    }

    public Buffer buffer()
    {
        return this;
    }

    public void clear()
    {
        setGetIndex(0);
        setPutIndex(0);
    }

    public void compact()
    {
        if (isReadOnly()) Portable.throwIllegalState(__READONLY);
        int s = markIndex() >= 0 ? markIndex() : getIndex();
        if (s > 0)
        {
            byte array[] = array();
            int length = putIndex() - s;
            if (length > 0)
            {
                if (array != null)
                    Portable.arraycopy(array(), s, array(), 0, length);
                else
                {
                    poke(0, peek(s, length));
                    // for (int i = length; i-- > 0;)
                    //     poke(i, peek(s + i));
                }
            }
            if (markIndex() > 0) setMarkIndex(markIndex() - s);
            setGetIndex(getIndex() - s);
            setPutIndex(putIndex() - s);
        }
    }


    public boolean equals(Object obj)
    {
        // reject non buffers;
        if (obj == null || !(obj instanceof Buffer)) return false;
        Buffer b = (Buffer) obj;

        // reject different lengths
        if (b.length() != length()) return false;

        // reject AbstractBuffer with different hash value
        if (_hash != 0 && obj instanceof AbstractBuffer)
        {
            AbstractBuffer ab = (AbstractBuffer) obj;
            if (ab._hash != 0 && _hash != ab._hash) return false;
        }

        // Nothing for it but to do the hard grind.
        for (int i = length(); i-- > 0;)
        {
            byte b1 = peek(getIndex() + i);
            byte b2 = b.peek(b.getIndex() + i);
            if (b1 != b2)
            {
                if (isCaseSensitive() && b.isCaseSensitive()) return false;
                if ('a' <= b1 && b1 <= 'z') b1 = (byte) (b1 - 'a' + 'A');
                if ('a' <= b2 && b2 <= 'z') b2 = (byte) (b2 - 'a' + 'A');
                if (b1 != b2) return false;
            }
        }
        return true;
    }

    public byte get()
    {
        int gi = getIndex();
        byte b = peek(gi);
        setGetIndex(gi + 1);
        return b;
    }

    public int get(byte[] b, int offset, int length)
    {
        int gi = getIndex();
        length = peek(gi, b, offset, length);
        setGetIndex(gi + length);
        return length;
    }

    public Buffer get(int length)
    {
        int gi = getIndex();
        Buffer view = peek(gi, length);
        setGetIndex(gi + length);
        return view;
    }

    public int getIndex()
    {
        return _get;
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
            if (!isCaseSensitive() && 'a' >= b && b <= 'z') b = (byte) (b - 'a' + 'A');
            hash = 31 * hash + b;
        }
        if (hash == 0) hash = -1;
        return hash;
    }

    public int hashCode()
    {
        if (!isImmutable()) return hash();
        if (_hash == 0) _hash = hash();
        return _hash;
    }

    public boolean isCaseSensitive()
    {
        return _caseSensitive;
    }

    public boolean isImmutable()
    {
        return _access <= IMMUTABLE;
    }

    public boolean isReadOnly()
    {
        return _access <= READONLY;
    }

    public boolean isVolatile()
    {
        return _volatile;
    }

    public int length()
    {
        return putIndex() - getIndex();
    }

    public void mark()
    {
        setMarkIndex(getIndex() - 1);
    }

    public void mark(int offset)
    {
        setMarkIndex(getIndex() + offset);
    }

    public int markIndex()
    {
        return _mark;
    }

    public byte peek()
    {
        return peek(getIndex());
    }

    public Buffer peek(int index, int length)
    {
        if (_view == null)
        {
            _view = new View(this, -1, index, index + length, isReadOnly() ? READONLY : READWRITE);
        }
        else
        {
            _view.setMarkIndex(-1);
            _view.setGetIndex(0);
            _view.setPutIndex(index + length);
            _view.setGetIndex(index);
        }
        return _view;
    }

    public void poke(int index, Buffer src)
    {
        if (isReadOnly()) Portable.throwIllegalState(__READONLY);
        if (index < 0) Portable.throwIllegalArgument("index<0: " + index + "<0");
        if (index + src.length() > capacity())
                Portable.throwIllegalArgument("index+length>capacity(): " + index + "+"
                        + src.length() + ">" + capacity());
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
        if (isReadOnly()) Portable.throwIllegalState(__READONLY);
        if (index < 0) Portable.throwIllegalArgument("index<0: " + index + "<0");
        if (index + length > capacity())
                Portable.throwIllegalArgument("index+length>capacity(): " + index + "+" + length
                        + ">" + capacity());
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
        int pi = putIndex();
        poke(pi, src);
        setPutIndex(pi + src.length());
    }

    public void put(byte b)
    {
        int pi = putIndex();
        poke(pi, b);
        setPutIndex(pi + 1);
    }

    public void put(byte[] b, int offset, int length)
    {
        int pi = putIndex();
        poke(pi, b, offset, length);
        setPutIndex(pi + length);
    }

    public int putIndex()
    {
        return _put;
    }

    public void reset()
    {
        if (markIndex() >= 0) setGetIndex(markIndex());
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

    public void setGetIndex(int getIndex)
    {
        if (isImmutable()) Portable.throwIllegalState(__IMMUTABLE);
        if (getIndex < 0) Portable.throwIllegalArgument("getIndex<0: " + getIndex + "<0");
        if (getIndex > putIndex())
                Portable.throwIllegalArgument("getIndex>putIndex: " + getIndex + ">" + putIndex());
        _get = getIndex;
    }

    public void setMarkIndex(int index)
    {
        if (index>=0 && isImmutable()) Portable.throwIllegalState(__IMMUTABLE);
        _mark = index;
    }

    public void setPutIndex(int putIndex)
    {
        if (isImmutable()) Portable.throwIllegalState(__IMMUTABLE);
        if (putIndex > capacity())
                Portable.throwIllegalArgument("putIndex>capacity: " + putIndex + ">" + capacity());
        if (getIndex() > putIndex)
                Portable.throwIllegalArgument("getIndex>putIndex: " + getIndex() + ">" + putIndex);
        _put = putIndex;
    }

    public int skip(int n)
    {
        if (length() < n) n = length();
        setGetIndex(getIndex() + n);
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
        if (markIndex() < 0) return null;
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
        if (isImmutable())
        {
            if (_string == null) _string = new String(asArray(), 0, length());
            return _string;
        }
        return new String(asArray(), 0, length());
    }
}
