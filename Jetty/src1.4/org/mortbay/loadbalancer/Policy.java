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
    private int next;
    
    public Policy(Server[] server)
    {
        _server=server;
    }
    
    public void allocate(Connection connection)
        throws IOException
    {
        next=(next+1)%_server.length;
        connection.allocate(_server[next]);
    }
    
}
