package org.mortbay.util;
/**
 * @author gregw
 *
 */
public interface Buffer
{
    public final static boolean MUTABLE= true;
    byte[] array();
       
    int offset();
    void offset(int newOffset);
    
    int limit();
    void limit(int newLimit);
    
    /**
     * @return limit-offset
     */
    int length();
	int available();

    /**
     * @return max limit
     */
    int capacity();

	byte peek();
	byte peek(int offset);
	Buffer peek(int offset, int length);
	
	void poke(int offset,byte b);
	void poke(int offset, Buffer src);
	
	byte get();
	Buffer get(int length);

	void put(byte b);
	void put(Buffer src);
	

	public int mark();
	public void mark(int newMark);
	public void markOffset();
	public void markOffset(int offset);
	public Buffer marked();
	public Buffer marked(int length);
	
	int fill();
	int flush();
	void clear();
	void rewind();
	void compact();
    
     boolean isMutable();
     Buffer immutable();
}
