// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.NBIO;


import com.mortbay.Util.Code;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.ServerSocket;
import ninja2.core.io_core.nbio.NonblockingServerSocket;

/* ------------------------------------------------------------ */
/** Wraps a NonblockingServerSocket. 
 * Works around limitation in ServerSocket constructors by opening
 * a normal ServerSocket, closing it and then opening the nonblocking
 * equivalent. All subsequent calls are delegated to the nonblocking socket. 
 *
 * @version 1.0 Mon Aug 27 2001
 * @author Greg Wilkins (gregw)
 */
public class ServerSocketWrapper extends ServerSocket
{
    private int _port=-1;
    private int _queue=-1;
    private InetAddress _addr=null;
    private NonblockingServerSocket socket=null;
    
    /* ------------------------------------------------------------ */
    public ServerSocketWrapper(int port)
        throws IOException
    {
        super(port);
        _port=port;
    }
    
    /* ------------------------------------------------------------ */
    public ServerSocketWrapper(int port,int queue)
        throws IOException
    {
        super(port,queue);
        _port=port;
        _queue=queue;
    }

    /* ------------------------------------------------------------ */
    public ServerSocketWrapper(int port,int queue,InetAddress addr)
        throws IOException
    {
        super(port,queue,addr);
        _port=port;
        _queue=queue;
        _addr=addr;
    }
    
    /* ------------------------------------------------------------ */
    private void checkSocket()
        throws IOException
    {
        if (socket!=null)
            return;
        
        super.close();
        if (_addr!=null)
            socket = new NonblockingServerSocket(_port,_queue,_addr);
        else if (_queue>=0)
            socket = new NonblockingServerSocket(_port,_queue);
        else
            socket = new NonblockingServerSocket(_port);
    }
    
    /* ------------------------------------------------------------ */
    public InetAddress getInetAddress()
    {
        try{checkSocket();}
        catch(IOException e)
        {
            Code.warning(e);
            throw new IllegalStateException(e.toString());
        }
        
        return socket.getInetAddress();
    }
    
    /* ------------------------------------------------------------ */
    public int getLocalPort()
    {
        try{checkSocket();}
        catch(IOException e)
        {
            Code.warning(e);
            throw new IllegalStateException(e.toString());
        }
        return socket.getLocalPort();
    }
    
    /* ------------------------------------------------------------ */
    public Socket accept()
        throws IOException
    {
        checkSocket();
        return socket.accept();
    }
    
    /* ------------------------------------------------------------ */
    public void close()
        throws IOException
    {
        checkSocket();
        socket.close();
    }
    
    /* ------------------------------------------------------------ */
    public void setSoTimeout(int s)
        throws SocketException
    {
        try{checkSocket();}
        catch(IOException e)
        {
            Code.warning(e);
            throw new IllegalStateException(e.toString());
        }
        
        socket.setSoTimeout(s);
    }
    
    /* ------------------------------------------------------------ */
    public int getSoTimeout()
        throws IOException
    {
        checkSocket();
        return socket.getSoTimeout();
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        try{checkSocket();}
        catch(IOException e)
        {
            Code.warning(e);
            throw new IllegalStateException(e.toString());
        }
        return socket.toString();
    }
}
