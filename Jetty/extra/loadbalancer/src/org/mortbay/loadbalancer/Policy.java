// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.loadbalancer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Policy
{
    private static Log log = LogFactory.getLog(Policy.class);

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
        log.info("Unstick "+client+" from "+sticky);
        
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

        if(log.isDebugEnabled())log.debug("Allocate "+ client + " size="+queue.size());
            
        Integer s = (Integer)_stickyInet.get(client);
        if (s==null)
        {
            _next=(_next+1)%_server.length;
            log.info("Stick "+client+" to "+_next);
            connection.allocate(_server[_next],tries);
            _stickyInet.put(client,new Integer(_next));
        }
        else
        {
            if(log.isDebugEnabled())log.debug(client+" stuck to "+s);
            connection.allocate(_server[s.intValue()],tries);
        }
    }
    
}
