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

public class Listener extends LifeCycleThread
{
    Policy _policy;
    Selector _selector;
    ServerSocketChannel _acceptChannel;
    InetSocketAddress _address;
    ByteBufferPool _bufferPool ;
    
    /* ------------------------------------------------------------ */
    public Listener()
        throws IOException
    {
    }
    
    /* ------------------------------------------------------------ */
    public Listener(ByteBufferPool pool,
                    InetSocketAddress address,
                    Policy policy)
        throws IOException
    {
        _address=address;
        _bufferPool=pool;
        _policy=policy;
    }
    
    /* ------------------------------------------------------------ */
    public Listener(ByteBufferPool pool,
                    InetAddrPort address,
                    Policy policy)
        throws IOException
    {
        _address=new InetSocketAddress(address.getInetAddress(),
                                       address.getPort());
        _bufferPool=pool;
        _policy=policy;
    }

    /* ------------------------------------------------------------ */
    public Selector getSelector()
    {
        return _selector;
    }
    
    /* ------------------------------------------------------------ */
    public InetSocketAddress getInetSocketAddress()
    {
        return _address;
    }
    
    /* ------------------------------------------------------------ */
    public void setInetSocketAddress(InetSocketAddress address)
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        _address=address;
    }
    
    /* ------------------------------------------------------------ */
    public ByteBufferPool getBufferPool()
    {
        return _bufferPool;
    }

    /* ------------------------------------------------------------ */
    public void setBufferPool(ByteBufferPool bufferPool)
    {
        _bufferPool = bufferPool;
    }

    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        if (_bufferPool==null)
            throw new IllegalStateException("No BufferPool");
        
	// Create a new server socket and set to non blocking mode
	_acceptChannel=ServerSocketChannel.open();
	_acceptChannel.configureBlocking(false);

	// Bind the server socket to the local host and port
	_acceptChannel.socket().bind(_address);

        System.err.println("Bound "+_acceptChannel);
        
        // create a selector;
        _selector=Selector.open();
        
	// Register accepts on the server socket with the selector. This
	// step tells the selector that the socket wants to be put on the
	// ready list when accept operations occur, so allowing multiplexed
	// non-blocking I/O to take place.
        _acceptChannel.register(_selector,SelectionKey.OP_ACCEPT);

        System.err.println("Selector "+_selector);
        
        super.start();
        
    }

    /* ------------------------------------------------------------ */
    public void loop()
        throws Exception
    {
        System.err.println("client keys="+_selector.keys());
        if (_selector.select()>0)
        {
            Set ready=_selector.selectedKeys();
            System.err.println("\nclient ready="+ready);
            Iterator iter = ready.iterator();
            while(iter.hasNext())
            {
                SelectionKey key = (SelectionKey)iter.next();
                iter.remove();

                Channel channel = key.channel();
                System.err.println("Ready key "+key+
                                   " for "+channel);

                if (!channel.isOpen())
                    key.cancel();
                else if (channel instanceof ServerSocketChannel)
                {
                    SocketChannel socket_channel =(SocketChannel)
                        ((ServerSocketChannel)channel).accept();
                    System.err.println("Accepted "+socket_channel);
                    socket_channel.configureBlocking(false);

                    Connection connection=
                        new Connection(_bufferPool,
                                       this,
                                       socket_channel,
                                       16);
                    _policy.allocate(connection);
                    socket_channel.register(_selector,
                                            SelectionKey.OP_READ,
                                            connection);
                }
                else if (channel instanceof SocketChannel)
                {
                    Connection connection=(Connection)key.attachment();

                    if ((key.interestOps()&SelectionKey.OP_WRITE)!=0)
                        connection.clientWriteWakeup(key);
                    else if ((key.interestOps()&SelectionKey.OP_READ)!=0)
                        connection.client2server(key);
                }       
            }
        }
    }
}

