package org.mortbay.io;
/**
 * Buffer interface.
 * This buffer interface is designed to be similar, but not dependant on the java.nio buffers.
 * This is for portability between JVMs and also to add the concepts of fill and flush have been added.
 *
 * For this buffer it is always true that:
 *  markValue <= position <= limit 
 *  @author gregw
 *
 * @version 1.0
 */
public interface Buffer
{
    public final static boolean READONLY= true;   
    public final static boolean READWRITE= false;

    /**
     * The index within the buffer that will next be read or written.
     * @return an <code>int</code> value >=0 <= limit()
     */
    int position();
    
    /**
     * Set the buffers position.
     * @param newPosition an <code>int</code> value
     */
    void position(int newPosition);

    /**
     * The index of the first element that should not be read.
     * @return an <code>int</code> value >= position() 
     */
    int limit();
    
    /**
     * 
     * @param newLimit an <code>int</code> value
     */
    void limit(int newLimit);

    /**
     * The bytes remaining to be read or written in this buffer.
     * @return an <code>int</code> == limit()-position()
     */
    int remaining();
    
    /**
     * True if bytes remain to be read or written.
     * @return a <code>boolean</code> value true iff remaining()>0
     */
    boolean hasRemaining();
    
    /**
     * 
     * The capacity of the buffer. This is the maximum limit that may be set.
     * @return an <code>int</code> value
     */
    int capacity();

    /**
     * Get the byte at the current position without incrementing the position.
     * @return The <code>byte</code> value from the current position.
     */
    byte peek();

    /**
     * Get the byte at the current positon and increment the position.
     * @return The <code>byte</code> value from the current position.
     */
    byte get();
    
    /**
     * Get bytes from the current postion and put them into the passed byte array.
     * The position is incremented by the number of bytes copied into the array.
     * @param b The byte array to fill.
     * @param offset Offset in the array.
     * @param length The max number of bytes to read.
     * @return The number of bytes actually read.
     */
    int get(byte[] b, int offset, int length);
  
    /**
     * Get the byte at a specific index in the buffer.
     * @param index an <code>int</code> value
     * @return a <code>byte</code> value
     */
    byte get(int index);

    /**
     * 
     * @param length an <code>int</code> value
     * @return a <code>Buffer</code> value
     */
    Buffer getBuffer(int length);
    
    /**
     * 
     * @param index an <code>int</code> value
     * @param length an <code>int</code> value
     * @return The <code>Buffer</code> value from the requested position.
     */
    Buffer getBuffer(int index, int length);

    /**
     * Skip content. The position is updated by min(remaining(), n)
     * @param n The number of bytes to skip
     * @return the number of bytes skipped.
     */
    int skip(int n);

    /**
     * Put a byte to the current position and increment the position.
     * @param b a <code>byte</code> value
     */
    void put(byte b);
    
    /**
     * Write the bytes from the source buffer to the current position.
     * @param src a <code>Buffer</code> value
     */
    void put(Buffer src);
    
    /**
     * Put a specific byte to a specific position.
     * @param index an <code>int</code> value
     * @param b a <code>byte</code> value
     */
    void put(int index, byte b);
    
    /**
     * Put the contents of the buffer at the specific position.
     * @param index an <code>int</code> value
     * @param src a <code>Buffer</code> value
     */
    void put(int index, Buffer src);

    /**
     * The current index of the mark.
     * @return an <code>int</code> index in the buffer or -1 if the mark is not set.
     */
    int markValue();
    
    /**
     * Set a specific value for the mark.
     * @param newMark an <code>int</code> value
     */
    void markValue(int newMark);
    
    /**
     * Set the mark to the current position.
     */
    void mark();
    
    /**
     * Set the mark relative to the current position
     * @param offset an <code>int</code> value to add to the current position to obtain the mark value.
     */
    void mark(int offset);
    
    /**
     * Reset the current position to the mark 
     */
    void reset();

    /**
     * Fill the buffer from the current limit to it's capacity from whatever 
     * byte source is backing the buffer. The limit is increased if bytes filled.
     * @return an <code>int</code> value indicating the number of bytes 
     * filled or -1 if EOF is reached.
     */
    int fill();
    
    /**
     * Flush the buffer from the current position to it's limit to whatever byte
     * sink is backing the buffer. The postion is updated with the number of bytes flushed.
     * This will often be called after a flip.
     * 
     * If the entire contents of the buffer are flushed, then an implicit empty() is done.
     * 
     * @return an <code>int</code> value
     */
    int flush();
    
    /**
     * Clear the buffer. position=0, limit=capacity.
     */
    void clear();
    
    /**
     * Clear the buffer. position=0, limit=0.
     */
    void empty();
    
    /**
     * Rewind the buffer: position=0
     */
    void rewind();
    
    /**
     * Flip the buffer by setting limit=postion() and position=0
     */
    void flip();
    
    /**
     * Compact the buffer by discarding bytes before the postion (or mark if set).
     * Bytes from the position (or mark) to the limit are moved to the beginning of 
     * the buffer and the values adjusted accordingly.
     */
    void compact();

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
     * 
     * @return a <code>boolean</code> value true if comparisons on this buffer are case sensitive.
     */
    boolean isCaseSensitive();

    /**
     * 
     * @return a volitile <code>Buffer</code> from the postion to the limit.
     */
    Buffer slice();
    
    /**
     * 
     *
     * @return a volitile <code>Buffer</code> value from the mark to the limit
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
     * @return a <code>Buffer</code> duplicate.
     */
    Buffer duplicate();

    /**
     *
     * @return a readonly version of this <code>Buffer</code>.
     */
    Buffer asReadOnlyBuffer();
    
    /**
     * 
     * @return a non volitile version of this <code>Buffer</code> value
     */
    Buffer asNonVolatile();
    
    /**
     * 
     * @return a <code>byte[]</code> value of the bytes from the postion to the limit.
     */
    byte[] asArray();
    
    /**
     * 
     * @return a <code>String</code> value describing the state and contents of the buffer.
     */
    String toDetailString();
}
