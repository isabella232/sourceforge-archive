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

    /**
     * @return max limit
     */
    int capacity();
   
	byte get(int offset);
	Buffer get(int offset, int length);
    
	void put(int offset, byte b);
	void put(int offset, Buffer src);
   
    void move(int offset, int newOffset, int length);
    
     boolean isMutable();
     Buffer immutable();
}
