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

/* ------------------------------------------------------------ */
/** 
 * The start of a TCP/IP connection load balancer
 *
 * currently takes a single listener and server.
 * @see
 * @version 1.0 Sat Mar  9 2002
 * @author Greg Wilkins (gregw)
 */
public class Balancer 
{
    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
        throws Exception
    {
        System.err.println("SelectionKey.OP_CONNECT == "+SelectionKey.OP_CONNECT);
        System.err.println("SelectionKey.OP_ACCEPT == "+SelectionKey.OP_ACCEPT);
        System.err.println("SelectionKey.OP_READ == "+SelectionKey.OP_READ);
        System.err.println("SelectionKey.OP_WRITE == "+SelectionKey.OP_WRITE);
        
        ByteBufferPool pool = new ByteBufferPool(512,true);
        
        Server server = new Server(pool,new InetAddrPort(arg[1]));
        Policy policy = new Policy(server);
        Listener listener = new Listener(pool,new InetAddrPort(arg[0]),policy);
        
        server.start();
        listener.start();
    }
}
