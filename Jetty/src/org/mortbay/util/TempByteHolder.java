/*
 * $Id$
 */

package org.mortbay.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.RandomAccessFile;



/**
 * Temporary buffer for bytes to be used in situations where bytes need to be buffered 
 * but total size of data is not known in advance and may potentially be very large.
 * Provides easy way to access small buffered data as byte[] or String.
 * Enables efficient memory-only handling of small data while automatically switching
 * to temporary file storage when data gets too big to fit in memory buffer.
 * It is highly efficient for both byte-per-byte and block I/O.
 * This class is not a FIFO - you can't mix reading and writing infinitely as all data
 * keep being buffered, not just unread data.
 * Mixing reads and writes may be inefficient in some situations but is fully supported.
 * <br>
 * Overall usage strategy: You first write data to the buffer using OutputStream 
 * returned by getOutputStream(), then examine data size using getLength() 
 * and isSmallEnough() and either call getBytes() to get byte[],
 * getString() to get data as String or getInputStream() to read data using stream.
 * Instance of TempByteHolder can be safely and efficiently reused by calling clear().
 * When TempByteHolder is no longer needed you must call close() to ensure underlying
 * temporary file is closed and deleted.
 * <br><br>
 * <i>NOTE:</i> For performance, this class is not synchronized. If you need thread safety, 
 * use synchronized wrapper.<br>
 * This class can hold up to 2GB of data.
 * <br><br>
 * <i>SECURITY NOTE:</i> As data may be written to disk, don't use this for sensitive information.
 * @author  Jan Hlavatý <hlavac@code.cz>
 */
public class TempByteHolder {
    
    byte[] memory_buffer = null;    /** buffer to use */
    
    boolean file_mode = false;  /** false: memory buffer mode (small data)
                                    true: temp file mode (large data) */

    int window_size = 0;    /** size of memory buffer */
    int window_low = 0;     /** offset of first byte in memory buffer */
    int window_high = 0;    /** offset of first byte after memory buffer */
    int file_high = 0;      /** offset of fist byte not yet written to temp file */
    int write_pos = 0;      /** offset of next byte to be writen; number of bytes written */
    int read_pos = 0;       /** offset of fist byte to be read */
    int file_pos = -1;      /** current temp file seek offset; -1 = unknown */
    int mark_pos = 0;       /** mark */
    

    /** Instance of OutputStream is cached and reused. */
    TempByteHolder.OutputStream output_stream = new TempByteHolder.OutputStream();
    /** Instance of InputStream is cached and reused. */
    TempByteHolder.InputStream input_stream = new TempByteHolder.InputStream();
    
    /** Temporary directory to be used, or null for system default */
    File temp_directory = null;
    /** File object representing temporary file. */
    File tempfilef = null;
    /** Temporary file or null when none is used yet */
    RandomAccessFile tempfile = null;

    
    //----- constructors -------
    
    /**
     * Creates a new instance of TempByteHolder allocating memory buffer of given capacity.
     * You should use reasonably large buffer for potentionally large data to improve
     * effect of caching for file operations (about 512 bytes).
     * @param in_memory_capacity Size in bytes of memory buffer to allocate.
     */
    public TempByteHolder(int in_memory_capacity) {
        this(new byte[in_memory_capacity]);
    }
    
    /**
     * Creates a new instance of TempByteHolder using passed byte[] as memory buffer.
     * @param byte_array byte array to be used as memory buffer.
     */
    public TempByteHolder(byte[] byte_array) {
        if (byte_array == null) throw new NullPointerException();
        memory_buffer = byte_array;
        window_size = memory_buffer.length;
        window_high = window_size;
    }
    
    public void finalize() {
        try {
            close();
        } catch (IOException e) {
        }
    }

    /**
     * Erases all unread buffered data and prepares for next use cycle.
     * If temporary file was used, it is not closed/deleted yet as it may be needed again.
     */
    public void clear() {
        file_mode = false;
        write_pos = 0;
        read_pos = 0;
        window_low = 0;
        window_high = window_size;
        file_high = 0;
        mark_pos = 0;
    }
    
    /**
     * Clears all data and closes/deletes backing temporary file if used.
     * @throws IOException when something goes wrong.
     */
    public void close() throws IOException {
        clear();
        if (tempfile != null) {
            tempfile.close();
            tempfile = null;
            tempfilef.delete();
            tempfilef = null;
        }
    }

