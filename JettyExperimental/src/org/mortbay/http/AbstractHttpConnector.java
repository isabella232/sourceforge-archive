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

package org.mortbay.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import org.apache.ugli.ULogger;
import org.apache.ugli.LoggerFactory;
import org.mortbay.io.Buffer;
import org.mortbay.thread.AbstractLifeCycle;
import org.mortbay.util.LogSupport;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 * 
 * TODO - allow multiple Acceptor threads
 */
public abstract class AbstractHttpConnector extends AbstractLifeCycle implements HttpConnector
{
    private static ULogger log= LoggerFactory.getLogger(AbstractHttpConnector.class);
    
    private final static int BIG_BUF_SIZE=32768;
    private final static int SMALL_BUF_SIZE=1500;

    private String _host;
    private int _port=8080;
    
    protected long _maxIdleTime=30000;  // TODO Configure
    protected long _soLingerTime=1000;  // TODO Configure
    
    private transient SocketAddress _address;
    
    private transient HttpServer _server;
    private transient ArrayList _headerBuffers;
    private transient ArrayList _buffers;
    private transient Thread _acceptorThread;
    
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     * 
     */
    public AbstractHttpConnector()
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

    /* ------------------------------------------------------------------------------- */
    public SocketAddress getAddress()
    {
        if (_address==null)
            _address=(_host==null)?new InetSocketAddress(_port):new InetSocketAddress(_host,_port);
       
        return _address;
    }

    /* ------------------------------------------------------------ */
    protected void doStart() throws Exception
    {
        // open listener port
        open();
        _buffers=new ArrayList();

        // Start selector thread
        _server.dispatch(new Acceptor());
    }
    
    /* ------------------------------------------------------------ */
    protected void doStop() throws Exception
    {
        if (_acceptorThread != null)
            _acceptorThread.interrupt();
        _acceptorThread=null;
        _address=null;
        if (_buffers!=null)
            _buffers.clear();
        _buffers=null;
        
        try{close();} catch(IOException e) {log.warn("stop",e);}
    }

    /* ------------------------------------------------------------ */
    public void join() throws InterruptedException
    {
        if (_acceptorThread!=null)
            _acceptorThread.join();
    }

    /* ------------------------------------------------------------ */
    protected void configure(Socket socket)
    	throws IOException
    {   
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
    }

    /* ------------------------------------------------------------ */
    protected abstract Buffer newBuffer(int size);

    
    
    /* ------------------------------------------------------------ */
    public Buffer getBuffer(boolean big)
    {
        if (big)
        {
            synchronized(_buffers)
            {
                if (_buffers.size()==0)
                    return newBuffer(BIG_BUF_SIZE);
                return (Buffer) _buffers.remove(_buffers.size()-1);
            }
        }
        else
        {
            synchronized(_headerBuffers)
            {
                if (_headerBuffers.size()==0)
                    return newBuffer(SMALL_BUF_SIZE);
                return (Buffer) _headerBuffers.remove(_headerBuffers.size()-1);
            }
        }
        
    }
    

    /* ------------------------------------------------------------ */
    public void returnBuffer(Buffer buffer)
    {
        buffer.clear();
        if (!buffer.isVolatile() && !buffer.isImmutable())
        {
            if (buffer.capacity()==BIG_BUF_SIZE)
            {
                synchronized(_buffers)
                {
                    _buffers.add(buffer);
                }
            }
            else if(buffer.capacity()==SMALL_BUF_SIZE)
            {
                synchronized(_headerBuffers)
                {
                    _headerBuffers.add(buffer);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    protected abstract void accept() throws IOException, InterruptedException;

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "Listener "+getHost()+":"+getPort();
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Acceptor implements Runnable
    {
        /* ------------------------------------------------------------ */
        public void run()
        {   
            _acceptorThread=Thread.currentThread();
            String name =_acceptorThread.getName();
            try
            {
                _acceptorThread.setName(name+" - Acceptor "+AbstractHttpConnector.this);
                while (isRunning())
                {   
                    accept();
                }
            }
            catch(Exception e)
            {
                log.error("select ",e);
            }
            finally
            {
                log.info("Stopping " + this);
                Thread.currentThread().setName(name);
                try
                {
                    close();
                }
                catch (IOException e)
                {
                    log.warn("close", e);
                }
            }
        }

    }
}
