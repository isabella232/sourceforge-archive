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
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.AbstractHttpListener;
import org.mortbay.http.HttpServer;
import org.mortbay.io.Buffer;
import org.mortbay.io.nio.ChannelEndPoint;
import org.mortbay.io.nio.NIOBuffer;
import org.mortbay.util.LogSupport;
import org.mortbay.http.HttpConnection;

/* ------------------------------------------------------------------------------- */
/**  EXPERIMENTAL NIO listener!
 * 
 * @version $Revision$
 * @author gregw
 */
public class SocketChannelListener extends AbstractHttpListener
{
    private static Log log= LogFactory.getLog(SocketChannelListener.class);

    private transient ServerSocketChannel _acceptChannel;
    private transient SelectionKey _acceptKey;
    private transient Selector _selector;
    private transient ArrayList _keyChanges=new ArrayList();
    
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     * 
     */
    public SocketChannelListener()
    {
    }

    public void open() throws IOException
    {
        if (_acceptChannel==null)
        {
            // Create a new server socket and set to non blocking mode
            _acceptChannel= ServerSocketChannel.open();
            _acceptChannel.configureBlocking(false);

            // Bind the server socket to the local host and port
            _acceptChannel.socket().bind(getAddress());

            // create a selector;
            _selector= Selector.open();

            // Register accepts on the server socket with the selector.
            _acceptKey=_acceptChannel.register(_selector, SelectionKey.OP_ACCEPT);
        }
    }

    public void close() throws IOException
    {
        if (_acceptChannel != null)
            _acceptChannel.close();
        _acceptChannel=null;

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
    }

    /* ------------------------------------------------------------ */
    public void accept()
    	throws IOException
    {      
        // Give other threads a chance to process last loop.
        Thread.yield();
        
        // Make any key changes required

        synchronized(_keyChanges)
        {
            for (int i=0;i<_keyChanges.size();i++)
            {
                try
                {
                    Connection c = (Connection)_keyChanges.get(i);
                    if (c._interestOps>=0)
                        c._key.interestOps(c._interestOps);
                    else
                        c._key.cancel();
                }
                catch(CancelledKeyException e)
                {
                    log.warn("???",e);
                }
            }
            _keyChanges.clear();
        }
        
        
        // SELECT for things to do!
        _selector.select(_maxIdleTime);
        
        // Look for things to do
        Iterator iter= _selector.selectedKeys().iterator();
        while (iter.hasNext())
        {
            SelectionKey key= (SelectionKey)iter.next();
            iter.remove();
            
            try
            {
                if (!key.isValid())
                {
                    key.cancel();
                    continue;
                }
                
                if (key.equals(_acceptKey))
                {
                    if (key.isAcceptable())
                    {
                        SocketChannel channel = _acceptChannel.accept();
                        channel.configureBlocking(false);
                        Socket socket=channel.socket();
                        configure(socket);
                        SelectionKey newKey = channel.register(_selector, SelectionKey.OP_READ);
                        Connection connection=new Connection(channel,newKey);
                    }
                }
                else
                {
                    Connection connection = (Connection)key.attachment();
                    if (connection!=null)
                        connection.dispatch();
                    
                }
                
                key= null;
           
            }
            catch (CancelledKeyException e)
            {
                LogSupport.ignore(log,e);
                key.cancel();
                continue;   
            }
            catch (Exception e)
            {
                if (isStarted())
                    log.warn("selector", e);
                if (key != null && key!=_acceptKey)
                    key.interestOps(0);
            }
        }   
    }

    /* ------------------------------------------------------------------------------- */
    protected Buffer newBuffer(int size)
    {
        return new NIOBuffer(size, true);
    }

    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    private class Connection extends ChannelEndPoint implements Runnable
    {
        boolean _dispatched=false;
        boolean _writable=true;
        SelectionKey _key;
        HttpConnection _connection;
        int _interestOps;
        
        
        Connection(SocketChannel channel,SelectionKey key) 
        {
            super(channel);
            _connection = new HttpConnection(SocketChannelListener.this,this);
            key.attach(this);
            _key=key;
            dispatch();
        }
        
        void dispatch() 
        {
            synchronized(this)
            {
                if (_dispatched)
                {
                    // Still dispatched, so not interested in further selecting.
                    _key.interestOps(0);
                    return;
                }
                
                _dispatched=true;
            }
            
            try
            {
                getHttpServer().dispatch(this);
            }
            catch(InterruptedException e)
            {
                synchronized(this)
                {
                    undispatch();
                }
            }
        }
        
        /**
         */
        private void undispatch()
        {
            try
            {
                _dispatched=false;
                
                if (getChannel().isOpen() && _key.isValid())
                {
                    int ops = _key.interestOps();
                    _interestOps=SelectionKey.OP_READ | (_writable?0:SelectionKey.OP_WRITE);
                    
                    _writable=true;
                    
                    if (_interestOps!=ops)
                    {
                        synchronized(_keyChanges)
                        {
                            _keyChanges.add(this);
                        }
                        _selector.wakeup();
                    }
                }
            }
            catch(Exception e)
            {
                log.error("???",e);
                _interestOps=-1;
                synchronized(_keyChanges)
                {
                    _keyChanges.add(this);
                }
            }
        }

        /* 
         */
        public int fill(Buffer buffer) throws IOException
        {
            int l = super.fill(buffer);
            if (l<0)
                getChannel().close();
            return l;
        }
        
        /*
         */
        public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
        {
            int l0 = (header!=null?header.length():0) +
                     (buffer!=null?buffer.length():0) +
                     (trailer!=null?trailer.length():0);
            int l = super.flush(header, buffer, trailer);
            _writable=l0==l;
            return l;
        }
        
        /*
         */
        public int flush(Buffer buffer) throws IOException
        {
            int l0=buffer.length();
            int l = super.flush(buffer);
            _writable=l==l0;
            return l;
        }
        
        public void run()
        {
            try
            {
                _connection.handle();
            }
            catch(ClosedChannelException e)
            {
                log.debug("handle",e);
            }
            catch(IOException e)
            {
                // TODO - better than this
                if ("BAD".equals(e.getMessage()))
                {
                    log.warn("BAD Request");
                    log.debug("BAD",e);
                }
                else if ("EOF".equals(e.getMessage()))
                    log.debug("EOF",e);
                else
                    log.warn("IO",e);
                _key.cancel();
                try{close();}
                catch(IOException e2){LogSupport.ignore(log, e2);}
            }
            catch(Throwable e)
            {
                log.warn("handle failed",e);
                _key.cancel();
                try{close();}
                catch(IOException e2){LogSupport.ignore(log, e2);}
            }
            finally
            {
                synchronized(this)
                {
                    undispatch();
                }
            }
        }
    }
    
    
    public static void main(String[] arg)
    	throws Exception
    {
        HttpServer server = new HttpServer();
        SocketChannelListener scl = new SocketChannelListener();
        scl.setHttpServer(server);
        scl.start();
        scl.join();
    }
}
