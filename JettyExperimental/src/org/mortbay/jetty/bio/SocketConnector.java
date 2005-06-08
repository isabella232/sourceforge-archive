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
 
package org.mortbay.jetty.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.bio.SocketEndPoint;
import org.mortbay.jetty.AbstractConnector;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.util.LogSupport;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/* ------------------------------------------------------------------------------- */
/**  Socket Connector.
 * This connector implements a traditional blocking IO and threading model.
 * Normal JRE sockets are used and a thread is allocated per connection.
 * Buffers are managed so that large buffers are only allocated to active connections.
 * 
 * This Connector should only be used if NIO is not available.
 * 
 * @version $Revision$
 * @author gregw
 */
public class SocketConnector extends AbstractConnector
{
    private static Logger log= LoggerFactory.getLogger(SocketConnector.class);
    
    ServerSocket _acceptSocket;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * 
     */
    public SocketConnector()
    {
    }

    /* ------------------------------------------------------------ */
    public void open() throws IOException
    {
        // Create a new server socket and set to non blocking mode
        _acceptSocket= new ServerSocket();

        // Bind the server socket to the local host and port
        _acceptSocket.bind(getAddress());
        
        log.info("Opened "+_acceptSocket);
        
    }

    /* ------------------------------------------------------------ */
    public void close() throws IOException
    {
        if (_acceptSocket!=null)
            _acceptSocket.close();
        _acceptSocket=null;
    }
    
    /* ------------------------------------------------------------ */
    public void accept()
    	throws IOException, InterruptedException
    {   
        Socket socket = _acceptSocket.accept();
        configure(socket);

        Connection connection=new Connection(socket);
        connection.dispatch();
    }

    /* ------------------------------------------------------------------------------- */
    protected Buffer newBuffer(int size)
    {
        return new ByteArrayBuffer(size);
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
            _connection = new HttpConnection(SocketConnector.this,this,getHandler());
        }
        
        void dispatch() throws InterruptedException
        {
            getThreadPool().dispatch(this);
        }
        
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
                {
                    log.warn("BAD Request");
                    log.debug("BAD",e);
                }
                else if ("EOF".equals(e.getMessage()))
                    log.debug("EOF",e);
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
}
