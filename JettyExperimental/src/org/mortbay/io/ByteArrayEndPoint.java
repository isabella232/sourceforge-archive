//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.io;

import java.io.IOException;


/* ------------------------------------------------------------ */
/** ByteArrayEndPoint.
 * @author gregw
 *
 */
public class ByteArrayEndPoint implements EndPoint
{
    byte[] _inBytes;
    ByteArrayBuffer _in;
    ByteArrayBuffer _out;
    boolean _closed;

    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public ByteArrayEndPoint()
    {
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public ByteArrayEndPoint(byte[] input, int outputSize)
    {
        _inBytes=input;
        _in=new ByteArrayBuffer(input);
        _out=new ByteArrayBuffer(outputSize);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the in.
     */
    public ByteArrayBuffer getIn()
    {
        return _in;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param in The in to set.
     */
    public void setIn(ByteArrayBuffer in)
    {
        _in = in;
    }
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the out.
     */
    public ByteArrayBuffer getOut()
    {
        return _out;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param out The out to set.
     */
    public void setOut(ByteArrayBuffer out)
    {
        _out = out;
    }
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#isClosed()
     */
    public boolean isClosed()
    {
        return _closed;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#isBlocking()
     */
    public boolean isBlocking()
    {
        return false;
    }

    /* ------------------------------------------------------------ */
    public void blockReadable(long millisecs)
    {
    }

    /* ------------------------------------------------------------ */
    public void blockWritable(long millisecs)
    {
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#close()
     */
    public void close() throws IOException
    {
        _closed=true;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#fill(org.mortbay.io.Buffer)
     */
    public int fill(Buffer buffer) throws IOException
    {
        if (_closed)
            Portable.throwIO("CLOSED");
        if (_in.length()<=0)
            return -1;
        return buffer.put(_in);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#flush(org.mortbay.io.Buffer)
     */
    public int flush(Buffer buffer) throws IOException
    {
        if (_closed)
            Portable.throwIO("CLOSED");
        return _out.put(buffer);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#flush(org.mortbay.io.Buffer, org.mortbay.io.Buffer, org.mortbay.io.Buffer)
     */
    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
    {
        if (_closed)
            Portable.throwIO("CLOSED");
        int flushed=0;
        if (header!=null && header.length()>0)
            flushed+=_out.put(header);
        
        if (header==null || header.length()==0)
        {
            if (buffer!=null && buffer.length()>0)
                flushed+=_out.put(buffer);
            
            if (buffer==null || buffer.length()==0)
            {
                if (trailer!=null && trailer.length()>0)
                    flushed+=_out.put(trailer);
            }
        }
        
        return flushed;
    }

    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public void reset()
    {
        _closed=false;
        _in.clear();
        _out.clear();
        if (_inBytes!=null)
            _in.setPutIndex(_inBytes.length);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getLocalAddr()
     */
    public String getLocalAddr()
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getLocalHost()
     */
    public String getLocalHost()
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getLocalPort()
     */
    public int getLocalPort()
    {
        return 0;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getRemoteAddr()
     */
    public String getRemoteAddr()
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getRemoteHost()
     */
    public String getRemoteHost()
    {
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.io.EndPoint#getRemotePort()
     */
    public int getRemotePort()
    {
        return 0;
    }


}
