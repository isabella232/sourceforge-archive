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

public class Server extends LifeCycleThread
{
    Selector _selector;
    InetSocketAddress _address;
    ByteBufferPool _bufferPool;
    ArrayList _pending=new ArrayList();
    
    /* ------------------------------------------------------------ */
    public Server()
        throws IOException
    {}
    
    /* ------------------------------------------------------------ */
    public Server(ByteBufferPool pool,InetSocketAddress address)
        throws IOException
    {
        _address=address;
        _bufferPool=pool;
    }
    
    /* ------------------------------------------------------------ */
    public Server(ByteBufferPool pool,InetAddrPort address)
        throws IOException
    {
        _address=new InetSocketAddress(address.getInetAddress(),
                                       address.getPort());
        _bufferPool=pool;
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
    public synchronized void connect(Connection connection)
        throws IOException
    {
        SocketChannel socket_channel= SocketChannel.open();
        socket_channel.configureBlocking(false);
        Code.debug("Connecting... ",socket_channel);

        if (socket_channel.connect(_address))
            connection.connected(socket_channel,_selector);
        
        _pending.add(socket_channel);
        _pending.add(connection);
        
        Code.debug("wakeup ",_selector);
        _selector.wakeup();
    }

    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        
        _selector=Selector.open();
        
        super.start();
    }

    /* ------------------------------------------------------------ */
    public void loop()
        throws Exception
    {
        Code.debug("server keys=",_selector.keys());
        if (_selector.select()>0)
        {
            Set ready=_selector.selectedKeys();
            Iterator iter = ready.iterator();
            while(iter.hasNext())
            {
                SelectionKey key = (SelectionKey)iter.next();
                iter.remove();
                
                Channel channel = key.channel();
                if (Code.debug())
                    Code.debug("Ready key "+key+" for "+channel);

                if (!channel.isOpen())
                    key.cancel();
                else if (channel instanceof SocketChannel)
                {
                    SocketChannel socket_channel=(SocketChannel)channel;
                    Connection connection=(Connection)key.attachment();
                    
                    if ((key.interestOps()&SelectionKey.OP_CONNECT)!=0)
                    {
                        boolean connected=false;
                        
                        try{connected=socket_channel.finishConnect();}
                        catch(Exception e)
                        {
                            if (Code.debug())Code.warning(e);
                            else Log.event(e.toString());
                            key.cancel();
                            connection.deallocate();
                        }
                        
                        if (connected)
                        {
                            connection.connected(socket_channel,_selector);
                            socket_channel.socket().setTcpNoDelay(true);
                            key.interestOps(key.interestOps()&~SelectionKey.OP_CONNECT
                                            |SelectionKey.OP_READ);
                        }
                        else
                            Code.debug("Not Connected ",socket_channel);
                    }
                    else if ((key.interestOps()&SelectionKey.OP_WRITE)!=0)
                        connection.serverWriteWakeup(key);
                    else if ((key.interestOps()&SelectionKey.OP_READ)!=0)
                        connection.server2client(key);

                }
            }
        }

        synchronized(this)
        {
            // Add pending connections.
            for (int i=0;i<_pending.size();i++)
            {
                SocketChannel sc = (SocketChannel)_pending.get(i++);
                Connection c=(Connection)_pending.get(i);
                Code.debug("register ",sc);
                sc.register(_selector,
                            sc.isConnected()
                            ?SelectionKey.OP_WRITE:SelectionKey.OP_CONNECT,
                            c);
            }
            _pending.clear();
        }
    }    
}
