// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.IO;
import org.mortbay.util.LogSupport;

/* ------------------------------------------------------------ */
/** HTTP Tunnel.
 * A HTTP Tunnel can be used to take over a HTTP connection in order to
 * tunnel another protocol over it.  The prime example is the CONNECT method
 * handled by the ProxyHandler to setup a SSL tunnel between the client and
 * the real server.
 *
 * @see HttpConnection
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class HttpTunnel
{
    private static Log log= LogFactory.getLog(HttpTunnel.class);

    private Socket _socket;
    private Thread _thread;
    private int _timeoutMs;
    private InputStream _in;
    private OutputStream _out;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    protected HttpTunnel()
    {
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param socket The tunnel socket.
     * @param timeoutMs The maximum time to wait for a read on the tunnel. Note that
     * sotimer exceptions are ignored by the tunnel.
     */
    public HttpTunnel(Socket socket, int timeoutMs)
    {
        _socket= socket;
    }

    /* ------------------------------------------------------------ */
    /** handle method.
     * This method is called by the HttpConnection.handleNext() method if
     * this HttpTunnel has been set on that connection.
     * The default implementation of this method copies between the HTTP
     * socket and the socket passed in the constructor.
     * @param in 
     * @param out 
     */
    public void handle(InputStream in, OutputStream out)
    {
        Copy copy= new Copy();
        _in= in;
        _out= out;
        try
        {
            _thread= Thread.currentThread();
            copy.start();

            copydata(_socket.getInputStream(), _out);
        }
        catch (Exception e)
        {
            LogSupport.ignore(log, e);
        }
        finally
        {
            try
            {
                _in.close();
                _socket.shutdownOutput();
                _socket.close();
            }
            catch (Exception e)
            {
                LogSupport.ignore(log, e);
            }
            copy.interrupt();
        }
    }

    /* ------------------------------------------------------------ */
    private void copydata(InputStream in, OutputStream out) throws java.io.IOException
    {
        long timestamp= 0;
        while (true)
        {
            try
            {
                IO.copy(in, out);
                timestamp= 0;
                return;
            }
            catch (InterruptedIOException e)
            {
                LogSupport.ignore(log, e);
                if (timestamp == 0)
                    timestamp= System.currentTimeMillis();
                else if (_timeoutMs > 0 && (System.currentTimeMillis() - timestamp) > _timeoutMs)
                    throw e;
            }
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Copy thread.
     * Helper thread to copy from the HTTP input to the sockets output
     */
    private class Copy extends Thread
    {
        public void run()
        {
            try
            {
                copydata(_in, _socket.getOutputStream());
            }
            catch (Exception e)
            {
                LogSupport.ignore(log, e);
            }
            finally
            {
                try
                {
                    _out.close();
                    _socket.shutdownInput();
                    _socket.close();
                }
                catch (Exception e)
                {
                    LogSupport.ignore(log, e);
                }
                _thread.interrupt();
            }
        }
    }
}
