package au.com.forge.Protekt.Jetty;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.HTTP.*;

import java.io.*;
import java.net.*;

import au.com.forge.Protekt.*;

// =================================================================
// = PROJECT
//					Jetty Integration
// = FILENAME
//					ProtektListener.java
// = Created
//					1 July 1999
// = AUTHOR
// 				    David Taylor (Forge Research Pty Ltd)
// = COPYRIGHT
//                  (c) Copyright 1999 Forge Research Pty Ltd
//
/* =================================================================

	RCS Automatic variables.

	$Author$

	$Date$

	$Log$
	Revision 1.2  1999/07/01 09:32:07  dtaylor
	Removed references to this.address and thise.server
	to clean the code up.

	Revision 1.1  1999/07/01 08:10:03  dtaylor
	Initial version.


	$Id$

	==================================================================*/

/**
 *<BR>
 *<BR><I>Copyight &copy; 1998 Forge Research</I>
 *<BR>
 *<BR>
 *<HR>
 * <p> Instances of ProtektListener handle a single receiving HTTP
 * connection, using Protekt SSL sockets. They make calls into
 * HttpServer to handle the requests that they receive.
 *
 * <p> The major change is in the accept method, where the SSL
 * handshake is performed. The accept method does not return
 * until the handshake is complete and will throw an IOException
 * if the handshake fails.
 *
 * <p> The default server certificate defined in the Protekt.properties
 * file will be used for the server certificate.
 *
 *<HR>
 *       <B>Bugs/Features & Functionality yet to be implemented</B>
 *       <UL>
 *       <LI> There is no way to switch on Protekt's client authentication
 *            processing at present.
 *       </UL>
 *<HR>
 *
 * @version			$Revision$
 * @author			David Taylor (Forge Research)
 * @see				com.mortbay.HTTP.HttpListener
 */

public class ProtektListener extends HttpListener
{
	/**
	 * Construct a ProtektListener.
	 *
	 * @param address The InetAddress and port on which to listen
	 *                If address.inetAddress==null, InetAddrPort.getLocalHost()
	 *                is used and set in address. If address.port==0, 80 is
	 *                used and set in address.
	 * @param server The HttpServer to pass requests to.
	 */     
	public ProtektListener(InetAddrPort address, HttpServer server)
	throws IOException
	{
		this(address,server,0,0,0);
	}

	/**
	 * Constructor. 
	 *
	 * @param address The InetAddress and port on which to listen
	 *                If address.inetAddress==null, InetAddrPort.getLocalHost()
	 *                is used and set in address. If address.port==0, 80 is used
	 *                and set in address.
	 *
	 * @param server  The HttpServer to pass requests to.
	 * @param minThreads 
	 * @param maxThreads 
	 * @param maxIdleTimeMs 
	 * @exception IOException 
	 */
    public ProtektListener(InetAddrPort address,
			HttpServer server,
			int minThreads,
			int maxThreads,
			int maxIdleTimeMs)
	throws IOException
    {
		super(address,server,minThreads,maxThreads,maxIdleTimeMs);
		if(address.getPort()==0)
		{
			address.setPort(80);
			super.setAddress(address.getInetAddress(),address.getPort());
		}

		Log.event("ProtektListener started on " + address);
	}

	/**
	 * Creates a new servers socket.
	 *
	 * This method has been overridden and now creates
	 * Protekt SSL SSLServerSockets.
	 *
	 * @param address Address and port
	 * @param acceptQueueSize Accept queue size
	 * @return The new ServerSocket
	 * @exception java.io.IOException 
	 */
	protected ServerSocket newServerSocket(InetAddrPort address, int acceptQueueSize)
	 throws java.io.IOException
	{
		if (address==null)
			return(ServerSocket)new SSLServerSocket(0,acceptQueueSize);

		return(ServerSocket)new SSLServerSocket(address.getPort(), acceptQueueSize, address.getInetAddress());
	}

    /**
	 * Accept socket connection.
	 *
	 * The given server socket is assumed to be a Protekt SSL
	 * SSLServerSocket, and when a connection is accepted the
	 * SSL handshake is started.
	 *
     * @param serverSocket
     * @return Accepted Socket with the SSL handshake already done
     * @exception java.io.IOException 
     */
    protected Socket accept(ServerSocket serverSocket)
	 throws java.io.IOException
    {
		SSLSocket s = (SSLSocket)serverSocket.accept();
		try
		{
			/*
			 * Adding some code to read a property with the keystore
			 * alias to use for this particular server would be a nice
			 * touch.
			 *
			 * A property specifying whether to use client
			 * authentication would also be good.
			 */
			s.startHandshake();
			s.waitForHandshake();
		}
		catch(SSLException e)
		{
			throw new IOException(e.getMessage());
		}

		return (Socket)s;
	}
}


//--------------------------------------------------------------------------
//                           End of File
//--------------------------------------------------------------------------



