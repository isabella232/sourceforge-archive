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

package org.mortbay.jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

import org.mortbay.io.Buffer;
import org.mortbay.log.LogSupport;
import org.mortbay.thread.AbstractLifeCycle;
import org.mortbay.thread.ThreadPool;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.WaitingContinuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Abstract Connector implementation.
 * This abstract implemenation of the Connector interface provides:<ul>
 * <li>AbstractLifeCycle implementation</li>
 * <li>Implementations for connector getters and setters</li>
 * <li>Buffer management</li>
 * <li>Socket configuration</li>
 * <li>Base acceptor thread</li>
 * </ul>
 * 
 * @author gregw
 *
 * TODO - allow multiple Acceptor threads
 */
public abstract class AbstractConnector extends AbstractLifeCycle implements Connector
{
    private static Logger log= LoggerFactory.getLogger(AbstractConnector.class);

    private int _headerBufferSize=2*1024;
    private int _requestBufferSize=16*1024;
    private int _responseBufferSize=48*1024;

    private ThreadPool _threadPool;
    private Handler _handler;
    private String _host;
    private int _port=8080;
    
    protected long _maxIdleTime=30000; 
    protected long _soLingerTime=1000; 
    
    private transient SocketAddress _address;
    
    private transient ArrayList _headerBuffers;
    private transient ArrayList _requestBuffers;
    private transient ArrayList _responseBuffers;
    private transient Thread _acceptorThread;
    
    
    /* ------------------------------------------------------------------------------- */
    /** 
     */
    public AbstractConnector()
    {
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.jetty.HttpListener#getHttpServer()
     */
    public ThreadPool getThreadPool()
    {
        return _threadPool;
    }

    /* ------------------------------------------------------------------------------- */
    public void setThreadPool(ThreadPool pool)
    {
        _threadPool=pool;
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
     * @see org.mortbay.jetty.HttpListener#setPort(int)
     */
    public void setPort(int port)
    {
        _port=port;
    }

    /* ------------------------------------------------------------------------------- */
    /*
     * @see org.mortbay.jetty.HttpListener#getPort()
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
    /**
     * @return Returns the httpHandler.
     */
    public Handler getHandler()
    {
        return _handler;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param httpHandler The httpHandler to set.
     */
    public void setHandler(Handler handler)
    {
        _handler = handler;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the headerBufferSize.
     */
    public int getHeaderBufferSize()
    {
        return _headerBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param headerBufferSize The headerBufferSize to set.
     */
    public void setHeaderBufferSize(int headerBufferSize)
    {
        _headerBufferSize = headerBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the maxIdleTime.
     */
    public long getMaxIdleTime()
    {
        return _maxIdleTime;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param maxIdleTime The maxIdleTime to set.
     */
    public void setMaxIdleTime(long maxIdleTime)
    {
        _maxIdleTime = maxIdleTime;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the requestBufferSize.
     */
    public int getRequestBufferSize()
    {
        return _requestBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param requestBufferSize The requestBufferSize to set.
     */
    public void setRequestBufferSize(int requestBufferSize)
    {
        _requestBufferSize = requestBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the responseBufferSize.
     */
    public int getResponseBufferSize()
    {
        return _responseBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param responseBufferSize The responseBufferSize to set.
     */
    public void setResponseBufferSize(int responseBufferSize)
    {
        _responseBufferSize = responseBufferSize;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the soLingerTime.
     */
    public long getSoLingerTime()
    {
        return _soLingerTime;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param soLingerTime The soLingerTime to set.
     */
    public void setSoLingerTime(long soLingerTime)
    {
        _soLingerTime = soLingerTime;
    }
    
    /* ------------------------------------------------------------ */
    protected void doStart() throws Exception
    {
        // open listener port
        open();
        
        _headerBuffers=new ArrayList();
        _requestBuffers=new ArrayList();
        _responseBuffers=new ArrayList();
        
        // Start selector thread
        _threadPool.dispatch(new Acceptor());
    }
    
    /* ------------------------------------------------------------ */
    protected void doStop() throws Exception
    {
        if (_acceptorThread != null)
            _acceptorThread.interrupt();
        _acceptorThread=null;
        _address=null;
        
        if (_headerBuffers!=null)
            _headerBuffers.clear();
        _headerBuffers=null;
        if (_requestBuffers!=null)
            _requestBuffers.clear();
        _requestBuffers=null;
        if (_responseBuffers!=null)
            _responseBuffers.clear();
        _responseBuffers=null;
        
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
            socket.setTcpNoDelay(true);
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
    public Buffer getBuffer(int size)
    {
        if (size==_headerBufferSize)
        {
            synchronized(_headerBuffers)
            {
                if (_headerBuffers.size()==0)
                    return newBuffer(size);
                return (Buffer) _headerBuffers.remove(_headerBuffers.size()-1);
            }
        }
        else if (size==_responseBufferSize)
        {
            synchronized(_responseBuffers)
            {
                if (_responseBuffers.size()==0)
                    return newBuffer(size);
                return (Buffer) _responseBuffers.remove(_responseBuffers.size()-1);
            }
        }
        else if (size==_requestBufferSize)
        {
            synchronized(_requestBuffers)
            {
                if (_requestBuffers.size()==0)
                    return newBuffer(size);
                return (Buffer) _requestBuffers.remove(_headerBuffers.size()-1);
            }   
        }
        
        return newBuffer(size);
        
    }
    

    /* ------------------------------------------------------------ */
    public void returnBuffer(Buffer buffer)
    {
        buffer.clear();
        if (!buffer.isVolatile() && !buffer.isImmutable())
        {
            int c=buffer.capacity();
            if (c==_headerBufferSize)
            {
                synchronized(_headerBuffers)
                {
                    _headerBuffers.add(buffer);
                }
            }
            else if (c==_responseBufferSize)
            {
                synchronized(_responseBuffers)
                {
                    _responseBuffers.add(buffer);
                }
            }
            else if (c==_requestBufferSize)
            {
                synchronized(_requestBuffers)
                {
                    _requestBuffers.add(buffer);
                }
            }
        }
    }

    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Connector#getConfidentialPort()
     */
    public int getConfidentialPort()
    {
        return 443;
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Connector#getConfidentialScheme()
     */
    public String getConfidentialScheme()
    {
        return "https";
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Connector#isConfidential(org.mortbay.jetty.Request)
     */
    public boolean isConfidential(Request request)
    {
        return false;
    }
    
    /* ------------------------------------------------------------ */
    protected abstract void accept() throws IOException, InterruptedException;

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "Connector "+getHost()+":"+getPort();
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
            _acceptorThread.setName(name+" - Acceptor "+AbstractConnector.this);
            try
            {
                while (isRunning())
                {
                    try
                    {
                        accept(); 
                    }
                    catch(Exception e)
                    {
                        log.error("select ",e);
                    }
                }
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

    
    public Continuation newContinuation()
    {
        return new WaitingContinuation();
    }
}
