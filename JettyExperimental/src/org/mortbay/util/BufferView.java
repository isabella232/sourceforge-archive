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
	

	public int position()
	{
		return _position;
	}

	public void position(int newPosition)
	{
		_position= newPosition;
	}

	public int available()
	{
		return _buffer.limit() - _position;
	}
	
	public void compact()
    {
		int s= _mark >= 0 ? _mark : _position;
		int length=_buffer.limit()-s;
		int offset= _buffer.offset()-s;
		if (offset<0)
		{
			_buffer.move(s,_buffer.offset(),length);
			if (_mark > 0)
				_mark+=offset;
			_position+=offset;
			_buffer.limit(_buffer.limit() + offset);
		}
    }
    
	public void rewind()
   	{
		_position= _buffer.offset();
		_mark= -1;
   	}
   	
	public int fill()
    {
    	return -1;
    }
    
    public int flush()
	{
		return -1;
	}

	public byte peek()
	{
		return _buffer.get(_position);
	}
	
	public Buffer peek(int length)
	{
		Buffer view=_buffer.get(_position,length);
		return view;
	}
	
    public byte get()
	{
		return _buffer.get(_position++);
	}
	
    public Buffer get(int length)
    {
    	Buffer view=_buffer.get(_position,length);
 		_position+=length;
 		return view;
    }
    
    public void put(byte b)
    {
    	_buffer.put(_position++,b);
    }
    
    public void put(Buffer src)
    {
    	_buffer.put(_position,src);
    	_position+=src.length();
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
		return marked(_position-_mark);
	}
	
    public Buffer marked(int length)
    {
		if (_mark < 0)
			return null;
		Buffer view = _buffer.get(_mark,length);
		_mark= -1;
		return view;
    }
    
 
}
