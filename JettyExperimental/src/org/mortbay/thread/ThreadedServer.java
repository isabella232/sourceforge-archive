// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.thread;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.LogSupport;

/* ======================================================================= */
/** Threaded socket server.
 * This class listens at a socket and gives the connections received
 * to a pool of Threads
 * <P>
 * The class is abstract and derived classes must provide the handling
 * for the connections.
 * <P>
 * The properties THREADED_SERVER_MIN_THREADS and THREADED_SERVER_MAX_THREADS
 * can be set to control the number of threads created.
 * <P>
 * @version $Id$
 * @author Greg Wilkins
 */
abstract public class ThreadedServer extends ThreadPool
{
    private static Log log = LogFactory.getLog(ThreadedServer.class);
    
    public final static String ANY_IP= "0.0.0.0";

    /* ------------------------------------------------------------------- */
    private InetAddress _inetAddress;
    private String _host;
    private int _port;

    private int _soTimeOut= -1;
    private int _lingerTimeSecs= 30;
    private boolean _tcpNoDelay= true;

    private transient Acceptor _acceptor= null;
    private transient ServerSocket _listen= null;
    private transient boolean _running= false;

    /* ------------------------------------------------------------------- */
    /* Construct
     */
    public ThreadedServer()
    {
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return The ServerSocket
     */
    public ServerSocket getServerSocket()
    {
        return _listen;
    }

    /* ------------------------------------------------------------------- */
    /** Construct for specific port.
     */
    public ThreadedServer(int port)
    {
        setPort(port);
    }

    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port.
     */
    public ThreadedServer(String host, int port) throws UnknownHostException
    {
        setHost(host);
        setPort(port);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param host 
     */
    public synchronized void setHost(String host) throws UnknownHostException
    {
        if (_host != null && _host.equals(host))
            return;
        _host= host;
        if (ANY_IP.equals(host) || host == null)
            _inetAddress= null;
        else
            _inetAddress= InetAddress.getByName(_host);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return Host name
     */
    public String getHost()
    {
        return _host;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return IP Address
     */
    public InetAddress getInetAddress()
    {
        return _inetAddress;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param port 
     */
    public synchronized void setPort(int port)
    {
        _port= port;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return port number
     */
    public int getPort()
    {
        return _port;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param ls seconds to linger or -1 to disable linger.
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
    /** 
     * @param tcpNoDelay if true then setTcpNoDelay(true) is called on accepted sockets.
     */
    public void setTcpNoDelay(boolean tcpNoDelay)
    {
        _tcpNoDelay= tcpNoDelay;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return true if setTcpNoDelay(true) is called on accepted sockets.
     */
    public boolean getTcpNoDelay()
    {
        return _tcpNoDelay;
    }

    /* ------------------------------------------------------------------- */
    /** Handle new connection.
     * This method should be overridden by the derived class to implement
     * the required handling.  It is called by a thread created for it and
     * does not need to return until it has finished it's task
     */
    protected void handleConnection(InputStream in, OutputStream out)
    {
        throw new Error("Either handlerConnection must be overridden");
    }

    /* ------------------------------------------------------------------- */
    /** Handle new connection.
     * If access is required to the actual socket, override this method
     * instead of handleConnection(InputStream in,OutputStream out).
     * The default implementation of this just calls
     * handleConnection(InputStream in,OutputStream out).
     */
    protected void handleConnection(Socket connection) throws IOException
    {
        InputStream in= connection.getInputStream();
        OutputStream out= connection.getOutputStream();

        handleConnection(in, out);
        out.flush();

        in= null;
        out= null;
        connection.close();
    }

    /* ------------------------------------------------------------ */
    /** Handle Job.
     * Implementation of ThreadPool.handle(), calls handleConnection.
     * @param job A Connection.
     */
    public void handle(Object job)
    {
        Socket socket= (Socket)job;
        try
        {
            if (_tcpNoDelay)
                socket.setTcpNoDelay(true);
            handleConnection(socket);
        }
        catch (IOException e)
        {
            log.warn(LogSupport.EXCEPTION,e);
        }
        finally
        {
            try
            {
                socket.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** New server socket.
     * Creates a new servers socket. May be overriden by derived class
     * to create specialist serversockets (eg SSL).
     * @param address Address and port
     * @param acceptQueueSize Accept queue size
     * @return The new ServerSocket
     * @exception java.io.IOException 
     */
    protected ServerSocket newServerSocket(InetAddress inetAddress, int port, int acceptQueueSize)
        throws java.io.IOException
    {
        if (inetAddress == null && port == 0)
            return new ServerSocket(0, acceptQueueSize);

        return new ServerSocket(port, acceptQueueSize, inetAddress);
    }

    /* ------------------------------------------------------------ */
    /** Accept socket connection.
     * May be overriden by derived class
     * to create specialist serversockets (eg SSL).
     * @param serverSocket
     * @param timeout The time to wait for a connection. Normally
     *                 passed the ThreadPool maxIdleTime.
     * @return Accepted Socket
     */
    protected Socket acceptSocket(ServerSocket serverSocket, int timeout) throws IOException
    {
        Socket s= null;

        if (_soTimeOut != timeout)
        {
            _soTimeOut= timeout;
            _listen.setSoTimeout(_soTimeOut);
        }

        if (_listen != null)
        {
            s= _listen.accept();

            if (getMaxIdleTimeMs() >= 0)
                s.setSoTimeout(getMaxIdleTimeMs());
            if (_lingerTimeSecs >= 0)
                s.setSoLinger(true, _lingerTimeSecs);
            else
                s.setSoLinger(false, 0);
        }
        return s;
    }

    /* ------------------------------------------------------------------- */
    /** Open the server socket.
     * This method can be called to open the server socket in advance of starting the
     * listener. This can be used to test if the port is available.
     *
     * @exception IOException if an error occurs
     */
    public void open() throws IOException
    {
        if (_listen == null)
        {
            _listen= newServerSocket(_inetAddress, _port, (getMaxThreads() > 0 ? (getMaxThreads() + 1) : 50));

            _soTimeOut= getMaxIdleTimeMs();
            if (_soTimeOut >= 0)
                _listen.setSoTimeout(_soTimeOut);
        }
    }

    /* ------------------------------------------------------------------- */
    /* Start the ThreadedServer listening
     */
    synchronized public void start() throws Exception
    {
        if (isStarted())
            return;

        open();

        _running= true;
        _acceptor= new Acceptor();
        _acceptor.setDaemon(isDaemon());
        _acceptor.start();

        super.start();
        
       log.info("Started "+this);
    }

    /* --------------------------------------------------------------- */
    public void stop() throws InterruptedException
    {
        synchronized (this)
        {
            // Signal that we are stopping
            _running= false;

            // Close the listener socket.
            try
            {
                if (_listen != null)
                    _listen.close();
            }
            catch (IOException e)
            {
                log.warn(LogSupport.EXCEPTION,e);
            }

            // Do we have an acceptor thread (running or not)
            Thread.yield();
            if (_acceptor != null)
            {
                // Tell the acceptor to exit and wake it up
                _acceptor.interrupt();
                wait(getMaxIdleTimeMs());

                // Do we still have an acceptor thread? It is playing hard to stop!
                // Try forcing the stop to be noticed by making a connection to self.
                if (_acceptor != null)
                {
                    _acceptor.forceStop();
                    // Assume that worked and go on as if it did.
                    _acceptor= null;
                }
            }
        }

        // Stop the thread pool
        try
        {
            super.stop();
        }
        catch (Exception e)
        {
            log.warn(LogSupport.EXCEPTION,e);
        }

        // Clean up
        _listen= null;
        _acceptor= null;

        log.info("Stopped "+this);
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Kill a job.
     * This method closes IDLE and socket associated with a job
     * @param thread 
     * @param job 
     */
    protected void stopJob(Thread thread, Object job)
    {
        if (job instanceof Socket)
        {
            try
            {
                ((Socket)job).close();
            }
            catch (Exception e)
            {
                log.warn(LogSupport.EXCEPTION,e);
            }
        }
        super.stopJob(thread, job);
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (_inetAddress == null)
            return getName() + "@0.0.0.0:"+_port;
        if (_listen != null)
            return getName() + "@" + _listen.getInetAddress().getHostAddress() + ":" + _listen.getLocalPort();
        return getName() + "@" + _host + ":" +_port;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Acceptor extends Thread
    {
        /* ------------------------------------------------------------ */
        public void run()
        {
            ThreadedServer threadedServer= ThreadedServer.this;
            try
            {
                this.setName("Acceptor " + _listen);
                while (_running)
                {
                    try
                    {
                        // Accept a socket
                        Socket socket= acceptSocket(_listen, _soTimeOut);

                        // Handle the socket
                        if (_running)
                        {
                            if (socket == null)
                                threadedServer.shrink();
                            else
                                threadedServer.run(socket);
                        }
                        else
                            if (socket != null)
                                socket.close();
                    }
                    catch (SocketTimeoutException e)
                    {
                        LogSupport.ignore(log, e);
                    }
                    catch (InterruptedIOException e)
                    {
                        LogSupport.ignore(log, e);
                    }
                    catch (InterruptedException e)
                    {
                        LogSupport.ignore(log, e);
                    }
                    catch (Exception e)
                    {
                        if (_running)
                            log.warn(LogSupport.EXCEPTION, e);
                        else
                            log.debug(LogSupport.EXCEPTION, e);
                    }
                    catch (Error e)
                    {
                        log.warn(LogSupport.EXCEPTION, e);
                        break;
                    }
                }
            }
            finally
            {
                if (_running)
                    log.warn("Stopping " + this.getName());
                else
                    log.info("Stopping " + this.getName());
                synchronized (threadedServer)
                {
                    _acceptor= null;
                    threadedServer.notifyAll();
                }
            }
        }

        /* ------------------------------------------------------------ */
        void forceStop()
        {
            if (_listen != null)
            {
                InetAddress addr= _listen.getInetAddress();
                int port=_listen.getLocalPort();
                
                try
                {
                    if (addr == null || addr.toString().startsWith("0.0.0.0"))
                        addr= InetAddress.getByName("127.0.0.1");
                        
                    if (log.isDebugEnabled())
                        log.debug("Self connect to close listener " + addr + ":" + port);
                    Socket socket= new Socket(addr, port);
                    Thread.yield();
                    socket.close();
                    Thread.yield();
                }
                catch (IOException e)
                {
                    if (log.isDebugEnabled())
                        log.debug("problem stopping acceptor " + addr + ": ", e);
                }
            }
        }
    }
}
