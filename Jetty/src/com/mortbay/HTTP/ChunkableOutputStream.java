// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.util.Observer;
import java.io.*;
import java.lang.reflect.*;

/* ---------------------------------------------------------------- */
/** HTTP Chunkable OutputStream
 * Acts as a BufferedOutputStream until setChunking(true) is called.
 * Once chunking is enabled, the raw stream is chunk encoded as per RFC2616.
 *
 * Implements the following HTTP and Servlet features: <UL>
 * <LI>Filters for content and transfer encodings.
 * <LI>Allows output to be reset if not committed (buffer never flushed).
 * <LI>Notification of significant output events for filter triggering,
 *     header flushing, etc.
 * </UL>
 * 
 * @version $Id$
 * @author Greg Wilkins
*/
public class ChunkableOutputStream extends FilterOutputStream
{
    /* ------------------------------------------------------------ */
    final static byte[]
        __CRLF_B      ={(byte)'\015',(byte)'\012'},
        __CHUNK_EOF_B ={(byte)'0',(byte)';',(byte)'\015',(byte)'\012'};

    public final static Class[] __filterArg = {java.io.OutputStream.class};
        
    /* ------------------------------------------------------------ */
    /** Buffer class.
     */
    private static class Buffer extends ByteArrayOutputStream
    {
        int _flushAt=4000;
        Buffer(){super(4096);}
        Buffer(int capacity){super(capacity);_flushAt=(capacity*95)/100;}
        int getCapacity(){return buf.length;}
        boolean isFull() {return count>=_flushAt;}
    }
    
    
    /* ------------------------------------------------------------ */
    OutputStream _realOut;
    Buffer _buffer;
    byte[] _chunkSize;
    HttpFields _trailer;
    boolean _committed;
    boolean _written;
    int _filters;
    ArrayList _observers;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param outputStream The outputStream to buffer or chunk to.
     */
    public ChunkableOutputStream(OutputStream outputStream)
    {
        super(new Buffer());
        _buffer=(Buffer)out;
        _realOut=outputStream;
        _committed=false;
        _written=false;
    }

    /* ------------------------------------------------------------ */
    /** Get the raw stream.
     * A stream without filters or chunking is returned.
     * @return Raw OutputStream.
     */
    public OutputStream getRawStream()
    {
        return _realOut;
    }
    
    /* ------------------------------------------------------------ */
    /** Has any data been written to the stream.
     * @return True if write has been called.
     */
    public synchronized boolean isWritten()
    {
        return !_written;
    }
    
    /* ------------------------------------------------------------ */
    /** Has any data been sent from this stream.
     * @return True if buffer has been flushed to destination.
     */
    public synchronized boolean isCommitted()
    {
        return _committed;
    }
        
    /* ------------------------------------------------------------ */
    /** Get the output buffer capacity
     * @return Buffer capacity in bytes.
     */
    public int getBufferCapacity()
    {
        return _buffer.getCapacity();
    }
    
    /* ------------------------------------------------------------ */
    /** Set the output buffer capacity.
     * Note that this is the minimal buffer capacity and that installed
     * filters may perform their own buffering and are likely to change
     * the size of the output.
     * @param capacity Minimum buffer capacity in bytes
     * @exception IllegalStateException If output has been written.
     */
    public void setBufferCapacity(int capacity)
        throws IllegalStateException
    {
        if (capacity<=getBufferCapacity())
            return;
        
        if (_buffer.size()>0)
            throw new IllegalStateException("Buffer is not empty");
        if (_committed)
            throw new IllegalStateException("Output committed");
        if (out!=_buffer)
            throw new IllegalStateException("Filter(s) installed");

        out=_buffer=new Buffer(capacity);
    }

    
    /* ------------------------------------------------------------ */
    /** Reset Buffered output.
     * If no data has been committed, the buffer output is discarded and
     * the filters may be reinitialized.
     * @exception IllegalStateException
     * @exception Problem with observer notification.
     */
    public synchronized void resetBuffer()
        throws IllegalStateException, IOException
    {
        if (_committed)
            throw new IllegalStateException("Output committed");

        if (Code.verbose())
            Code.debug("resetBuffer()");
        
        // Shutdown filters without observation
        ArrayList save_observers=_observers;
        _observers=null;
        try
        {
            out.flush();
            out.close();
        }
        catch(Exception e)
        {
            Code.ignore(e);
        }
        finally
        {
            _observers=save_observers;
        }

        // discard current buffer and set it to output
        _buffer.reset();
        out=_buffer;
        _filters=0;
        _written=false;
        _committed=false;
        notify(OutputObserver.__RESET_BUFFER);
    }

    /* ------------------------------------------------------------ */
    /** Add an Output Observer.
     * Output Observers get notified of significant events on the
     * output stream. They are removed when the stream is closed.
     * @param observer The observer. 
     */
    public synchronized void addObserver(OutputObserver observer)
    {
        if (_observers==null)
            _observers=new ArrayList(4);
        _observers.add(observer);
    }
    
    
    /* ------------------------------------------------------------ */
    /** Set chunking mode.
     */
    public synchronized void setChunking()
        throws IOException
    {
        flush();
        _chunkSize=new byte[16];
    }
    
