package org.mortbay.util;

/**
 * @author gregw
 *
 */
public class BufferView
{
    Buffer _buffer;
    int _mark;
    int _position;

    /**
     * Constructor for BufferView
     */
    public BufferView(Buffer buffer)
    {
        _buffer= buffer;
        _mark= -1;
        _position= buffer.offset();
    }

    public Buffer buffer()
    {
        return _buffer;
    }

    public int position()
    {
        return _position;
    }

    public void position(int newPosition)
    {
		if (newPosition < _buffer.offset())
			Portable.throwIllegalArgument("newPosition<offset(): " + newPosition + "<" + _buffer.offset());
		if (newPosition>_buffer.limit())
			Portable.throwIllegalArgument("newPosition>limit(): " + newPosition + ">" + _buffer.limit());
        _position= newPosition;
    }

    public int available()
    {
        return _buffer.limit() - _position;
    }

    public void compact()
    {
        int s= _mark >= 0 ? _mark : _position;
        if (s>_buffer.offset())
        {
        	_buffer.offset(s);
        	_buffer.compact();
			if (_mark > 0)
				_mark -= s;
			_position -= s;
        }
    }

    public void rewind()
    {
        _position= _buffer.offset();
        _mark= -1;
    }

    public int fill()
    {
    	// XXX should make compact conditional
    	compact();
        return _buffer.fill();
    }

    public int flush()
    {
        return _buffer.flush();
    }

    public byte peek()
    {
        return _buffer.get(_position);
    }

    public Buffer peek(int length)
    {
        Buffer view= _buffer.get(_position, length);
        return view;
    }

    public byte get()
    {
        return _buffer.get(_position++);
    }

    public Buffer get(int length)
    {
        Buffer view= _buffer.get(_position, length);
        _position += length;
        return view;
    }

    public void put(byte b)
    {
        _buffer.put(_position++, b);
    }

    public void put(Buffer src)
    {
        _buffer.put(_position, src);
        _position += src.length();
    }

    public int mark()
    {
        return _mark;
    }

    public void mark(int newMark)
    {
        _mark= newMark;
    }

    public void markPosition()
    {
        _mark= _position;
    }

    public void markPosition(int offset)
    {
        _mark= _position + offset;
    }

    public Buffer marked()
    {
        return marked(_position - _mark);
    }

    public Buffer marked(int length)
    {
        if (_mark < 0)
            return null;
        Buffer view= _buffer.get(_mark, length);
        _mark= -1;
        return view;
    }

}
