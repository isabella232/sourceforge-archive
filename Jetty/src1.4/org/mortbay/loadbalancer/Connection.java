// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.loadbalancer;

import org.mortbay.util.*;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/* ------------------------------------------------------------ */
public class Connection 
{
    private ByteBufferPool _bufferPool;
    private Listener _listener;
    private Server _server;
    private QueuedChannel _serverQ;
    private QueuedChannel _clientQ;
    
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
            _listener.getPolicy().allocate(this,_serverQ);
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
    public synchronized void connected(SocketChannel channel,
                                       Selector selector)
        throws IOException
    {
        _serverQ._channel=channel;
        _serverQ._selector=selector;
        if (!_serverQ.isEmpty())
            _serverQ.writeWakeup(null);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void allocate(Server server)
        throws IOException
    {
        _server=server;
        server.connect(this);            
    }

    /* ------------------------------------------------------------ */
    public boolean isAllocated()
    {
        return _server!=null &&
            _serverQ!=null &&
            _serverQ._channel!=null &&
            _serverQ._channel.isOpen();
    }
    
    /* ------------------------------------------------------------ */
    public void close()
    {
        try{_clientQ._channel.close();}catch(IOException e){Code.warning(e);}
        try{_serverQ._channel.close();}catch(IOException e){Code.warning(e);}
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
            System.err.println("Read "+key);
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

            if (key.isValid())
                System.err.println(key+" interested in "+key.interestOps());

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
                System.err.println("QUEUE! "+buffer);
                queue(buffer);
            }
            else
            {
                // No - write buffer directly
                System.err.println("Write! "+buffer);

                if (_channel.write(buffer)<0)
                {
                    _bufferPool.add(buffer);
                    close();
                }
                
                // If we have bytes remaining
                if (buffer.remaining()>0)
                {
                    System.err.println("QUEUE "+buffer);
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
                        System.err.println(key+" interested in "+key.interestOps());
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
            System.err.println("WRITE WAKEUP: "+key);
            
            boolean was_full = isFull();
            
            System.err.println("was_full=="+was_full+
                               "\nisEmpty()=="+isEmpty());
            
            // While we have buffers to write
            while (!isEmpty())
            {
                ByteBuffer buffer = (ByteBuffer)peek();
                System.err.println("Write  "+buffer);
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
            {
                System.err.println("All written");
                key.interestOps(~SelectionKey.OP_WRITE&key.interestOps());
                System.err.println(key+" interested in "+key.interestOps());
            }
            else
                System.err.println("Some written");
            
            if (was_full)
                _reverse.readRegister();
        }

        /* ------------------------------------------------------------ */
        void readRegister()
            throws IOException
        {
            System.err.println("READ REGISTER: ");
            
            SelectionKey key=_channel.keyFor(_selector);
            if (key!=null)
                key.interestOps(SelectionKey.OP_READ|key.interestOps());
            else                    
                _channel.register(_selector,
                                  SelectionKey.OP_READ,
                                  Connection.this);
            _selector.wakeup();
            System.err.println(key+" interested in "+key.interestOps());
        }
    
        
        /* ------------------------------------------------------------ */
        void close()
        {
            System.err.println("CLOSE: "+this+" in "+Connection.this);
            Connection.this.close();
        }
    }
}

    
    
    