    /* ------------------------------------------------------------ */
    /** Send the final chunk chunking mode.
     * This also calls resetStream().
     * @exception IOException 
     * @exception IllegalStateException chunking not set
     */
    public synchronized void endChunking()
        throws IOException,IllegalStateException
    {
        if (!isChunking())
            throw new IllegalStateException("Not Chunking");
        
        if (Code.verbose())
            Code.debug("endChunking()");
        
        flush();
        
        notify(OutputObserver.__CLOSING);
        
        // send last chunk and revert to normal output
        _realOut.write(__CHUNK_EOF_B);
        if (_trailer!=null)
            _trailer.write(_realOut);
        else
            _realOut.write(__CRLF_B);
        _realOut.flush();
        _chunkSize=null;
        resetStream();
        notify(OutputObserver.__CLOSED);
    }
    
    /* ------------------------------------------------------------ */
    /** Reset the stream.
     * Turn disable all filters.
     * @exception IllegalStateException The stream cannot be
     * reset if chunking is enabled.
     */
    public synchronized void resetStream()
        throws IllegalStateException
    {
        if (isChunking())
            throw new IllegalStateException("Chunking");
        
        if (Code.verbose())
            Code.debug("resetStream()");
        
        _trailer=null;
        _committed=false;
        _written=false;
        _buffer.reset();
        out=_buffer;    
        _filters=0;
    }
    
        
    /* ------------------------------------------------------------ */
    /** Get chunking mode 
     */
    public boolean isChunking()
    {
        return _chunkSize!=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Insert FilterOutputStream.
     * Place a Filtering OutputStream into this stream, but before the
     * chunking stream.  
     * @param filter The Filter constructor.  It must take an OutputStream
     *             as the first arguement.
     * @param arg  Optional argument array to pass to filter constructor.
     *             The first element of the array is replaced with the
     *             current output stream.
     */
    public synchronized void insertFilter(Constructor filter,
                                          Object[] args)
        throws InstantiationException,
               InvocationTargetException,
               IllegalAccessException
    {
        if (args==null || args.length<1)
            args=new Object[1];
        
        args[0]=out;
        out=(OutputStream)filter.newInstance(args);
        _filters++;
    }

    /* ------------------------------------------------------------ */
    /** Set the trailer to send with a chunked close
     * @param trailer 
     */
    public void setTrailer(HttpFields trailer)
    {
        if (!isChunking())
            throw new IllegalStateException("Not Chunking");
        _trailer=trailer;
    }
    
    /* ------------------------------------------------------------ */
    public void write(int b) throws IOException
    {
        if (!_written)
        {
            _written=true;
            notify(OutputObserver.__FIRST_WRITE);
        }
        
        out.write(b);
        if (_buffer.isFull())
            flush();
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[]) throws IOException
    {
        if (!_written)
        {
            _written=true;
            notify(OutputObserver.__FIRST_WRITE);
        }
        out.write(b);
        if (_buffer.isFull())
            flush();
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[], int off, int len) throws IOException
    {
        if (!_written)
        {
            _written=true;
            notify(OutputObserver.__FIRST_WRITE);
        }
        out.write(b,off,len);
        if (_buffer.isFull())
            flush();
    }

    /* ------------------------------------------------------------ */
    public synchronized void flush() throws IOException
    {
        if (out!=null)
            out.flush();
        if (_buffer.size()>0)
        {
            if (!_committed)
                notify(OutputObserver.__COMMITING);
            
            if (_chunkSize!=null)
            {
                String size = Integer.toString(_buffer.size(),16);
                byte[] b = size.getBytes();
                int i;
                for (i=0;i<b.length;i++)
                    _chunkSize[i]=b[i];
                _chunkSize[i++]=(byte)';';
                _chunkSize[i++]=__CRLF_B[0];
                _chunkSize[i++]=__CRLF_B[1];
                _realOut.write(_chunkSize,0,i);
                _buffer.writeTo(_realOut);
                _buffer.reset();
                _realOut.write(__CRLF_B);
            }
            else
            {
                _buffer.writeTo(_realOut);
                _buffer.reset();
            }
            _realOut.flush();
            
            if (!_committed)
                notify(OutputObserver.__COMMITED);
            _committed=true;
        }
    }

    /* ------------------------------------------------------------ */
    /** Close the stream.
     * In chunking mode, the underlying stream is not closed.
     * All filters are closed and discarded.
     * @exception IOException 
     */
    public synchronized void close()
        throws IOException
    {
        // Are we already closed?
        if (out==null)
            return;

        // Close
        try {
            notify(OutputObserver.__CLOSING);
            flush();

            // close filters
            out.close();
            out=null;
            flush();
            
            // If chunking
            if (isChunking())
                endChunking();
            else
                _realOut.close();
            notify(OutputObserver.__CLOSED);
        }
        catch (IOException e)
        {
            Code.ignore(e);
        }
    }

    /* ------------------------------------------------------------ */
    /* Notify observers of action.
     * @see OutputObserver
     * @param action the action.
     */
    private void notify(int action)
        throws IOException
    {
        if (_observers!=null)
            for (int i=0;i<_observers.size();i++)
                ((OutputObserver)_observers.get(i))
                    .outputNotify(this,action);
    }


    /* ------------------------------------------------------------ */
    public void write(InputStream in, int len)
        throws IOException
    {
        IO.copy(in,this,len);
    }
    
    /* ------------------------------------------------------------ */
    public void println()
        throws IOException
    {
        write("\n".getBytes());
    }
    
    /* ------------------------------------------------------------ */
    public void println(Object o)
        throws IOException
    {
        if (o!=null)
            write(o.toString().getBytes());
        write("\n".getBytes());
    }
    
    /* ------------------------------------------------------------ */
    public void print(Object o)
        throws IOException
    {
        if (o!=null)
            write(o.toString().getBytes());
    }
}
