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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.io.Buffer;
import org.mortbay.util.LogSupport;

/**
 * @author gregw
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class AbstractHttpListener implements HttpListener
{
    private static Log log= LogFactory.getLog(AbstractHttpListener.class);

    private String _host;
    private int _port=8080;
    
    protected long _maxIdleTime=30000;  // TODO Configure
    protected long _soLingerTime=1000;  // TODO Configure
    
    private transient SocketAddress _address;
    
    private transient HttpServer _server;
    private transient Thread _thread;
    private transient boolean _started;
    private transient ArrayList _buffers;
    
    
    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     * 
     */
    public AbstractHttpListener()
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
    public void start() throws Exception
    {
        if (isStarted())
            throw new IllegalStateException("Started");

        // open listener port
        open();

        // Start selector thread
        _thread = new ListenerThread();
        _thread.setName("listener");
        _thread.start();
        _buffers=new ArrayList();
        _started=true;

        log.info("Started " + this);
    }

    /* ------------------------------------------------------------ */
    /*
     */
    public boolean isStarted()
    {
        return _started && _thread!=null && _thread.isAlive();
    }
    
    /* ------------------------------------------------------------ */
    public void stop() throws InterruptedException
    {
        _started=false;
        if (_thread != null)
            _thread.interrupt();
        _thread=null;
        _address=null;
        if (_buffers!=null)
            _buffers.clear();
        _buffers=null;
        
        try{close();} catch(IOException e) {log.warn("stop",e);}
        log.info("Stopping " + this);
    }

    /* ------------------------------------------------------------ */
    public void join() throws InterruptedException
    {
        if (_thread!=null)
            _thread.join();
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
    public Buffer getBuffer()
    {
        synchronized(_buffers)
        {
            if (_buffers.size()==0)
                return newBuffer(8192);
            return (Buffer) _buffers.remove(_buffers.size()-1);
        }
    }
    

    /* ------------------------------------------------------------ */
    protected abstract Buffer newBuffer(int size);

    /* ------------------------------------------------------------ */
    public void returnBuffer(Buffer buffer)
    {
        buffer.clear();
        if (!buffer.isVolatile() && !buffer.isImmutable())
        {
            synchronized(_buffers)
            {
                _buffers.add(buffer);
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
    private class ListenerThread extends Thread
    {
        /* ------------------------------------------------------------ */
        public void run()
        {   
            try
            {
                while (isStarted())
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
                log.info("Stopping " + this.getName());

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
