package org.mortbay.io;
/** Buffer interface.
 * This buffer interface is designed to be similar, but not dependant on the java.nio buffers.
 * This is for portability between JVMs and also to add the concepts of fill and flush have been added.
 * @author gregw
 *
 */
public interface Buffer
{
    public final static boolean READONLY= true;
	public final static boolean READWRITE=false;
	
    byte[] toArray();
       
    int position();
    void position(int newPosition);
    
    int limit();
    void limit(int newLimit);
    
    int remaining();
    boolean hasRemaining();
    int capacity();

	byte peek();
	
	byte get();
	byte get(int index);
	
	Buffer getBuffer(int length);
	Buffer getBuffer(int index,int length);

	void put(byte b);
	void put(Buffer src);
	void put(int index,byte b);
	void put(int index,Buffer src);
	
	int markValue();
	void markValue(int newMark);
	void mark();
	void mark(int position);
	
	int fill();
	int flush();
	void clear();
	void rewind();
	void flip();
	void compact();
    
     boolean isReadOnly();
     boolean isVolatile();
     boolean isCaseSensitive();
     
     
     Buffer slice();
	 Buffer sliceFromMark();
	 Buffer sliceFromMark(int length);
	 Buffer duplicate();
	 
     Buffer asReadOnlyBuffer();
     Buffer asNonVolatile();
     
     String toDetailString();
}
