// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.Code;
import com.mortbay.Util.InetAddrPort;
import com.mortbay.Util.ThreadPool;
import com.mortbay.Util.ThreadedServer;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

/* ------------------------------------------------------------ */
/** JSSE Socket Listener.
 *
 * This specialization of HttpListener is an abstract listener
 * that can be used as the basis for a specific JSSE listener.
 *
 * This is heavily based on the work from Court Demas, which in
 * turn is based on the work from Forge Research.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw@mortbay.com)
 * @author Court Demas (court@kiwiconsulting.com)
 * @author Forge Research Pty Ltd  ACN 003 491 576
 **/
public abstract class JsseListener extends SocketListener
{
    // location of the keystore (defaults to ~/.keystore)
    public static final String KEYSTORE_PROPERTY = "jetty.ssl.keystore";
    public static final String DEFAULT_KEYSTORE  =
        System.getProperty("user.home" ) + File.separator + ".keystore";

    // password for the keystore
    public static final String PASSWORD_PROPERTY = "jetty.ssl.password";
    // password for the key password
    public static final String KEYPASSWORD_PROPERTY = "jetty.ssl.keypassword";

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param p_address 
     * @param p_server 
     * @exception IOException 
     */
    public JsseListener()
        throws IOException
    {
        super();
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
    public JsseListener( InetAddrPort p_address)
        throws IOException
    {
        super(p_address);
        if( p_address.getPort() == 0 )
        {
            p_address.setPort( 443 );
            setPort(443);
        }
    }

    /* --------------------------------------------------------------- */
    public String getDefaultScheme()
    {
        return "https";
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
            Code.warning(e);
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
    protected void customizeRequest(Socket socket,
                                    HttpRequest request)
    {
        if (!(socket instanceof javax.net.ssl.SSLSocket))
            return; // I'm tempted to let it throw an exception...

        try
        {
            SSLSocket sslSocket = (SSLSocket) socket;
            SSLSession sslSession = sslSocket.getSession();

            //request.setScheme("https");
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
            Code.warning(e);
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
            Code.warning(e);
            throw new IOException( e.getMessage() );
        }
    }
}
