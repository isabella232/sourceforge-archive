// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.loadbalancer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.NonBlockingQueue;

public class Policy
{
    private Server[] _server;
    private int _next;
    private Map _stickyInet = new HashMap();
    
    /* ------------------------------------------------------------ */
    public Policy(Server[] server)
    {
        _server=server;
    }
    
    /* ------------------------------------------------------------ */
    public void deallocate(Connection connection,
                           NonBlockingQueue queue,
                           int tries)
        throws IOException
    {
        InetAddress client =
            connection.getClientSocketChannel().socket().getInetAddress();
        Object sticky=_stickyInet.remove(client);
        Log.event("Unstick "+client+" from "+sticky);
        
        if (tries+1<_server.length)
            allocate(connection,queue,tries+1);
        else
            connection.close();
    }
    
    /* ------------------------------------------------------------ */
    public void allocate(Connection connection,
                         NonBlockingQueue queue,
                         int tries)
        throws IOException
    {
        InetAddress client =
            connection.getClientSocketChannel().socket().getInetAddress();

        if (Code.debug())
            Code.debug("Allocate "+ client + " size="+queue.size());

        Integer s = (Integer)_stickyInet.get(client);
        if (s==null)
        {
            _next=(_next+1)%_server.length;
            Log.event("Stick "+client+" to "+_next);
            connection.allocate(_server[_next],tries);
            _stickyInet.put(client,new Integer(_next));
        }
        else
        {
            Code.debug(client," stuck to ",s);
            connection.allocate(_server[s.intValue()],tries);
        }
    }
    
}
