// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.security.*;
import com.sun.net.ssl.*;

import com.mortbay.Util.*;
import com.mortbay.Base.*;
import com.mortbay.HTTP.HttpListener;
import com.mortbay.HTTP.HttpServer;



/* ------------------------------------------------------------ */
/** JSSE Socket Listener.
 *
 * This specialization of JsseListener is an specific listener
 * using the Sun reference implementation.
 *
 * This is heavily based on the work from Court Demas, which in
 * turn is based on the work from Forge Research.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw@mortbay.com)
 * @author Court Demas (court@kiwiconsulting.com)
 * @author Forge Research Pty Ltd  ACN 003 491 576
 **/
public class SunJsseListener extends JsseListener
{
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
    public SunJsseListener(InetAddrPort p_address,
			   HttpServer p_server )
        throws IOException
    {
	super( p_address, p_server, 0, 0, 0 );
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
    public SunJsseListener( InetAddrPort p_address,
			    HttpServer p_server,
			    int p_minThreads,
			    int p_maxThreads,
			    int p_maxIdleTimeMs)
        throws IOException
    {
	super( p_address, p_server, p_minThreads, p_maxThreads,
	       p_maxIdleTimeMs );
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @return 
     * @exception Exception 
     */
    protected SSLServerSocketFactory createFactory()
        throws Exception
    {
        String keystore = System.getProperty( KEYSTORE_PROPERTY,
					      DEFAULT_KEYSTORE );
	Log.event(KEYSTORE_PROPERTY+"="+keystore);

	
        Password password = new Password(PASSWORD_PROPERTY);
	Log.event(PASSWORD_PROPERTY+"="+password.toStarString());
        Password keypassword = new Password(KEYPASSWORD_PROPERTY,
					    null,
					    password.toString());
	Log.event(KEYPASSWORD_PROPERTY+"="+keypassword.toStarString());

	try
	{
	    KeyStore ks = KeyStore.getInstance( "JKS" );
	    ks.load( new FileInputStream( new File( keystore ) ),
		     password.getCharArray());
	    
	    KeyManagerFactory km = KeyManagerFactory.getInstance( "SunX509"); 
	    km.init( ks, keypassword.getCharArray() );
	    KeyManager[] kma = km.getKeyManagers();                        
	    
	    TrustManagerFactory tm = TrustManagerFactory.getInstance("SunX509" );
	    tm.init( ks ); 
	    TrustManager[] tma = tm.getTrustManagers(); 
	    SSLContext sslc = SSLContext.getInstance( "SSL" ); 
	    sslc.init( kma, tma, SecureRandom.getInstance( "SHA1PRNG" ) ); 
	    SSLServerSocketFactory ssfc = sslc.getServerSocketFactory();
	    Log.event("SSLServerSocketFactory="+ssfc);
	    return ssfc;
	}
	finally
	{
	    password.zero();
	    keypassword.zero();
	}
    }
}








