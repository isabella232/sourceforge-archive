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

import org.apache.ugli.ULogger;
import org.apache.ugli.LoggerFactory;
import org.mortbay.http.AbstractHttpConnector;
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
public class SocketChannelConnector extends AbstractHttpConnector
{
    private static ULogger log= LoggerFactory.getLogger(SocketChannelConnector.class);

    private transient ServerSocketChannel _acceptChannel;
    private transient SelectionKey _acceptKey;
    private transient Selector _selector;
    private transient ArrayList _keyChanges=new ArrayList();
    private transient boolean _idle;    
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     * 
     */
    public SocketChannelConnector()
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
        // TODO ? Thread.yield();
        
        // Make any key changes required
        synchronized(_keyChanges)
        {
            for (int i=0;i<_keyChanges.size();i++)
            {
                try
                {
                    HttpEndPoint c = (HttpEndPoint)_keyChanges.get(i);
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
        boolean dispatched=false;
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
                        HttpEndPoint connection=new HttpEndPoint(channel,newKey);
                        
                        // assume something to do
                        dispatched=connection.dispatch()||dispatched;
                    }
                }
                else
                {
                    HttpEndPoint connection = (HttpEndPoint)key.attachment();
                    if (connection!=null)
                        dispatched=connection.dispatch()||dispatched;    
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
                if (isRunning())
                    log.warn("selector", e);
                if (key != null && key!=_acceptKey)
                    key.interestOps(0);
            }
        }   
        _idle=!dispatched;
    }

    /* ------------------------------------------------------------------------------- */
    protected Buffer newBuffer(int size)
    {
        return new NIOBuffer(size, true);
    }

    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    private class HttpEndPoint extends ChannelEndPoint implements Runnable
    {
        boolean _dispatched=false;
        boolean _writable=true;  // TODO - get rid of this bad side effect
        SelectionKey _key;
        HttpConnection _connection;
        int _interestOps;
        int _readBlocked;
        int _writeBlocked;
        

        /* ------------------------------------------------------------ */
        HttpEndPoint(SocketChannel channel,SelectionKey key) 
        {
            super(channel);
            _connection = new HttpConnection(SocketChannelConnector.this,this, null);
            key.attach(this);
            _key=key;
        }

        /* ------------------------------------------------------------ */
        /** Dispatch the endpoint by arranging for a thread to service it.
         * Either a blocked thread is woken up or the endpoint is passed to the server job queue.
         * If the thread is already dispatched and the server is otherwise idle, then the selection key 
         * is modified  so that it is no longer selected.
         */
        boolean dispatch() 
        {
            synchronized(this)
            {
                // If threads are blocked on this
                if (_readBlocked>0 || _writeBlocked>0)
                {
                    // wake them up is as good as a dispatched.
                    this.notifyAll();

                    // then we are not interested in further selecting if we are idle
                    if (_idle)
                        _key.interestOps(0);
                    return true;
                }
                
                // Otherwise if we are still dispatched
                if (_dispatched)
                {
                    // then we are not interested in further selecting if we are idle
                    if (_idle)
                        _key.interestOps(0);
                    return false;
                }
                
                // Remove writeable op
                if ((_key.readyOps()|SelectionKey.OP_WRITE)!=0 &&
                    (_key.interestOps()|SelectionKey.OP_WRITE)!=0)
                    _key.interestOps(_interestOps=_key.interestOps()&(-1^SelectionKey.OP_WRITE));
                
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
            return true;
        }

        /* ------------------------------------------------------------ */
        /** Called when a dispatched thread is no longer handling the endpoint.
         * The selection key operations are updated.
         */
        private void undispatch()
        {
            try
            {
                _dispatched=false;
                
                if (getChannel().isOpen() && _key.isValid())
                    updateKey();
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

        /* ------------------------------------------------------------ */
        /* 
         */
        public int fill(Buffer buffer) throws IOException
        {
            int l = super.fill(buffer);
            if (l<0)
                getChannel().close();
            return l;
        }

        /* ------------------------------------------------------------ */
        /*
         */
        public int flush(Buffer header, Buffer buffer, Buffer trailer) throws IOException
        {
            int l = super.flush(header, buffer, trailer);
            _writable=l>0;
            return l;
        }

        /* ------------------------------------------------------------ */
        /*
         */
        public int flush(Buffer buffer) throws IOException
        {
            int l = super.flush(buffer);
            _writable=l>0;
            return l;
        }
        
        /* ------------------------------------------------------------ */
        /* Allows thread to block waiting for further events.
         */
        public void blockReadable(long timeoutMs)
        {
            synchronized(this)
            {
                if (getChannel().isOpen() && _key.isValid())
                {
                    try
                    {
                        _readBlocked++;
                        updateKey();
                        this.wait(timeoutMs);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        _readBlocked--;
                    }
                }
            }
        }

        /* ------------------------------------------------------------ */
        /* Allows thread to block waiting for further events.
         */
        public void blockWritable(long timeoutMs)
        {
            synchronized(this)
            {
                if (getChannel().isOpen() && _key.isValid())
                {
                    try
                    {
                        _writeBlocked++;
                        updateKey();
                        this.wait(timeoutMs);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    finally
                    {
                        _writeBlocked--;
                    }
                }
            }
        }

        /* ------------------------------------------------------------ */
        /** Updates selection key.
         * Adds operations types to the selection key as needed.
         * No operations are removed as this is only done during dispatch
         */
        private void updateKey()
        {
            synchronized(this)
            {
                int ops = _key.interestOps();
                _interestOps=ops | 
                ((!_dispatched||_readBlocked>0)?SelectionKey.OP_READ:0) | 
                ((!_writable||_writeBlocked>0)?SelectionKey.OP_WRITE:0);
                _writable=true; // Once writable is in ops, only removed with dispatch.
                                
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
        
        /* ------------------------------------------------------------ */
        /* 
         */
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
        server.start();
        SocketChannelConnector scl = new SocketChannelConnector();
        scl.setHttpServer(server);
        scl.start();
        scl.join();
    }
}
