// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import org.mortbay.util.InetAddrPort;
import org.mortbay.util.Log;
import org.mortbay.util.Password;
import com.sun.net.ssl.KeyManager;
import com.sun.net.ssl.KeyManagerFactory;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManager;
import com.sun.net.ssl.TrustManagerFactory;
import com.sun.net.ssl.internal.ssl.Provider;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import javax.net.ssl.SSLServerSocketFactory;




/* ------------------------------------------------------------ */
/** SSL Socket Listener for Sun's JSSE.
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
    private String _keystore=DEFAULT_KEYSTORE ;
    public void setKeystore(String keystore)
    {
        _keystore = keystore;
    }
    
    /* ------------------------------------------------------------ */
    private Password _password;
    public void setPassword(String password)
    {
        _password = new Password(PASSWORD_PROPERTY,password);
    }
    
    /* ------------------------------------------------------------ */
    private Password _keypassword;
    public void setKeyPassword(String password)
    {
        _keypassword = new Password(KEYPASSWORD_PROPERTY,password);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception IOException 
     */
    public SunJsseListener()
        throws IOException
    {
        super();
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param p_address 
     * @param p_server 
     * @exception IOException 
     */
    public SunJsseListener(InetAddrPort p_address)
        throws IOException
    {
        super( p_address);
    }

    
    /* ------------------------------------------------------------ */
    /* 
     * @return 
     * @exception Exception 
     */
    protected SSLServerSocketFactory createFactory()
        throws Exception
    {
        _keystore = System.getProperty( KEYSTORE_PROPERTY,_keystore);
        
        Log.event(KEYSTORE_PROPERTY+"="+_keystore);

        if (_password==null)
            _password = new Password(PASSWORD_PROPERTY);
        Log.event(PASSWORD_PROPERTY+"="+_password.toStarString());

        if (_keypassword==null)
            _keypassword = new Password(KEYPASSWORD_PROPERTY,
                                        null,
                                        _password.toString());
        Log.event(KEYPASSWORD_PROPERTY+"="+_keypassword.toStarString());

        try
        {
            KeyStore ks = KeyStore.getInstance( "JKS" );
            ks.load( new FileInputStream( new File( _keystore ) ),
                     _password.getCharArray());
            
            KeyManagerFactory km = KeyManagerFactory.getInstance( "SunX509"); 
            km.init( ks, _keypassword.getCharArray() );
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
            _password.zero();
            _keypassword.zero();
        }
    }
}



