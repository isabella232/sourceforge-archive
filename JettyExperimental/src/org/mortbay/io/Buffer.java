package org.mortbay.io;


/**
 * Byte Buffer interface.
 * 
 * This is a byte buffer that is designed to work like a FIFO for bytes. Puts and Gets operate on different
 * pointers into the buffer and the valid content of the buffer is always between the getIndex and the putIndex.
 * 
 * This buffer interface is designed to be similar, but not dependant on the java.nio buffers, which may
 * be used to back an implementation of this Buffer. The main difference is that NIO buffer after a put have 
 * their valid content before the position and a flip is required to access that data.
 *
 * For this buffer it is always true that:
 *  markValue <= getIndex <= putIndex <= capacity
 *  @author gregw
 *
 * @version 1.0
 */
public interface Buffer
{

    /**
     *  Get the underlying array, if one exists.
     * @return a <code>byte[]</code> backing this buffer or null if none exists.
     */
    byte[] array();
    
    /**
     * 
     * @return a <code>byte[]</code> value of the bytes from the getIndex to the putIndex.
     */
    byte[] asArray();
    
    /**
     * 
     * @return a non volitile version of this <code>Buffer</code> value
     */
    Buffer asNonVolatile();

    /**
     *
     * @return a readonly version of this <code>Buffer</code>.
     */
    Buffer asReadOnlyBuffer();
    
    
    /**
     * 
     * The capacity of the buffer. This is the maximum putIndex that may be set.
     * @return an <code>int</code> value
     */
    int capacity();
    
    /**
     * the space remaining in the buffer.
     * @return capacity - putIndex
     */
    int space();
    
    /**
     * Clear the buffer. getIndex=0, putIndex=0.
     */
    void clear();

    /**
     * Compact the buffer by discarding bytes before the postion (or mark if set).
     * Bytes from the getIndex (or mark) to the putIndex are moved to the beginning of 
     * the buffer and the values adjusted accordingly.
     */
    void compact();
    
    /**
     * 
     * @return a <code>Buffer</code> duplicate.
     */
    Buffer duplicate();

    /**
     * Get the byte at the current getIndex and increment it.
     * @return The <code>byte</code> value from the current getIndex.
     */
    byte get();
    
    /**
     * Get bytes from the current postion and put them into the passed byte array.
     * The getIndex is incremented by the number of bytes copied into the array.
     * @param b The byte array to fill.
     * @param offset Offset in the array.
     * @param length The max number of bytes to read.
     * @return The number of bytes actually read.
     */
    int get(byte[] b, int offset, int length);

    /**
     * 
     * @param length an <code>int</code> value
     * @return a <code>Buffer</code> value
     */
    Buffer get(int length);

    /**
     * The index within the buffer that will next be read or written.
     * @return an <code>int</code> value >=0 <= putIndex()
     */
    int getIndex();
    
    /**
     * @return true of putIndex > getIndex
     */
    boolean hasContent();
    
    /**
     * 
     * @return a <code>boolean</code> value true if comparisons on this buffer are case sensitive.
     */
    boolean isCaseSensitive();

    /**
     * 
     * @return a <code>boolean</code> value true if the buffer is readonly
     */
    boolean isReadOnly();
    
    /**
     * 
     * @return a <code>boolean</code> value true if the buffer is expected to changed 
     * outside of the current scope. 
     */
    boolean isVolatile();

    /**
     * The number of bytes from the getIndex to the putIndex
     * @return an <code>int</code> == putIndex()-getIndex()
     */
    int length();
    
    /**
     * Set the mark to the current getIndex.
     */
    void mark();
    
    /**
     * Set the mark relative to the current getIndex
     * @param offset an <code>int</code> value to add to the current getIndex to obtain the mark value.
     */
    void mark(int offset);

    /**
     * The current index of the mark.
     * @return an <code>int</code> index in the buffer or -1 if the mark is not set.
     */
    int markIndex();

    /**
     * Get the byte at the current getIndex without incrementing the getIndex.
     * @return The <code>byte</code> value from the current getIndex.
     */
    byte peek();
  
    /**
     * Get the byte at a specific index in the buffer.
     * @param index an <code>int</code> value
     * @return a <code>byte</code> value
     */
    byte peek(int index);

    /**
     * 
     * @param index an <code>int</code> value
     * @param length an <code>int</code> value
     * @return The <code>Buffer</code> value from the requested getIndex.
     */
    Buffer peek(int index, int length);
    
    /**
     * Put the contents of the buffer at the specific index.
     * @param index an <code>int</code> value
     * @param src a <code>Buffer</code> value
     */
    void poke(int index, Buffer src);
    
    /**
     * Put a specific byte to a specific getIndex.
     * @param index an <code>int</code> value
     * @param b a <code>byte</code> value
     */
    void poke(int index, byte b);
    
    /**
     * Write the bytes from the source buffer to the current getIndex.
     * @param src a <code>Buffer</code> value
     */
    void put(Buffer src);

    /**
     * Put a byte to the current getIndex and increment the getIndex.
     * @param b a <code>byte</code> value
     */
    void put(byte b);

    /**
     * The index of the first element that should not be read.
     * @return an <code>int</code> value >= getIndex() 
     */
    int putIndex();
    
    /**
     * Reset the current getIndex to the mark 
     */
    void reset();
    
    /**
     * Set the buffers start getIndex.
     * @param newStart an <code>int</code> value
     */
    void setGetIndex(int newStart);
    
    /**
     * Set a specific value for the mark.
     * @param newMark an <code>int</code> value
     */
    void setMarkIndex(int newMark);
    
    /**
     * 
     * @param newLimit an <code>int</code> value
     */
    void setPutIndex(int newLimit);
    
    /**
     * Skip content. The getIndex is updated by min(remaining(), n)
     * @param n The number of bytes to skip
     * @return the number of bytes skipped.
     */
    int skip(int n);

    /**
     * 
     * @return a volitile <code>Buffer</code> from the postion to the putIndex.
     */
    Buffer slice();
    
    /**
     * 
     *
     * @return a volitile <code>Buffer</code> value from the mark to the putIndex
     */
    Buffer sliceFromMark();
    
    /**
     * 
     *
     * @param length an <code>int</code> value
     * @return a valitile <code>Buffer</code> value from the mark of the length requested.
     */
    Buffer sliceFromMark(int length);
    
    /**
     * 
     * @return a <code>String</code> value describing the state and contents of the buffer.
     */
    String toDetailString();
    
    
    public final static boolean READONLY= true;   
    public final static boolean READWRITE= false;
}
