// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.loadbalancer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.LogSupport;

/* ------------------------------------------------------------ */
public class Connection 
{
    static Log log = LogFactory.getLog(Connection.class);
    ByteBufferPool _bufferPool;
    
    private Listener _listener;
    private Server _server;
    private QueuedChannel _serverQ;
    private QueuedChannel _clientQ;
    private int _allocationTry;
    
    /* ------------------------------------------------------------ */
    public Connection(ByteBufferPool bufferPool,
                      Listener listener,
                      SocketChannel client,
                      int capacity)
    {
        _bufferPool=bufferPool;
        _listener=listener;
        _clientQ=new QueuedChannel(capacity,client,listener.getSelector());
        _serverQ=new QueuedChannel(capacity,null,null);
        _clientQ.setReverse(_serverQ);
        _serverQ.setReverse(_clientQ);
    }

    /* ------------------------------------------------------------ */
    public SocketChannel getClientSocketChannel()
    {
        return _clientQ._channel;
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void client2server(SelectionKey key)
        throws IOException
    {
        _serverQ.read(key);
        if (!isAllocated())
            _listener.getPolicy().allocate(this,_serverQ,0);
    }
    
    
    /* ------------------------------------------------------------ */
    public synchronized void serverWriteWakeup(SelectionKey key)
        throws IOException
    {
        _serverQ.writeWakeup(key);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void server2client(SelectionKey key)
        throws IOException
    {
        _clientQ.read(key);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void clientWriteWakeup(SelectionKey key)
        throws IOException
    {
        _clientQ.writeWakeup(key);
    }
    
    
    /* ------------------------------------------------------------ */
    public synchronized void allocate(Server server, int allocationTry)
        throws IOException
    {
        _server=server;
        _allocationTry=allocationTry;
        server.connect(this);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void deallocate()
        throws IOException
    {
        _server=null;
        _serverQ._channel=null;
        _serverQ._selector=null;
        
        _listener.getPolicy().deallocate(this,_serverQ,_allocationTry);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void connected(SocketChannel channel,
                                       Selector selector)
        throws IOException
    {
        _serverQ._channel=channel;
        _serverQ._selector=selector;
        if (!_serverQ.isEmpty())
            _serverQ.writeWakeup(null);
        if(log.isDebugEnabled())log.debug("Connect "+this);
    }
    
    /* ------------------------------------------------------------ */
    public boolean isAllocated()
    {
        return _server!=null;
    }

    /* ------------------------------------------------------------ */
    public synchronized void close()
    {
        if(log.isDebugEnabled())log.debug("Closing "+this);
        try{
            if (_clientQ._channel!=null && _clientQ._channel.isOpen())
            {
                _clientQ._channel.socket().setTcpNoDelay(true);
                _clientQ._channel.socket().shutdownOutput();
                _clientQ._channel.socket().close();
                _clientQ._channel.close();
            }
        }
        catch(IOException e){log.warn(LogSupport.EXCEPTION,e);}
        
        try{
            if (_serverQ._channel!=null && _serverQ._channel.isOpen())
            {
                _serverQ._channel.close();
            }
        }
        catch(IOException e){log.warn(LogSupport.EXCEPTION,e);}
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return 
            (_clientQ._channel!=null
             ?(_clientQ._channel.socket().getRemoteSocketAddress()+
               "-->"+
               _clientQ._channel.socket().getLocalPort())
             :"?-->?")
            + "-->" +
            (_serverQ._channel!=null
             ?_serverQ._channel.socket().getRemoteSocketAddress().toString()
             :"?");
    }
    
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** A queue to a writable channel and selector.
     */
    private class QueuedChannel extends NonBlockingQueue
    {
        SocketChannel _channel;
        Selector _selector;
        QueuedChannel _reverse;

        /* ------------------------------------------------------------ */
        QueuedChannel(int capacity,
                      SocketChannel channel,
                      Selector selector)
        {
            super(capacity);
            _channel=channel;
            _selector=selector;
        }
        
        /* ------------------------------------------------------------ */
        void setReverse(QueuedChannel reverse)
        {
            _reverse=reverse;
        }
        
        /* ------------------------------------------------------------ */
        synchronized void read(SelectionKey key)
            throws IOException
        {
            if(log.isDebugEnabled())log.debug("Read "+key);
            // Do we have space?
            if (isFull())
                // No - unregister READs from src
                key.interestOps(~SelectionKey.OP_READ&key.interestOps());
            else
            {
                // Read/Write a Buffers.
                SocketChannel socket_channel=(SocketChannel)key.channel();
                ByteBuffer buffer = _bufferPool.get();
                int l=-1;
                while (!isFull() && (l=socket_channel.read(buffer))>0)
                {
                    // write it
                    buffer.flip();
                    write(buffer);
                    buffer = _bufferPool.get();
                }
                _bufferPool.add(buffer);
                if (l<0)
                    close();
            }
        }
        
        /* ------------------------------------------------------------ */
        synchronized void write(ByteBuffer buffer)
            throws IOException
        {
            if (buffer.remaining()==0)
            {
                _bufferPool.add(buffer);
                return;
            }
            
            if (isFull())
                throw new IllegalStateException("Full");

            // Are we using the queue
            if (!isEmpty() || _channel==null)
            {
                if(log.isDebugEnabled())log.debug("QUEUE! "+buffer);
                queue(buffer);
            }
            else
            {
                // No - write buffer directly
                if(log.isDebugEnabled())log.debug("Write! "+buffer);

                if (_channel.write(buffer)<0)
                {
                    _bufferPool.add(buffer);
                    close();
                }
                
                // If we have bytes remaining
                if (buffer.remaining()>0)
                {
                    if(log.isDebugEnabled())log.debug("QUEUE "+buffer);
                    // Queue it
                    queue(buffer);
                    synchronized(_selector)
                    {
                        SelectionKey key=_channel.keyFor(_selector);
                        if (key!=null)
                            key.interestOps(SelectionKey.OP_WRITE|key.interestOps());
                        else                    
                            key=_channel.register(_selector,
                                                  SelectionKey.OP_WRITE,
                                                  Connection.this);
                        _selector.wakeup();
                    }
                }
                else
                    _bufferPool.add(buffer);
            }
        }
    

    
        /* ------------------------------------------------------------ */
        synchronized void writeWakeup(SelectionKey key)
            throws IOException
        {
            if(log.isDebugEnabled())log.debug("WRITE WAKEUP: "+key);
            
            boolean was_full = isFull();

            if (log.isDebugEnabled())
                if(log.isDebugEnabled())log.debug("was_full=="+was_full+" isEmpty()=="+isEmpty());
            
            // While we have buffers to write
            while (!isEmpty())
            {
                ByteBuffer buffer = (ByteBuffer)peek();
                if(log.isDebugEnabled())log.debug("Write  "+buffer);
                int len=_channel.write(buffer);
                
                if (len<0)
                {
                    _bufferPool.add(buffer);
                    close();
                    return;
                }
                else if (buffer.remaining()==0)
                {
                    // consume the buffer
                    next();
                    _bufferPool.add(buffer);
                }
                else
                    break;
            }

            if (key!=null&& isEmpty())
                key.interestOps(~SelectionKey.OP_WRITE&key.interestOps());
            
            if (was_full)
                _reverse.readRegister();
        }

        /* ------------------------------------------------------------ */
        void readRegister()
            throws IOException
        {
            if(log.isDebugEnabled())log.debug("READ REGISTER: "+this);
            
            SelectionKey key=_channel.keyFor(_selector);
            if (key!=null)
                key.interestOps(SelectionKey.OP_READ|key.interestOps());
            else                    
                _channel.register(_selector,
                                  SelectionKey.OP_READ,
                                  Connection.this);
            _selector.wakeup();
        }
    
        
        /* ------------------------------------------------------------ */
        void close()
        {
            Connection.this.close();
        }
    }
}

    
    
    
