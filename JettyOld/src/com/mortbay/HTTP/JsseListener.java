package com.mortbay.HTTP;

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.security.*;
import com.sun.net.ssl.*;

import com.mortbay.Base.Code;
import com.mortbay.Util.InetAddrPort;
import com.mortbay.Base.Log;
import com.mortbay.HTTP.HttpListener;
import com.mortbay.HTTP.HttpServer;

/* ------------------------------------------------------------ */
/** JSSE Socket Listener.
 *
 * This specialization of HttpListener is an abstract listener
 * that can be used as the basis for a specific JSSE listener.
 *
 * This is heavily based on the work from Court Demas, which in
 * turn is based on the work from Forge Research.
 *
 * @author Greg Wilkins (gregw@mortbay.com)
 * @author Court Demas (court@kiwiconsulting.com)
 * @author Forge Research Pty Ltd  ACN 003 491 576
 **/
public abstract class JsseListener extends HttpListener
{
    // location of the keystore (defaults to ~/.keystore)
    public static final String KEYSTORE_PROPERTY = "jetty.ssl.keystore";
    public static final String DEFAULT_KEYSTORE  =
	System.getProperty("user.home" ) + File.separator + ".keystore";

    // password for the keystore
    public static final String PASSWORD_PROPERTY = "jetty.ssl.password";

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param p_address 
     * @param p_server 
     * @exception IOException 
     */
    public JsseListener(InetAddrPort p_address,
			HttpServer p_server )
        throws IOException
    {
	this( p_address, p_server, 0, 0, 0 );
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param p_address 
     * @param p_server 
     * @param p_minThreads 
     * @param p_maxThreads 
     * @param p_maxIdleTimeMs 
     * @exception IOException 
     */
    public JsseListener( InetAddrPort p_address,
			 HttpServer p_server,
			 int p_minThreads,
			 int p_maxThreads,
			 int p_maxIdleTimeMs)
        throws IOException
    {
	super( p_address, p_server, p_minThreads, p_maxThreads,
	       p_maxIdleTimeMs );
	if( p_address.getPort() == 0 )
	{
	    p_address.setPort( 80 );
	    super.setAddress( p_address.getInetAddress(), p_address.getPort() );
	}

    }

    
    /* ------------------------------------------------------------ */
    protected abstract SSLServerSocketFactory createFactory()
        throws Exception;
    
        
    /* ------------------------------------------------------------ */
    /** 
     * @param p_address 
     * @param p_acceptQueueSize 
     * @return 
     * @exception IOException 
     */
    protected ServerSocket newServerSocket( InetAddrPort p_address,
					    int p_acceptQueueSize )
        throws IOException
    {
        SSLServerSocketFactory factory = null;
        SSLServerSocket socket = null;

        try
        {
            factory = createFactory();

	    
            if( p_address == null )
            {
                socket = (SSLServerSocket)
		    factory.createServerSocket(0, p_acceptQueueSize );
            }
            else
            {
                socket = (SSLServerSocket)
		    factory.createServerSocket(p_address.getPort(),
					       p_acceptQueueSize,
					       p_address.getInetAddress() );
            }
        }
        catch( Exception e ) //TEMPXXXFIXME is this the "right thing to do"?
        {
            e.printStackTrace();
            return null;
        }
        finally
        {
            return socket;
        }
    }

    /* ------------------------------------------------------------ */
    /** Allow the Listener a chance to customise the request
     * before the server does its stuff.
     * <br> This allows extra attributes to be set for SSL connections.
     */
    protected void customiseRequest(Socket connection,
				    HttpRequest request)
    {
	if (!(connection instanceof javax.net.ssl.SSLSocket))
	    return; // I'm tempted to let it throw an exception...

	try
	{
	    SSLSocket sslSocket = (SSLSocket) connection;
	    SSLSession sslSession = sslSocket.getSession();

	    request.setScheme("https");
	    try
	    {
		javax.security.cert.X509Certificate[] chain
		    = sslSession.getPeerCertificateChain();
		
		request.setAttribute("javax.servlet.request.X509Certificate",
				     ((chain.length > 0) ? chain[0] : null));
		request.setAttribute(
		    "javax.servlet.request.X509Certificate_chain", chain);
	    }
	    catch (SSLPeerUnverifiedException ignore) {}

	    request.setAttribute("javax.servlet.request.cipher_suite",
				 sslSession.getCipherSuite());
	    request.setAttribute("javax.servlet.request.ssl_peer_host",
				 sslSession.getPeerHost());
	    request.setAttribute("javax.servlet.request.ssl_session",
				 new String(sslSession.getId()));

	    String valueNames[] = sslSession.getValueNames();
	    for (int i=0; i<valueNames.length; i++)
		request.setAttribute(
		    "javax.servlet.request.ssl_value," + valueNames[i],
		    sslSession.getValue(valueNames[i]));
	}
	catch (Exception e)
	{
	    Code.debug("Ignoring exception: " + e);
	}
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param p_serverSocket 
     * @return 
     * @exception IOException 
     */
    protected Socket accept( ServerSocket p_serverSocket )
        throws IOException
    {
        
	try
	{
            SSLSocket s = (SSLSocket) p_serverSocket.accept();
	    if (getMaxIdleTimeMs()>0)
		s.setSoTimeout(getMaxIdleTimeMs());
	    s.startHandshake();  // block until SSL handshaking is done
            return (Socket) s;
	}
	catch( SSLException e )
	{
            e.printStackTrace();
	    throw new IOException( e.getMessage() );
	}
    }
}
