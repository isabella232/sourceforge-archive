// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;

import com.mortbay.Base.Code;
import java.net.*;

/* ======================================================================== */
/** InetAddress and Port
 */
public class InetAddrPort
{
    public InetAddress inetAddress=null;
    public int	port=0;

    /* ------------------------------------------------------------------- */
    public InetAddrPort()
    {}

    /* ------------------------------------------------------------ */
    /** Constructor for a port on all local host address.
     * @param port 
     */
    public InetAddrPort(int port)
    {
	try{
	    this.inetAddress=InetAddress.getLocalHost();
	}
	catch(java.net.UnknownHostException e)
	{
	    Code.fail(e);
	}
	this.port=port;
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param addr 
     * @param port 
     */
    public InetAddrPort(InetAddress addr, int port)
    {
	this.inetAddress=addr;
	this.port=port;
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param inetAddrPort String of the form "addr:port"
     */
    public InetAddrPort(String inetAddrPort)
	throws java.net.UnknownHostException
    {
	int c = inetAddrPort.indexOf(":");
	Code.assert(c>0,"Badly formatted InetAddrPort");

	String addr=inetAddrPort.substring(0,c);
	if (addr.length()>0 && ! "0.0.0.0".equals(addr))
	    this.inetAddress=InetAddress.getByName(addr);
	this.port = Integer.parseInt(inetAddrPort.substring(c+1));	
    }
    
    /* ------------------------------------------------------------------- */
    public String toString()
    {
	if (inetAddress==null)
	    return "0.0.0.0:"+port;
	return inetAddress.toString()+':'+port;
    }
}

    
