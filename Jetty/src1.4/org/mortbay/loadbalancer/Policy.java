// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.loadbalancer;

import org.mortbay.util.*;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class Policy
{
    private Server[] _server;
    private int _next;
    private Map _stickyInet = new HashMap();
    
    public Policy(Server[] server)
    {
        _server=server;
    }
    
    public void allocate(Connection connection, NonBlockingQueue queue)
        throws IOException
    {
        InetAddress client =
            connection.getClientSocketChannel().socket().getInetAddress();
        
        System.err.println("\nAllocate "+ client +
                           " size="+queue.size());

        Integer s = (Integer)_stickyInet.get(client);
        if (s==null)
        {
            _next=(_next+1)%_server.length;
            connection.allocate(_server[_next]);
            _stickyInet.put(client,new Integer(_next));
            System.err.println("Stick "+client+" to "+_next);
        }
        else
        {
            System.err.println(client+" stuck to "+s);
            connection.allocate(_server[s.intValue()]);
        }
    }
    
}
