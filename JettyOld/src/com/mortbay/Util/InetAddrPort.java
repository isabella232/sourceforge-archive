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

    /* ------------------------------------------------------------------- */
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
    
    /* ------------------------------------------------------------------- */
    public InetAddrPort(InetAddress addr, int port)
    {
	this.inetAddress=addr;
	this.port=port;
    }
    
    /* ------------------------------------------------------------------- */
    public String toString()
    {
	return inetAddress.toString()+':'+port;
    }
}

    
