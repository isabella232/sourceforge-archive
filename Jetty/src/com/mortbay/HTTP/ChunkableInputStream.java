// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Util.*;
import com.sun.java.util.collections.*;

import java.io.*;

/* ------------------------------------------------------------ */
/** HTTP Chunking InputStream. 
 * This FilterInputStream acts as a BufferedInputStream until
 * setChunking(true) is called.  Once chunking is
 * enabled, the raw stream is chunk decoded as per RFC2616.
 *
 * @version 1.0 Wed Sep 29 1999
 * @author Greg Wilkins (gregw)
 */
public class ChunkableInputStream extends FilterInputStream
{
    /* ------------------------------------------------------------ */
    /** Limit max line length */
    public static int __maxLineLength=4096;
    
    /* ------------------------------------------------------------ */
    private DeChunker _deChunker;
    private LineInput _realIn;
    private boolean _chunking;
    private int _filters;
    
    /* ------------------------------------------------------------ */
    /** Constructor
     */
    public ChunkableInputStream( InputStream in)
    {
        super(new LineInput(in));
        _realIn=(LineInput)this.in;
    }
    
    /* ------------------------------------------------------------ */
    /** Get chunking mode 
     */
    public boolean getChunking()
    {
        return _chunking;
    }
    
    /* ------------------------------------------------------------ */
    /** Set chunking mode 
     * @param on Chunk if this is true.
     */
    public synchronized void setChunking(boolean on)
    {
        if (on)
        {
            if (_realIn.getByteLimit()>=0)
                throw new IllegalStateException("Has Content-Length");
            if (_deChunker==null)
                _deChunker=new DeChunker();
            in=_deChunker;
        }
        else
        {
            if (_deChunker._chunkSize>0)
                throw new IllegalStateException("Within Chunk");
        }
        
        _chunking=on;
        if (_deChunker!=null)
            _deChunker._footer=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the content length.
     * Only this number of bytes can be read before EOF is returned.
     * @param len length.
     */
    public void setContentLength(int len)
    {
        if (_chunking && len>=0)
            throw new IllegalStateException("Chunking");
        _realIn.setByteLimit(len);
    }
    
    /* ------------------------------------------------------------ */
    /** Get the content length
     * @return Number of bytes until EOF is returned or -1 for no limit.
     */
    public int getContentLength()
    {
        return _realIn.getByteLimit();
    }

    public com.mortbay.Util.LineInput$LineBuffer readLine(int maxLen)
        throws IOException,
               IllegalStateException
    {
        if (_chunking || _filters>0)
            throw new IllegalStateException("Chunking or filters");
        return _realIn.readLine(maxLen);
    }
    

    /* ------------------------------------------------------------ */
    public HttpFields getFooter()
    {
        return _deChunker._footer;
    }
    
    /* ------------------------------------------------------------ */
    /** Dechunk input.
     * Or limit content length.
     */
    private class DeChunker extends InputStream
    {
        /* ------------------------------------------------------------ */
        int _chunkSize=0;
        HttpFields _footer=null;
        
        /* ------------------------------------------------------------ */
        /** Constructor
         */
        public DeChunker()
        {}

        /* ------------------------------------------------------------ */
        public int read()
            throws IOException
        {
            int b=-1;
            if (_chunkSize<=0 && getChunkSize()<=0)
                return -1;
            b=_realIn.read();
            _chunkSize=(b<0)?-1:(_chunkSize-1);
            return b;
        }
 
        /* ------------------------------------------------------------ */
        public int read(byte b[]) throws IOException
        {
            int len = b.length;
            if (_chunkSize<=0 && getChunkSize()<=0)
                return -1;
            if (len > _chunkSize)
                len=_chunkSize;
            len=_realIn.read(b,0,len);
            _chunkSize=(len<0)?-1:(_chunkSize-len);
            return len;
        }
 
        /* ------------------------------------------------------------ */
        public int read(byte b[], int off, int len) throws IOException
        {  
            if (_chunkSize<=0 && getChunkSize()<=0)
                return -1;
            if (len > _chunkSize)
                len=_chunkSize;
            len=_realIn.read(b,off,len);
            _chunkSize=(len<0)?-1:(_chunkSize-len);
            return len;
        }
    
        /* ------------------------------------------------------------ */
        public long skip(long len) throws IOException
        { 
            if (_chunkSize<=0 && getChunkSize()<=0)
                return -1;
            if (len > _chunkSize)
                len=_chunkSize;
            len=_realIn.skip(len);
            _chunkSize=(len<0)?-1:(_chunkSize-(int)len);
            return len;
        }

        /* ------------------------------------------------------------ */
        public int available()
            throws IOException
        {
            int len = _realIn.available();
            if (len<=_chunkSize)
                return len;
            return _chunkSize;
        }
 
        /* ------------------------------------------------------------ */
        public void close()
            throws IOException
        {
            _realIn.close();
            _chunkSize=-1;
        }
 
        /* ------------------------------------------------------------ */
        /** Mark is not supported
         * @return false
         */
        public boolean markSupported()
        {
            return false;
        }
    
        /* ------------------------------------------------------------ */
        /** Not Implemented
         */
        public void reset()
        {
            Code.notImplemented();
        }

        /* ------------------------------------------------------------ */
        /** Not Implemented
         * @param readlimit 
         */
        public void mark(int readlimit)
        {
            Code.notImplemented();
        }
    
        /* ------------------------------------------------------------ */
        /* Get the size of the next chunk.
         * @return size of the next chunk or -1 for EOF.
         * @exception IOException 
         */
        private int getChunkSize()
            throws IOException
        {
            if (_chunkSize<0)
                return -1;
        
            _footer=null;
            _chunkSize=-1;

            // Get next non blank line
            com.mortbay.Util.LineInput$LineBuffer line_buffer
                =_realIn.readLine(64);
            while(line_buffer!=null && line_buffer.size==0)
                line_buffer=_realIn.readLine(64);
            String line= new String(line_buffer.buffer,0,line_buffer.size);
            
            // Handle early EOF or error in format
            if (line==null)
                return -1;
        
            // Get chunksize
            int i=line.indexOf(';');
            if (i>0)
                line=line.substring(0,i).trim();
            _chunkSize = Integer.parseInt(line,16);
        
            // check for EOF
            if (_chunkSize==0)
            {
                _chunkSize=-1;
                // Look for footers
                _footer = new HttpFields();
                _footer.read(_realIn);
            }

            return _chunkSize;
        }
    };
};