    /**
     * Repositions InputStream at given offset within buffered data.
     * @throws IOException when something goes wrong.
     */
    public void seek(int offset) throws IOException {
        if ((offset <= write_pos)&&(offset>=0)) {
            read_pos = offset;
        } else throw new IOException("bad seek offset");
    }
    
    /**
     * Truncates buffered data to specified size. Can not be used to extend data.
     * Repositions OutputStream at the end of truncated data.
     * If current read offset or mark is past the new end of data, it is moved at the new end.
     */
    public void truncate(int offset) throws IOException {
        if ((offset < 0)||(offset > write_pos)) throw new IOException("bad truncate offset");
        if (read_pos > offset) read_pos = offset;
        if (mark_pos > offset) mark_pos = offset;
        write_pos = offset;
        if (file_high > offset) file_high = offset;
        moveWindow(write_pos);
    }
    

    /**
     * Override directory to create temporary file in.
     * Does not affect already open temp file.
     * @param dir File object representing temporary directory.
     * May be null which means that system default
     * (java.io.tmpdir system property) should be used.
     * @throws IOException
     */
    public void setTempDirectory(File dir) throws IOException {
        File td = dir.getCanonicalFile();
        if (td.isDirectory()) {
            temp_directory = td;
        }
    }
    
    
    
    /**
     * Returns number of bytes buffered so far.
     * @return total number of bytes buffered. If you need number of bytes
     * to be read, use InputStream.available() .
     */
    public int getLength() {
        return write_pos;
    }
    
    /**
     * Tells whether buffered data is small enough to fit in memory buffer
     * so that it can be returned as byte[]. Data is considered small enough
     * when it will fit into backing memory buffer.
     * @return true when data is small and can be returned as byte[],
     * false when data will not fit in byte[] and has to be read using InputStream.
     */
    public boolean isSmallEngough() {
        return !file_mode;
    }
    

    /**
     * Returns byte[] that holds all buffered data in its first getLength() bytes.
     * If this instance was created using (byte[]) constructor, this is the same
     * array that has been passed to the constructor. If buffered data don't fit into
     * memory buffer, null is returned and data have to be read through InputStream
     * returned by getInputStream().
     * @return byte[] with data as its first getLength() bytes or null when data
     * is too big to be passed this way.
     */
    public byte[] getBytes() {
        if (file_mode) return null;
        return memory_buffer;
    }

    /**
     * Returns buffered data as String using given character encoding.
     * @param character_encoding Name of character encoding to use for
     * converting bytes to String.
     * @return Buffered data as String of null when data is too big.
     */
    public String getString(String character_encoding) throws java.io.UnsupportedEncodingException {
        if (file_mode) return null;
        return new String(memory_buffer,0,write_pos,character_encoding);
    }
    
    /**
     * Returns OutputStream filling this buffer.
     * @return OutputStream for writing in the buffer.
     */
    
    public java.io.OutputStream getOutputStream() {
        return output_stream;
    }
    
    
    /**
     * Returns InputSream for reading buffered data.
     * @return InputSream for reading buffered data.
     */    
    public java.io.InputStream getInputStream() {
        return input_stream;
    }

    
    // ----- helper methods -------
    
    /**
     * Create tempfile if it does not already exist
     */
    private void createTempFile() throws IOException {
        tempfilef = File.createTempFile("org.mortbay.util.TempByteHolder-",".tmp",temp_directory).getCanonicalFile();
        tempfilef.deleteOnExit();
        tempfile = new RandomAccessFile(tempfilef,"rw");
    }

    /**
     * Write chunk of data at specified offset in temp file.
     * Marks data as big.
     * Updates high water mark on tempfile content.
     */
    private void writeToTempFile(int at_offset, byte[] data, int offset, int len) throws IOException {
        if (tempfile == null) {
            createTempFile();
            file_pos = -1;
        }
        file_mode = true;
        if (at_offset != file_pos) {
            tempfile.seek((long)at_offset);
        }
        tempfile.write(data,offset,len);
        file_pos = at_offset + len;
        file_high = max(file_high,file_pos);
    }
    
