// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;

import java.net.*;

/* ======================================================================== */
/** InetAddress and Port
 */
public class InetAddrPort
{
    private InetAddress _addr=null;
    private int _port=0;

    /* ------------------------------------------------------------------- */
    public InetAddrPort()
    {}

    /* ------------------------------------------------------------ */
    /** Constructor for a port on all local host address.
     * @param port 
     */
    public InetAddrPort(int port)
    {
        _port=port;
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param addr 
     * @param port 
     */
    public InetAddrPort(InetAddress addr, int port)
    {
        _addr=addr;
        _port=port;
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param inetAddrPort String of the form "addr:port"
     */
    public InetAddrPort(String inetAddrPort)
        throws java.net.UnknownHostException
    {
        int c = inetAddrPort.indexOf(":");
        if (c>=0)
        {
            String addr=inetAddrPort.substring(0,c);
            inetAddrPort=inetAddrPort.substring(c+1);
        
            if (addr.length()>0 && ! "0.0.0.0".equals(addr))
                this._addr=InetAddress.getByName(addr);
        }
        
        _port = Integer.parseInt(inetAddrPort); 
    }
    
    /* ------------------------------------------------------------ */
    /** Get the IP address
     * @return The IP address
     */
    public InetAddress getInetAddress()
    {
        return _addr;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the IP address
     * @param addr The IP address
     */
    public void setInetAddress(InetAddress addr)
    {
        _addr=addr;
    }

    /* ------------------------------------------------------------ */
    /** Get the port
     * @return The port number
     */
    public int getPort()
    {
        return _port;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the port
     * @param port The port number
     */
    public void setPort(int port)
    {
        _port=port;
    }
    
    
    /* ------------------------------------------------------------------- */
    public String toString()
    {
        if (_addr==null)
            return "0.0.0.0:"+_port;
        return _addr.toString()+':'+_port;
    }
}

    




