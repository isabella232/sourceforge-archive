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
 
package org.mortbay.http.bio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
import org.mortbay.http.HttpServer;
import org.mortbay.io.Buffer;
import org.mortbay.io.bio.SocketEndPoint;
import org.mortbay.io.nio.ChannelEndPoint;
import org.mortbay.thread.LifeCycle;
import org.mortbay.util.LogSupport;
import org.mortbay.http.HttpConnection;

/* ------------------------------------------------------------------------------- */
/**  EXPERIMENTAL NIO listener!
 * 
 * @version $Revision$
 * @author gregw
 */
public class SocketListener implements LifeCycle 
{
    private static Log log= LogFactory.getLog(SocketListener.class);

    private String _host;
    private int _port=8080;
    
    private long _maxIdleTime=30000;  // TODO Configure
    private long _soLingerTime=1000;  // TODO Configure
    
    private transient InetSocketAddress _address;
    private transient HttpServer _server;
    private transient ServerSocket _acceptSocket;
    private transient AcceptorThread _acceptorThread;
    
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     * 
     */
    public SocketListener()
    {
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.http.HttpListener#getHttpServer()
     */
    public HttpServer getHttpServer()
    {
        return _server;
    }
    
    public void setHttpServer(HttpServer server)
    {
        _server=server;
    }
    
    /* ------------------------------------------------------------------------------- */
    /**
     */
    public void setHost(String host) 
    {
        _host=host;
    }

    /* ------------------------------------------------------------------------------- */
    /*
     */
    public String getHost()
    {
        return _host;
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.http.HttpListener#setPort(int)
     */
    public void setPort(int port)
    {
        _port=port;
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.http.HttpListener#getPort()
     */
    public int getPort()
    {
        return _port;
    }


    /* ------------------------------------------------------------ */
    public void start() throws Exception
    {
        if (isStarted())
            throw new IllegalStateException("Started");

        // resolve address
        _address=(_host==null)?new InetSocketAddress(_port):new InetSocketAddress(_host,_port);
        
        // Create a new server socket and set to non blocking mode
        _acceptSocket= new ServerSocket();

        // Bind the server socket to the local host and port
        _acceptSocket.bind(_address);

        // Read the address back from the server socket to fix issues
        // with listeners on anonymous ports
        _address= (InetSocketAddress)_acceptSocket.getLocalSocketAddress();


        // Start selector thread
        _acceptorThread = new AcceptorThread();
        _acceptorThread.start();

        log.info("Started SocketListener on " + getHost()+":"+getPort());
    }
    

    /* ------------------------------------------------------------ */
    /*
     */
    public boolean isStarted()
    {
        return _acceptSocket!=null && !_acceptSocket.isClosed();
    }
    
    /* ------------------------------------------------------------ */
    public void stop() throws InterruptedException
    {
        if (_acceptorThread != null)
            _acceptorThread.stopSelection();
            
        log.info("Stopped SocketListener on " + getHost()+":"+getPort());
    }

    /* ------------------------------------------------------------ */
    public Socket accept()
    	throws IOException
    {   
        Socket socket = _acceptSocket.accept();

        try
        {
            if (_maxIdleTime >= 0)
                socket.setSoTimeout((int)_maxIdleTime);
            if (_soLingerTime >= 0)
                socket.setSoLinger(true, (int)_soLingerTime/1000);
            else
                socket.setSoLinger(false, 0);
        }
        catch (Exception e)
        {
            LogSupport.ignore(log, e);
        }
        
        return socket;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class AcceptorThread extends Thread
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
                    // Give other threads a chance to process last loop.
                    Thread.yield();
                    
                    Socket socket = accept();
                    Connection connection=new Connection(socket);
                    connection.dispatch();
                }
            }
            catch(Exception e)
            {
                log.error("select ",e);
            }
            finally
            {
                log.info("Stopping " + this.getName());

                try
                {
                    if (_acceptSocket != null)
                        _acceptSocket.close();
                }
                catch (IOException e)
                {
                    LogSupport.ignore(log, e);
                }
                _acceptSocket= null;
                _acceptorThread= null;
            }
        }

        void stopSelection()
        {
            _running=false;
            Thread.yield();
        }
    }


    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    /* ------------------------------------------------------------------------------- */
    private class Connection extends SocketEndPoint implements Runnable
    {
        boolean _dispatched=false;
        HttpConnection _connection;
        
        
        Connection(Socket socket) throws IOException
        {
            super(socket);
            _connection = new HttpConnection(_server,this);
        }
        
        void dispatch() throws InterruptedException
        {
            _server.dispatch(this);
        }
        
        /* 
         */
        public int fill(Buffer buffer) throws IOException
        {
            int l = super.fill(buffer);
            if (l<0)
                close();
            return l;
        }
        
        
        public void run()
        {
            try
            {
                while (!isClosed())
                    _connection.handle();
            }
            catch(IOException e)
            {
                // TODO - better than this
                if ("BAD".equals(e.getMessage()))
                    log.warn("BAD Request");
                else
                    log.warn("IO",e);
                try{close();}
                catch(IOException e2){LogSupport.ignore(log, e2);}
            }
            catch(Throwable e)
            {
                log.warn("handle failed",e);
                try{close();}
                catch(IOException e2){LogSupport.ignore(log, e2);}
            }
            finally
            {
            }
        }
    }
    
    
    public static void main(String[] arg)
    	throws Exception
    {
        HttpServer server = new HttpServer();
        SocketListener scl = new SocketListener();
        scl.setHttpServer(server);
        scl.start();
        scl._acceptorThread.join();
        
    }
    
    

    
}