    /**
     * Read chunk of data from specified offset in tempfile
     */
    private void readFromTempFile(int at_offset, byte[] data, int offset, int len) throws IOException {
        if (file_pos != at_offset) {
            tempfile.seek((long)at_offset);
        }
        tempfile.readFully(data,offset,len);
        file_pos = at_offset+len;
    }
    
    
    /**
     * Move file window, synchronizing data with file.
     * Works somewhat like memory-mapping a file.
     * This one was nightmare to write :-)
     */
    private void moveWindow(int start_offset) throws IOException {
        if (start_offset != window_low) { // only when we have to move

            int end_offset = start_offset + window_size;
            // new window low/high = start_offset/end_offset
            int dirty_low = file_high;
            int dirty_high = write_pos;
            int dirty_len = write_pos - file_high;
            if (dirty_len > 0) {   // we need to be concerned at all about dirty data.
                // will any part of dirty data be moved out of window?
                if ( (dirty_low < start_offset) || (dirty_high > end_offset) ) {
                    // yes, dirty data need to be saved.
                    writeToTempFile(dirty_low, memory_buffer, dirty_low - window_low, dirty_len);
                }
            }
            
            // reposition any data from old window that will be also in new window:
            
            int stay_low = max(start_offset,window_low);
            int stay_high = min(write_pos, window_high, end_offset);
            // is there anything to preserve?
            int stay_size = stay_high - stay_low;
            if (stay_size > 0) {
                System.arraycopy(memory_buffer, stay_low-window_low, memory_buffer, stay_low-start_offset, stay_size);
            }
            
            // read in available data that were not in old window:
            if (stay_low > start_offset) {
                // read at the start of buffer
                int toread_low = start_offset;
                int toread_high = min(stay_low,end_offset);
                int toread_size = toread_high - toread_low;
                if (toread_size > 0) {
                    readFromTempFile(toread_low, memory_buffer, toread_low-start_offset, toread_size);
                }
            }
            if (stay_high < end_offset) {
                // read at end of buffer
                int toread_low = max(stay_high,start_offset);
                int toread_high = min(end_offset,file_high);
                int toread_size = toread_high-toread_low;
                if (toread_size > 0) {
                    readFromTempFile(toread_low, memory_buffer, toread_low-start_offset, toread_size);
                }
            }
            window_low = start_offset;
            window_high = end_offset;
        }
    }

    /** Simple minimum for 2 ints */    
    private static int min(int a, int b) {
        return (a<b?a:b);
    }
    
    /** Simple maximum for 2 ints */    
    private static int max(int a, int b) {
        return (a>b?a:b);
    }
    
    /** Simple minimum for 3 ints */    
    private static int min(int a, int b, int c) {
        int r = a;
        if (r > b) r = b;
        if (r > c) r = c;
        return r;
    }
    
    /** Simple maximum for 3 ints */    
    private static int max(int a, int b, int c) {
        int r = a;
        if (r < b) r = b;
        if (r < c) r = c;
        return r;
    }

    /**
     * @return true when range 1 is fully contained in range 2
     */
    private static boolean contained(int range1_low, int range1_high, int range2_low, int range2_high) {
        return ((range1_low >= range2_low)&&(range1_high <= range2_high));
    }
    
    /**
     * Internal implementation of java.io.OutputStream used to fill the byte buffer.
     */
    class OutputStream extends java.io.OutputStream {
        
        /**
         * Write whole byte array into buffer.
         * @param data byte[] to be written
         * @throws IOException when something goes wrong.
         */        
        public void write(byte[] data) throws IOException {
            write(data,0,data.length);
        }

