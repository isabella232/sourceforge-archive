// ========================================================================
// $Id$
// Copyright 2002-2004 Mort Bay Consulting Pty. Ltd.
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
