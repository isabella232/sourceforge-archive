// ========================================================================
// $Id$
// Copyright 2003-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
 
package org.mortbay.http.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.HttpServer;
import org.mortbay.util.LogSupport;
import org.mortbay.thread.ThreadPool;

/* ------------------------------------------------------------------------------- */
/**  EXPERIMENTAL NIO listener!
 * 
 * @version $Revision$
 * @author gregw
 */
public class SocketChannelListener extends ThreadPool 
{
    private static Log log= LogFactory.getLog(SocketChannelListener.class);
    
    private InetSocketAddress _address;
    private int _bufferSize= 4096;
    private int _lingerTimeSecs=5;
    
    private transient HttpServer _server;
    private transient ServerSocketChannel _acceptChannel;
    private transient Selector _selector;
    private transient SelectorThread _selectorThread;
    private transient boolean _isLow=false;
    private transient boolean _isOut=false;
    private transient long _warned=0;
    
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     * 
     */
    public SocketChannelListener()
    {
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.http.HttpListener#setHttpServer(org.mortbay.http.HttpServer)
     */
    public void setHttpServer(HttpServer server)
    {
        _server=server;
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.http.HttpListener#getHttpServer()
     */
    public HttpServer getHttpServer()
    {
        return _server;
    }

    /* ------------------------------------------------------------------------------- */
    /**
     * @see org.mortbay.http.HttpListener#setHost(java.lang.String)
     */
    public void setHost(String host) throws UnknownHostException
    {
        _address = new InetSocketAddress(host, _address == null ? 0 : _address.getPort());
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.http.HttpListener#getHost()
     */
    public String getHost()
    {
        if (_address == null || _address.getAddress() == null)
            return null;
        return _address.getHostName();
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.http.HttpListener#setPort(int)
     */
    public void setPort(int port)
    {
        if (_address == null || _address.getHostName() == null)
            _address= new InetSocketAddress(port);
        else
            _address= new InetSocketAddress(_address.getHostName(), port);
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.http.HttpListener#getPort()
     */
    public int getPort()
    {
        if (_address == null)
            return 0;
        return _address.getPort();
    }

    /* ------------------------------------------------------------ */
    public void setBufferSize(int size)
    {
        _bufferSize= size;
    }
    
    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.http.HttpListener#getBufferSize()
     */
    public int getBufferSize()
    {
        return _bufferSize;
    }


    /* ------------------------------------------------------------ */
    /** 
     * @param sec seconds to linger or -1 to disable linger.
     */
    public void setLingerTimeSecs(int ls)
    {
        _lingerTimeSecs= ls;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return seconds.
     */
    public int getLingerTimeSecs()
    {
        return _lingerTimeSecs;
    }
    


    /* ------------------------------------------------------------ */
    public void start() throws Exception
    {
        if (isStarted())
            throw new IllegalStateException("Started");

        // Create a new server socket and set to non blocking mode
        _acceptChannel= ServerSocketChannel.open();
        _acceptChannel.configureBlocking(false);

        // Bind the server socket to the local host and port
        _acceptChannel.socket().bind(_address);

        // Read the address back from the server socket to fix issues
        // with listeners on anonymous ports
        _address= (InetSocketAddress)_acceptChannel.socket().getLocalSocketAddress();

        // create a selector;
        _selector= Selector.open();

        // Register accepts on the server socket with the selector.
        _acceptChannel.register(_selector, SelectionKey.OP_ACCEPT);

        // Start selector thread
        _selectorThread= new SelectorThread();
        _selectorThread.start();

        // Start the thread Pool
        super.start();
        log.info("Started SocketChannelListener on " + getHost()+":"+getPort());
    }
    

    /* ------------------------------------------------------------ */
    public void stop() throws InterruptedException
    {
        if (_selectorThread != null)
            _selectorThread.doStop();
            
        super.stop();
        log.info("Stopped SocketChannelListener on " + getHost()+":"+getPort());
    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class SelectorThread extends Thread
    {
        boolean _running= false;

        /* ------------------------------------------------------------ */
        public void run()
        {
            try
            {
                _running= true;
                while (_running)
                {

                    SelectionKey key= null;
                    try
                    {
                        _selector.select();
                        Iterator iter= _selector.selectedKeys().iterator();

                        while (iter.hasNext())
                        {
                            key= (SelectionKey)iter.next();
                            if (key.isAcceptable())
                                doAccept(key);
                            if (key.isReadable())
                                doRead(key);
                            key= null;
                            iter.remove();
                        }
                    }
                    catch (Exception e)
                    {
                        if (_running)
                            log.warn("selector", e);
                        if (key != null)
                            key.cancel();
                    }
                }
            }
            finally
            {
                log.info("Stopping " + this.getName());

                try
                {
                    if (_acceptChannel != null)
                        _acceptChannel.close();
                }
                catch (IOException e)
                {
                    LogSupport.ignore(log, e);
                }
                try
                {
                    if (_selector != null)
                        _selector.close();
                }
                catch (IOException e)
                {
                    LogSupport.ignore(log, e);
                }

                _selector= null;
                _acceptChannel= null;
                _selectorThread= null;
            }
        }

        /* ------------------------------------------------------------ */
        void doAccept(SelectionKey key)
            throws IOException, InterruptedException
        {          
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel channel = server.accept();
            channel.configureBlocking(false);
            SelectionKey readKey = channel.register(_selector, SelectionKey.OP_READ);
            
            Socket socket=channel.socket();
            try
            {
                if (getMaxIdleTimeMs() >= 0)
                    socket.setSoTimeout(getMaxIdleTimeMs());
                if (_lingerTimeSecs >= 0)
                    socket.setSoLinger(true, _lingerTimeSecs);
                else
                    socket.setSoLinger(false, 0);
            }
            catch (Exception e)
            {
                LogSupport.ignore(log, e);
            }

            Connection connection=new Connection(channel,readKey, SocketChannelListener.this);
            readKey.attach(connection);
        }

        /* ------------------------------------------------------------ */
        void doRead(SelectionKey key) 
            throws IOException
        {
            Connection connection = (Connection)key.attachment();
            
        }   

        void doStop()
        {
            _running=false;
            _selector.wakeup();
            Thread.yield();
        }
    }


    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    private static class Connection
    {
        boolean _idle=true;
        SocketChannel _channel;
        SelectionKey _key;
        SocketChannelListener _listener;
        
        Connection(SocketChannel channel,SelectionKey key, SocketChannelListener listener)
        {
            _channel=channel;
            _key=key;
            _listener=listener;
        }
        
        
    }
    
}
