// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.nbio;
import org.mortbay.http.SocketListener;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpConnection;
import org.mortbay.http.ChunkableInputStream;
import org.mortbay.util.Code;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.Log;
import org.mortbay.util.ThreadedServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;

import ninja2.core.io_core.nbio.SelectSet;
import ninja2.core.io_core.nbio.SelectItem;
import ninja2.core.io_core.nbio.Selectable;
import ninja2.core.io_core.nbio.NonblockingServerSocket;
import ninja2.core.io_core.nbio.NonblockingSocket;
import ninja2.core.io_core.nbio.NonblockingInputStream;

/* ------------------------------------------------------------ */
/** Non blocking Listener.
 * This listener implementation uses the NBIO library from
 * http://www.cs.berkeley.edu/~mdw/proj/java-nbio/  to avoid having
 * threads assigned to idle connections.
 *
 * It is completely EXPERIMENTAL!
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class NBIOListener extends SocketListener
{
    SelectSet _selectSet;
    SelectSet _newSelectSet;
    Selector _selector;
    NonblockingServerSocket _server;
    NonblockingSocket _toSelector;
    NonblockingSocket _fromListener;
    NonblockingInputStream _in;
    OutputStream _out;
    
    /* ------------------------------------------------------------------- */
    public NBIOListener()
        throws IOException
    {}

    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        _selectSet = new SelectSet();
        _newSelectSet = new SelectSet();

        // Setup feedback socket to kick select out of select.
        _server = new NonblockingServerSocket(0);
        _toSelector = new NonblockingSocket("127.0.0.1",_server.getLocalPort());
        _fromListener = _server.accept();
        _in=(NonblockingInputStream)_fromListener.getInputStream();
        _out=_toSelector.getOutputStream();
        _selectSet.add(new SelectItem(_fromListener,null,Selectable.READ_READY));

        // Start selector thread
        _selector=new Selector();
        _selector.start();
       
        super.start();
        //System.err.println("Started "+_selector);
    }
    
    /* ------------------------------------------------------------ */
    public void stop()
        throws InterruptedException
    {
        if (_selector!=null)
        {
            _selector._running=false;
            _selector.interrupt();
            _selector=null;
            _selectSet=null;
            _newSelectSet=null;
            _toSelector=null;
            _fromListener=null;
            _in=null;
            _out=null;
        }
        super.stop();
    }
    
    /* ------------------------------------------------------------ */
    protected ServerSocket newServerSocket(InetAddrPort address,
                                           int acceptQueueSize)
         throws java.io.IOException
    {
        if (address==null)
            return new ServerSocketWrapper(0,acceptQueueSize);

        return new ServerSocketWrapper(address.getPort(),
                                       acceptQueueSize,
                                       address.getInetAddress());
    }
    
    /* ------------------------------------------------------------ */
    public void handle(Object job)
    {
        try
        {
            if (job instanceof HttpConnection)
            {
                HttpConnection connection=(HttpConnection)job;
                handleHttpConnection(connection);
            }
            else if (job instanceof SelectSet)
            {
                try
                {
                    _out.write(1);
                    _out.flush();
                }
                catch(Exception e)
                {
                    Code.warning(e);
                }
            }
            else
            {
                Socket socket = (Socket)job;
                handleConnection(socket);
            }
        }
        catch(IOException e)
        {
            Code.warning(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param job A Connection.
     */
    public void handleConnection(Socket socket)
        throws IOException
    {    
        HttpConnection connection =
            new HttpConnection(this,
                               socket.getInetAddress(),
                               socket.getInputStream(),
                               socket.getOutputStream(),
                               socket);
        handleHttpConnection(connection);
    }
    
    /* ------------------------------------------------------------ */
    /**
     */
    public void handleHttpConnection(HttpConnection connection)
        throws IOException
    {   
        while (connection.handleNext())
        {
            ChunkableInputStream cin=connection.getInputStream();
            if (cin.available()<=0)
            {
                pollOn(connection);
                break;
            }
        }
    }

    /* ------------------------------------------------------------ */
    protected void pollOn(HttpConnection connection)
    {
        //System.err.println("Polling on "+connection);
        SelectItem item = (SelectItem)connection.getObject();
        if (item==null)
        {
            item=new SelectItem((Selectable)connection.getConnection(),
                                connection,
                                (short)(Selectable.READ_READY |
                                        Selectable.SELECT_ERROR));
            //System.err.println("Created "+item);
            connection.setObject(item);
        }

        synchronized(_newSelectSet)
        {
            if (_newSelectSet.size()==0)
            {
                try{run(_newSelectSet);}
                catch(Exception e){Code.warning(e);}
            }
            _newSelectSet.add(item);
        }
    }

    /* ------------------------------------------------------------ */
    /** Customize request from socket.
     * Derived versions of PollListener may specialize this method
     * to customize the request with attributes of the socket used (eg
     * SSL session ids).
     * @param request
     */
    protected void customizeRequest(Socket socket,
                                    HttpRequest request)
    {
    }

    /* ------------------------------------------------------------ */
    /** Persist the connection
     * @param connection.
     */
    public void persistConnection(HttpConnection connection)
    {
        Thread.yield();
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if low on idle threads. 
     */
    public boolean isLowOnResources()
    {
        return false;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Selector extends Thread
    {
        boolean _running=false;
        public void run()
        {
            _running=true;
            while(_running)
            {
                try
                {
                    //System.err.println("Select "+_selectSet);
                    int numevents=_selectSet.select(10000);
                    if (numevents>0)
                    {
                        //System.err.println("Selected "+_selectSet);
                        for (int i=_selectSet.size();i-->0;)
                        {
                            SelectItem item = (SelectItem)_selectSet.elementAt(i);
                            if (item.returnedEvents()!=0)
                            {
                                // System.err.println("Event on "+item);
                                item.revents=0;
                                if (item.getObj()!=null)
                                {
                                    _selectSet.remove(i);
                                    NBIOListener.this.run(item.getObj());
                                }
                                else
                                    while(_in.nbRead()>=0);
                            }
                        }
                    }
                    Thread.yield();
                    synchronized(_newSelectSet)
                    {
                        for (int i=_newSelectSet.size();i-->0;)
                        {
                            _selectSet.add(_newSelectSet.elementAt(i));
                            _newSelectSet.remove(i);
                        }
                    }
                }
                catch(InterruptedException e)
                {
                    Code.ignore(e);
                }
                catch(Exception e)
                {
                    Code.warning(e);
                }
            }
        }
    }
    
}
