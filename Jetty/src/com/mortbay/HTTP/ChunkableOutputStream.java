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
    HttpFields _footer;
    boolean _committed;
    boolean _noWrite;
    ArrayList _filters;
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
        _noWrite=false;
    }

    /* ------------------------------------------------------------ */
    /** Has any data been written to the stream.
     * @return True if write has been called.
     */
    public synchronized boolean isWritten()
    {
        return !_noWrite;
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
     * the filters are reinitialized.
     * @exception IllegalStateException 
     */
    public synchronized void reset()
        throws IllegalStateException
    {
        if (_committed)
            throw new IllegalStateException("Output committed");
        if (out!=_buffer)  // XXX need to re-install filters.
            throw new IllegalStateException("Filter(s) installed");

        // Shutdown filters 
        try
        {
            out.flush();
            out.close();
        }
        catch(Exception e)
        {
            Code.ignore(e);
        }
        out=_buffer;
        _buffer.reset();

        // reinstall filters
        try
        {
            for (int i=0;_filters!=null && i<_filters.size();i++)
            {
                Constructor filter= (Constructor)_filters.get(i++);
                Object[] args = (Object[])_filters.get(i);
                insertFilter(filter,args);
            }
        }
        catch(Exception e)
        {
            // Should not fail as they have been installed before.
            Code.fail(e);
        }
        notify(OutputObserver.RESET_BUFFER);
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
    /** Insert FilterOutputStream.
     * Place a Filtering OutputStream into this stream, but before the
     * chunking stream.  If the reset() is called, new instances of
     * the filters are automatically re-inserted.
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
        if (_filters==null)
            _filters=new ArrayList(4);
        
        if (args==null || args.length<1)
            args=new Object[1];
        
        args[0]=out;
        out=(OutputStream)filter.newInstance(args);
        _filters.add(filter);
        _filters.add(args);
    }

    
    /* ------------------------------------------------------------ */
    /** Set chunking mode.
     * @param on 
     */
    public void setChunking(boolean on)
        throws IOException
    {
        flush();
        if (on)
            _chunkSize=new byte[16];
        else
            _chunkSize=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Get chunking mode 
     */
    public boolean getChunking()
    {
        return _chunkSize!=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the footer to send with a chunked close
     * @param footer 
     */
    public void setFooter(HttpFields footer)
    {
        if (!getChunking())
            throw new IllegalStateException("Not Chunking");
        _footer=footer;
    }
    
    /* ------------------------------------------------------------ */
    public void write(int b) throws IOException
    {
        if (!_noWrite)
        {
            _noWrite=false;
            notify(OutputObserver.FIRST_WRITE);
        }
        
        out.write(b);
        if (_buffer.isFull())
            flush();
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[]) throws IOException
    {
        if (!_noWrite)
        {
            _noWrite=false;
            notify(OutputObserver.FIRST_WRITE);
        }
        out.write(b);
        if (_buffer.isFull())
            flush();
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[], int off, int len) throws IOException
    {
        if (!_noWrite)
        {
            _noWrite=false;
            notify(OutputObserver.FIRST_WRITE);
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
            notify(OutputObserver.COMMITING);
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
                _committed=true;
                _realOut.write(_chunkSize,0,i);
                _buffer.writeTo(_realOut);
                _buffer.reset();
                _realOut.write(__CRLF_B);
            }
            else
            {
                _committed=true;
                _buffer.writeTo(_realOut);
                _buffer.reset();
            }
            _realOut.flush();
            notify(OutputObserver.COMMITED);
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
        try {
            notify(OutputObserver.CLOSING);
            flush();

            // close filters
            out.close();
            out=null;
            flush();
            out=_buffer;
            
            // If chunking
            if (_chunkSize!=null)
            {
                // send last chunk and revert to normal output
                _realOut.write(__CHUNK_EOF_B);
                if (_footer!=null)
                    _footer.write(_realOut);
                else
                    _realOut.write(__CRLF_B);
                _realOut.flush();
                _chunkSize=null;
                _footer=null;
                _committed=false;
                _buffer.reset();
                if (_filters!=null)
                    _filters.clear();
            }
            else
                _realOut.close();
            notify(OutputObserver.CLOSED);
            if (_observers!=null)
                _observers.clear();
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
    {
        if (_observers!=null)
            for (int i=0;i<_observers.size();i++)
                ((OutputObserver)_observers.get(i))
                    .outputNotify(this,action);
    }
}











