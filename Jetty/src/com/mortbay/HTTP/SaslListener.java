// ========================================================================
// Copyright (c) 2001, Forge Research Pty. Limited. All rights reserved.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import com.mortbay.Util.Code;
import com.mortbay.Util.InetAddrPort;
import com.mortbay.Util.Log;
import com.mortbay.Util.ThreadedServer;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Hashtable;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;


/* ------------------------------------------------------------ */
/** Socket SASL/HTTP Listener.
 * A socket listener to handle incoming SASL authentication requests.
 * @version $Id$
 * @author Raif S. Naffah (raif)
 */
public class SaslListener
    extends SocketListener
{
    /* ------------------------------------------------------------------- */
    public SaslListener()
        throws IOException
    {
        super();
    }

    /* ------------------------------------------------------------------- */
    public SaslListener(InetAddrPort address)
        throws IOException
    {
        super(address);
    }

    /* ------------------------------------------------------------------- */
    /** Handle Job.
     * Implementation of ThreadPool.handle(), calls handleConnection.
     * @param job A Connection.
     */
    public final void handleConnection(Socket socket)
        throws IOException
    {
        Code.debug("==> handleConnection()");

        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        int c;
        StringBuffer sb = new StringBuffer();
        while ((c = in.read()) != 0x00)
            sb.append((char) c);

        String mechanism = sb.toString();
        Code.debug("Requested authentication mechanism: ", mechanism);

        String host = socket.getInetAddress().getHostAddress();

        Hashtable properties = new Hashtable();
        String serverName = InetAddress.getLocalHost().getHostName();
        Code.debug("Server hostname: "+serverName);

        SaslServer saslServer =
            Sasl.createSaslServer(mechanism, "http", serverName, properties, null);
        if (saslServer == null)
            throw new RuntimeException("Unable to create "+mechanism+" SASL server");

		  byte[] response, challenge;
        int limit;
        do
		  {
            c = in.read();
            limit = in.available();
            response = new byte[limit+1];
            response[0] = (byte) c;
            in.read(response, 1, limit);
			   challenge = saslServer.evaluateResponse(response);
			   out.write(challenge);
        }
		  while (!saslServer.isComplete());

        InputStream secureIn = saslServer.getInputStream(in);
        OutputStream secureOut = saslServer.getOutputStream(out);

        HttpConnection connection =
            new HttpConnection(this,
                               socket.getInetAddress(),
                               secureIn,
                               secureOut,
                               socket);
        connection.handle();

        Code.debug("<== handleConnection()");
    }
}