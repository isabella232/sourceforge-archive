package com.kiwiconsulting.jetty;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Security;
import java.security.SecureRandom;

import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.TrustManagerFactory;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HandshakeCompletedEvent;

import com.mortbay.Util.InetAddrPort;
import com.mortbay.Base.Log;
import com.mortbay.HTTP.HttpListener;
import com.mortbay.HTTP.HttpServer;

/** An adapter to add SSL from the Sun JSSE to Jetty.  This is based
 * loosely on the Protekt implementation.
 *
 *<p>
 *
 * This implementation is trash.  I just wanted to get something working.
 *
 * @author Court Demas (court@kiwiconsulting.com)
 **/

public class JettyJavaSSLHttpListener extends HttpListener
{
    // location of the keystore (defaults to ~/.keystore)
    public static final String KEYSTORE_PROPERTY = "jetty.ssl.keystore";
    public static final String DEFAULT_KEYSTORE  = System.getProperty( "user.home" ) + File.separator + ".keystore";

    // password for the keystore
    public static final String PASSWORD_PROPERTY = "jetty.ssl.password";
    public static final String DEFAULT_PASSWORD  = "password";

    /* ------------------------------------------------------------ */
    static
    {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
    }

    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param p_address
     * @param p_server
     * @exception IOException
     */
    public JettyJavaSSLHttpListener( InetAddrPort p_address, HttpServer p_server )
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
    public JettyJavaSSLHttpListener( InetAddrPort p_address,
                                     HttpServer p_server,
                                     int p_minThreads,
                                     int p_maxThreads,
                                     int p_maxIdleTimeMs )
        throws IOException
    {
        super( p_address, p_server, p_minThreads, p_maxThreads, p_maxIdleTimeMs );
        if( p_address.getPort() == 0 )
        {
            p_address.setPort( 80 );
            super.setAddress( p_address.getInetAddress(), p_address.getPort() );
        }

        System.err.println( "JettyJavaSSLHttpListener started on " + p_address );
    }

    /* ------------------------------------------------------------ */
    /*
     * @return
     * @exception Exception
     */
    private SSLServerSocketFactory createFactoryHACK()
        throws Exception
    {
        String password = System.getProperty( PASSWORD_PROPERTY, DEFAULT_PASSWORD );
        Log.event(PASSWORD_PROPERTY+"="+password);
        char[] pwChars = password.toCharArray();

        KeyStore ks = KeyStore.getInstance( "JKS" );

        String keystore = System.getProperty( KEYSTORE_PROPERTY, DEFAULT_KEYSTORE );
        Log.event(KEYSTORE_PROPERTY+"="+keystore);

        ks.load( new FileInputStream( new File( keystore ) ), pwChars );

        KeyManagerFactory km = KeyManagerFactory.getInstance( "SunX509" );
        km.init( ks, pwChars );
        KeyManager[] kma = km.getKeyManagers();

        TrustManagerFactory tm = TrustManagerFactory.getInstance( "SunX509" );
        tm.init( ks );
        TrustManager[] tma = tm.getTrustManagers();
        SSLContext sslc = SSLContext.getInstance( "SSL" );
        sslc.init( kma, tma, SecureRandom.getInstance( "SHA1PRNG" ) );
        SSLServerSocketFactory ssfc = sslc.getServerSocketFactory();
        return ssfc;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param p_address
     * @param p_acceptQueueSize
     * @return
     * @exception IOException
     */
    protected ServerSocket newServerSocket( InetAddrPort p_address, int p_acceptQueueSize )
        throws IOException
    {
        SSLServerSocketFactory factory = null;
        SSLServerSocket socket = null;

        try
        {
            //factory = createFactory();
            factory = createFactoryHACK();

            // yeah right...
            //factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

            if( p_address == null )
            {
                socket = (SSLServerSocket) factory.createServerSocket( 0, p_acceptQueueSize );
            }
            else
            {
                socket = (SSLServerSocket) factory.createServerSocket( p_address.getPort(), p_acceptQueueSize, p_address.getInetAddress() );
            }
        }
        catch( Exception e )
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
            s.startHandshake();  // this should block until the handshaking is done
            return (Socket) s;
        }
        catch( SSLException e )
        {
            e.printStackTrace();
            throw new IOException( e.getMessage() );
        }
    }
}
