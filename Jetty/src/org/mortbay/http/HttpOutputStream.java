// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.http;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.ByteBufferOutputStream;
import org.mortbay.util.Code;
import org.mortbay.util.IO;
import org.mortbay.util.OutputObserver;

/* ---------------------------------------------------------------- */
/** HTTP Http OutputStream.
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
 * This class is not synchronized and should be synchronized
 * explicitly if an instance is used by multiple threads.
 *
 * @version $Id$
 * @author Greg Wilkins
*/
public class HttpOutputStream
    extends FilterOutputStream
    implements OutputObserver,
               HttpMessage.HeaderWriter
{
    /* ------------------------------------------------------------ */
    final static String
        __CRLF      = "\015\012";
    final static byte[]
        __CRLF_B    = {(byte)'\015',(byte)'\012'};
    final static byte[]
        __CHUNK_EOF_B ={(byte)'0',(byte)'\015',(byte)'\012'};

    final static int __BUFFER_SIZE=4096;
    final static int __FIRST_RESERVE=512;
    
    public final static Class[] __filterArg = {java.io.OutputStream.class};
    
    /* ------------------------------------------------------------ */
    private OutputStream _realOut;
    private NullableOutputStream _nullableOut;
    private HttpMessage.HeaderWriter _headerOut;
    private BufferedOutputStream _bufferedOut;
    private ChunkingOutputStream _chunkingOut;
    
    private boolean _written;
    
    private ArrayList _observers;
    
    private int _bytes;
    private int _bufferSize;
    private int _headerReserve;
    private boolean _bufferHeaders;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param outputStream The outputStream to buffer or chunk to.
     */
    public HttpOutputStream(OutputStream outputStream)
    {
        this (outputStream,__BUFFER_SIZE,__FIRST_RESERVE);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param outputStream The outputStream to buffer or chunk to.
     */
    public HttpOutputStream(OutputStream outputStream, int bufferSize)
    {
        this (outputStream,bufferSize,__FIRST_RESERVE);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param outputStream The outputStream to buffer or chunk to.
     */
    public HttpOutputStream(OutputStream outputStream,
                            int bufferSize,
                            int headerReserve)
    {
        super(outputStream);
        _written=false;
        _bufferSize=bufferSize;
        _headerReserve=headerReserve;
        
        _realOut=outputStream;
        _nullableOut=new NullableOutputStream(_realOut,headerReserve);
        _headerOut=_nullableOut;
        out=_nullableOut;
    }
    
    /* ------------------------------------------------------------ */
    public void setBufferedOutputStream(BufferedOutputStream bos,
                                        boolean bufferHeaders)
    {
        _bufferedOut=bos;
        _bufferedOut.setCommitObserver(this);
        _bufferHeaders=bufferHeaders;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the backing output stream.
     * A stream without filters or chunking is returned.
     * @return Raw OutputStream.
     */
    public OutputStream getOutputStream()
    {
        return _realOut;
    }
    
    /* ------------------------------------------------------------ */
    /** Get Filter OutputStream.
     * Get the current top of the OutputStream filter stack
     * @return OutputStream.
     */
    public OutputStream getFilterStream()
    {
        return out;
    }
    
    /* ------------------------------------------------------------ */
    /** Set Filter OutputStream.
     * Set output filter stream, which should be constructed to wrap
     * the stream returned from get FilterStream.
     */
    public void setFilterStream(OutputStream filter)
    {
        out=filter;
    }
    
    /* ------------------------------------------------------------ */
    /** Has any data been written to the stream.
     * @return True if write has been called.
     */
    public boolean isWritten()
    {
        return _written;
    }
        
    /* ------------------------------------------------------------ */
    /** Get the output buffer capacity.
     * @return Buffer capacity in bytes.
     */
    public int getBufferCapacity()
    {
        if (_bufferedOut==null)
            return _bufferSize-_headerReserve;
        return _bufferedOut.capacity();
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
        
        if (_bufferedOut!=null && _bufferedOut.size()>0)
            throw new IllegalStateException("Buffer is not empty");

        try
        {
            _bufferSize=capacity;
            if (_bufferedOut!=null)
                _bufferedOut.ensureSpareCapacity(capacity);
            if (_chunkingOut!=null)
                _chunkingOut.ensureSpareCapacity(capacity);
        }
        catch (IOException e){Code.warning(e);}
    }

    /* ------------------------------------------------------------ */
    public int getBytesWritten()
    {
        return _bytes;
    }
    
    /* ------------------------------------------------------------ */
    /** Reset Buffered output.
     * If no data has been committed, the buffer output is discarded and
     * the filters may be reinitialized.
     * @exception IllegalStateException
     * @exception Problem with observer notification.
     */
    public void resetBuffer()
        throws IllegalStateException
    {
        // Shutdown filters without observation
        if (out!=null && out!=_headerOut)
        {
            ArrayList save_observers=_observers;
            _observers=null;
            try
            {
                _nullableOut.nullOutput();
                out.close();
            }
            catch(Exception e)
            {
                Code.ignore(e);
            }
            finally
            {
                _observers=save_observers;
                _nullableOut.resetStream();
            }
        }
        
        // discard current buffer and set it to output
        if (_bufferedOut!=null)
            _bufferedOut.resetStream();
        if (_chunkingOut!=null)
            _chunkingOut.resetStream();

        _headerOut=_nullableOut;
        out=(OutputStream)_headerOut;
	_bytes=0;
        _written=false;
        try
        {
            notify(OutputObserver.__RESET_BUFFER);
        }
        catch(IOException e)
        {
            Code.ignore(e);
        }
    }

    /* ------------------------------------------------------------ */
    /** Add an Output Observer.
     * Output Observers get notified of significant events on the
     * output stream. Observers are called in the reverse order they
     * were added.
     * They are removed when the stream is closed.
     * @param observer The observer. 
     */
    public void addObserver(OutputObserver observer)
    {
        if (_observers==null)
            _observers=new ArrayList(4);
        _observers.add(observer);
        _observers.add(null);
    }
    
    /* ------------------------------------------------------------ */
    /** Add an Output Observer.
     * Output Observers get notified of significant events on the
     * output stream. Observers are called in the reverse order they
     * were added.
     * They are removed when the stream is closed.
     * @param observer The observer. 
     * @param data Data to be passed wit notify calls. 
     */
    public void addObserver(OutputObserver observer, Object data)
    {
        if (_observers==null)
            _observers=new ArrayList(4);
        _observers.add(observer);
        _observers.add(data);
    }
    
    /* ------------------------------------------------------------ */
    /** Null the output.
     * All output written is discarded until the stream is reset. Used
     * for HEAD requests.
     */
    public void nullOutput()
        throws IOException
    {
        _nullableOut.nullOutput();
    }
    
    /* ------------------------------------------------------------ */
    /** is the output Nulled?
     */
    public boolean isNullOutput()
        throws IOException
    {
        return _nullableOut.isNullOutput();
    }
    
    /* ------------------------------------------------------------ */
    /** Set chunking mode.
     */
    public void setChunking()
    {
        if (_chunkingOut==null)
        {
            _chunkingOut=new ChunkingOutputStream(_nullableOut,
                                                  _bufferSize,
                                                  _headerReserve);
            _chunkingOut.setCommitObserver(this);
        }
        _headerOut=_chunkingOut;
        out=_chunkingOut;
    }
    
    /* ------------------------------------------------------------ */
    /** Get chunking mode 
     */
    public boolean isChunking()
    {
        return _chunkingOut!=null && _headerOut==_chunkingOut;
    }
    
    /* ------------------------------------------------------------ */
    /** Reset the stream.
     * Turn disable all filters.
     * @exception IllegalStateException The stream cannot be
     * reset if chunking is enabled.
     */
    public void resetStream()
        throws IOException, IllegalStateException
    {
        if (isChunking())
            close();
        
        _written=false;
        if (_bufferedOut!=null)
            _bufferedOut.resetStream();
        if (_chunkingOut!=null)
            _chunkingOut.resetStream();
        _nullableOut.resetStream();
        
        _headerOut=_nullableOut;
        out=_nullableOut;

        _bytes=0;

        if (_observers!=null)
            _observers.clear();
    }

    /* ------------------------------------------------------------ */
    public void destroy()
    {
        if (_bufferedOut!=null)
            _bufferedOut.destroy();
        if (_chunkingOut!=null)
            _chunkingOut.destroy();
        if (_nullableOut!=null)
            _nullableOut.destroy();
    }
    
    /* ------------------------------------------------------------ */
    /** Set the trailer to send with a chunked close.
     * @param trailer 
     */
    public void setTrailer(HttpFields trailer)
    {
        if (!isChunking())
            throw new IllegalStateException("Not Chunking");
        _chunkingOut.setTrailer(trailer);
    }
    
    /* ------------------------------------------------------------ */
    public void writeHeader(HttpMessage httpMessage)
        throws IOException
    {
        if (isNullOutput())
            _nullableOut.writeHeader(httpMessage);
        else if (_bufferHeaders)
            _bufferedOut.writeHeader(httpMessage);
        else
            _headerOut.writeHeader(httpMessage);
    }
    
    /* ------------------------------------------------------------ */
    public void write(int b) throws IOException
    {
        prepareOutput();
        out.write(b);
        _bytes++;
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[]) throws IOException
    {
        write(b,0,b.length);
    }

    /* ------------------------------------------------------------ */
    public void write(byte b[], int off, int len)
        throws IOException
    {     
        prepareOutput();
        out.write(b,off,len);
        _bytes+=len;
    }
    
    /* ------------------------------------------------------------ */
    protected void prepareOutput()
        throws IOException
    {   
	if (out==null)
	    throw new IOException("closed");

        if (!_written)
        {
            _written=true;

            if (out==_nullableOut)
            {
                if (_bufferedOut==null)
                {
                    _bufferedOut=new BufferedOutputStream(_nullableOut,
                                                          _bufferSize,
                                                          _headerReserve,0,0);
                    _bufferedOut.setCommitObserver(this);
                    _bufferedOut.setBypassBuffer(true);
                    _bufferedOut.setFixed(true);
                }
                out=_bufferedOut;
                _headerOut=_bufferedOut;
            }
            
            notify(OutputObserver.__FIRST_WRITE);
            
            if (out==_nullableOut)
                notify(OutputObserver.__COMMITING);
        }        
    }
    /* ------------------------------------------------------------ */
    public void flush()
        throws IOException
    {
        if (out!=null)
        {
            if (out==_nullableOut)
                notify(OutputObserver.__COMMITING);
            out.flush();
        }
    }
    
    
    /* ------------------------------------------------------------ */
    /** Close the stream.
     * @exception IOException 
     */
    public void close()
        throws IOException
    {        
        // Are we already closed?
        if (out==null)
            return;
        
        // Close
        try {
            if (out==_nullableOut)
                notify(OutputObserver.__COMMITING);
            
            notify(OutputObserver.__CLOSING);
            if (_bufferHeaders)
                _bufferedOut.close();
            else if (out!=null)
                out.close();
            out=null;
            _headerOut=_nullableOut;
            notify(OutputObserver.__CLOSED);
        }
        catch (IOException e)
        {
            Code.ignore(e);
        }
    }

    /* ------------------------------------------------------------ */
    /** Output Notification.
     * Called by the internal Buffered Output and the event is passed on to
     * this streams observers.
     */
    public void outputNotify(OutputStream out, int action, Object ignoredData)
        throws IOException
    {
        notify(action);
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
            for (int i=_observers.size();i-->0;)
            {
                Object data=_observers.get(i--);
                ((OutputObserver)_observers.get(i)).outputNotify(this,action,data);
            }
    }

    /* ------------------------------------------------------------ */
    public void write(InputStream in, int len)
        throws IOException
    {
        IO.copy(in,this,len);
    }
}
