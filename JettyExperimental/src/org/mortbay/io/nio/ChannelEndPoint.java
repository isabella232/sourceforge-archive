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

package org.mortbay.io.nio;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.SocketChannel;

import org.mortbay.io.Buffer;
import org.mortbay.io.EndPoint;


/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ChannelEndPoint implements EndPoint
{
    ByteChannel _channel;
    ByteBuffer[] _gather2;
    ByteBuffer[] _gather3;
    
    /**
     * 
     */
    public ChannelEndPoint(ByteChannel chanel)
    {
        super();
        this._channel = chanel;
    }
    
    public boolean isBlocking()
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#isClosed()
     */
    public boolean isClosed()
    {
        return !_channel.isOpen();
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#close()
     */
    public void close() throws IOException
    {
        if (_channel instanceof SocketChannel)
        {
            // TODO - is this really required?
            Socket socket= ((SocketChannel)_channel).socket();
            socket.shutdownOutput();
            socket.close();
        }
        _channel.close();
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#fill(org.mortbay.io.Buffer)
     */
    public int fill(Buffer buffer) throws IOException
    {
        Buffer buf = buffer.buffer();
        int len=0;
        if (buf instanceof NIOBuffer)
        {
            NIOBuffer nbuf = (NIOBuffer)buf;
            ByteBuffer bbuf=nbuf.getByteBuffer();
            synchronized(nbuf)
            {
                try
                {
                    bbuf.position(buffer.putIndex());
                    len=_channel.read(bbuf);
                }
                finally
                {
                    buffer.setPutIndex(bbuf.position());
                    bbuf.position(0);
                }
            }
        }
        else
        {
            throw new IOException("Not Implemented");
        }
        
        return len;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#flush(org.mortbay.io.Buffer)
     */
    public int flush(Buffer buffer) throws IOException
    {
        Buffer buf = buffer.buffer();
        int len=0;
        if (buf instanceof NIOBuffer)
        {
            NIOBuffer nbuf = (NIOBuffer)buf;
            ByteBuffer bbuf=nbuf.getByteBuffer();
            
            // TODO synchronize or duplicate?
            synchronized(nbuf)
            {
                try
                {
                    bbuf.position(buffer.getIndex());
                    bbuf.limit(buffer.putIndex());
                    len=_channel.write(bbuf);
                }
                finally
                {
                    buffer.setGetIndex(bbuf.position());
                    bbuf.position(0);
                    bbuf.limit(bbuf.capacity());
                }
            }
        }
        else
        {
            throw new IOException("Not Implemented");
        }
        
        return len;
    }

    /* (non-Javadoc)
     * @see org.mortbay.io.EndPoint#flush(org.mortbay.io.Buffer, org.mortbay.io.Buffer, org.mortbay.io.Buffer)
     */
    public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
    {
        int length=0;

        Buffer buf0 = header==null?null:header.buffer();
        Buffer buf1 = buffer==null?null:buffer.buffer();
        Buffer buf2 = trailer==null?null:trailer.buffer();
        if (_channel instanceof GatheringByteChannel &&
            header!=null && header.length()!=0 && header instanceof NIOBuffer && 
            buffer!=null && buffer.length()!=0 && buffer instanceof NIOBuffer)
        {
            // Try a gather write!
            NIOBuffer nbuf0 = (NIOBuffer)buf0;
            NIOBuffer nbuf1 = (NIOBuffer)buf1;
            NIOBuffer nbuf2 = buf2==null?null:(NIOBuffer)buf2;
            
            ByteBuffer bbuf0=nbuf0.getByteBuffer();
            ByteBuffer bbuf1=nbuf1.getByteBuffer();
            ByteBuffer bbuf2=nbuf2==null?null:nbuf2.getByteBuffer();
            
            
            synchronized(nbuf0)
            {
                synchronized(nbuf1)
                {
                    
                    try
                    {
                        bbuf0.position(header.getIndex());
                        bbuf0.limit(header.putIndex());
                        bbuf1.position(buffer.getIndex());
                        bbuf1.limit(buffer.putIndex());
                        
                        if (bbuf2==null)
                        {
                            synchronized(this)
                            {
                                if (_gather2==null)
                                    _gather2=new ByteBuffer[2];
                                _gather2[0]=bbuf0;
                                _gather2[1]=bbuf1;

                                length=(int)((GatheringByteChannel)_channel).write(_gather2);
                            }
                        }
                        else
                        {
                            synchronized(nbuf2)
                            {
                                try
                                {
                                    bbuf2.position(trailer.getIndex());
                                    bbuf2.limit(trailer.putIndex());

                                    synchronized(this)
                                    {
                                        if (_gather3==null)
                                            _gather3=new ByteBuffer[3];
                                        _gather3[0]=bbuf0;
                                        _gather3[1]=bbuf1;
                                        _gather3[2]=bbuf2;
                                        length=(int)((GatheringByteChannel)_channel).write(_gather3);
                                    }
                                }
                                finally
                                {
                                    buffer.setGetIndex(bbuf2.position());
                                    bbuf2.position(0);
                                    bbuf2.limit(bbuf2.capacity());
                                }
                            }
                        }
                    }
                    finally
                    {
                        header.setGetIndex(bbuf0.position());
                        buffer.setGetIndex(bbuf1.position());
                       
                        bbuf0.position(0);
                        bbuf1.position(0);
                        bbuf0.limit(bbuf0.capacity());
                        bbuf1.limit(bbuf1.capacity());
                    }
                }
            }
        }
        else
        {
        
        if (header!=null && header.length()>0)
            length=flush(header);
        
        if ((header==null || header.length()==0) &&
            buffer!=null && buffer.length()>0)
            length+=flush(buffer);

        if ((header==null || header.length()==0) &&
            (buffer==null || buffer.length()==0) &&
            trailer!=null && trailer.length()>0)
            length+=flush(trailer);
        }
        
        return length;
    }

    /**
     * @return Returns the channel.
     */
    public ByteChannel getChannel()
    {
        return _channel;
    }
    
    /**
     * @param channel The channel to set.
     */
    public void setChannel(ByteChannel channel)
    {
        _channel = channel;
    }
}