        /**
         * Write segment of byte array to the buffer.
         * @param data Byte array with data
         * @param off Starting offset within the array.
         * @param len Number of bytes to write
         * @throws IOException when something goes wrong.
         */        
        public void write(byte[] data, int off, int len) throws IOException {
            int new_write_pos = write_pos + len;
            boolean write_pos_in_window = (write_pos >= window_low)&&(write_pos < window_high);
            
            if (!write_pos_in_window) {
                // either current window is full of dirty data or is somewhere low
                moveWindow(write_pos);  // flush buffer if necessary, move window at end
            }
            
            boolean end_of_data_in_window = (new_write_pos <= window_high);
            
            if ( end_of_data_in_window ) {
                // if there is space in window for all data, just put it in buffer.
                // 0 writes, window unchanged
                System.arraycopy(data, off, memory_buffer, write_pos-window_low, len);
                write_pos = new_write_pos;
            } else {
                int out_of_window = new_write_pos - window_high;
                if (out_of_window < window_size) {
                    // start of data in window, rest will fit in a new window:
                    // 1 write, window moved at window_high, filled with rest of data
                    
                    // fill in rest of the current window with first part of data
                    int part1_len = window_high - write_pos;
                    int part2_len = len - part1_len;
                    
                    System.arraycopy(data, off, memory_buffer, write_pos-window_low, part1_len);
                    write_pos = window_high;
                    
                    moveWindow(write_pos);  // flush data to file
                    
                    System.arraycopy(data, off+part1_len, memory_buffer, 0, part2_len);
                    write_pos = new_write_pos;
                    
                } else {
                    // start of data in window, rest will not fit in window (and leave some space):
                    // 2 writes; window moved at end, empty
                    
                    int part1_size = window_high - write_pos;
                    int part2_size = len - part1_size;
                    
                    if (part1_size == window_size) {
                        // buffer was empty - no sense in splitting the write
                        // write data directly to file in one chunk
                        writeToTempFile(write_pos, data, off, len);
                        write_pos = new_write_pos;
                        moveWindow(write_pos);
                        
                    } else {
                        // copy part 1 to window
                        if (part1_size > 0) {
                            System.arraycopy(data, off, memory_buffer, write_pos-window_low, part1_size);
                            write_pos += part1_size;
                            moveWindow(write_pos);  // flush buffer
                        }
                        // flush window to file
                        // write part 2 directly to file
                        writeToTempFile(write_pos, data, off+part1_size, part2_size);
                        write_pos = new_write_pos;
                        moveWindow(write_pos);
                    }
                }
            }
        }
        
        /**
         * Write single byte to the buffer.
         * @param b
         * @throws IOException
         */        
        public void write(int b) throws IOException {
            if ((write_pos >= window_high) || (write_pos < window_low)) {
                moveWindow(write_pos);
            }
            // we now have space for one byte in window.
            memory_buffer[write_pos - window_low] = (byte)(b &0xFF);
            write_pos++;
        }
        
        public void flush() throws IOException {
            moveWindow(write_pos); // or no-op? not needed
        }
        
        public void close() throws IOException {
            // no-op: this output stream does not need to be closed.
        }
    }
    
    
    
    
    
    /**
     * Internal implementation of InputStream used to read buffered data.
     */    
    class InputStream extends java.io.InputStream {
        
        public int read() throws IOException {
            int ret = -1;
            // if window does not contain read position, move it there
            //if ((read_pos < window_low)||(read_pos>= window_high)) {
            if (!contained(read_pos,read_pos+1, window_low, window_high)) {
                moveWindow(read_pos);
            }
            if (write_pos > read_pos) {
                ret = (memory_buffer[read_pos - window_low])&0xFF;
                read_pos++;
            }
            return ret;
        }
        
        public int read(byte[] buff) throws IOException {
            return read(buff,0, buff.length);
        }
        
        public int read(byte[] buff, int off, int len) throws IOException {
            // clip read to available data:
            int read_size = min(len,write_pos-read_pos);
            if (read_size > 0) {
                if (read_size >= window_size) {
                    // big chunk: read directly from file
                    moveWindow(write_pos);
                    readFromTempFile(read_pos, buff, off, read_size);
                } else {
                    // small chunk:
                    int read_low = read_pos;
                    int read_high = read_low + read_size;
                    // if we got all data in current window, read it from there
                    if (!contained(read_low,read_high, window_low,window_high)) {
                        moveWindow(read_pos);
                    }
                    System.arraycopy(memory_buffer, read_pos - window_low, buff, off, read_size);
                    read_pos += read_size;
                        
                }
            }
            return read_size;
        }
        
        public long skip(long bytes) throws IOException {
            if (bytes < 0 || bytes > Integer.MAX_VALUE) throw new IllegalArgumentException();
            int len = (int)bytes;
            if ( (len+read_pos) > write_pos ) len = write_pos - read_pos;
            read_pos+=len;
            moveWindow(write_pos);  // invalidate window without reading data by moving it at the end
            return (long)len;
        }
        
        public int available() throws IOException {
            return write_pos - read_pos;
        }
        
        
        public void mark(int readlimit) {
            // readlimit is ignored, we store all the data anyway
            mark_pos = read_pos;
        }
        
        public void reset() throws IOException {
            read_pos = mark_pos;
        }
        
        public boolean markSupported() {
            return true;
        }
        
        
    }
}
