// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

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
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.Log;
import org.mortbay.util.Password;




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
    private String _keystore=DEFAULT_KEYSTORE ;
    private Password _password;
    private Password _keypassword;

    /* ------------------------------------------------------------ */
    static
    {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
    }

    /* ------------------------------------------------------------ */
    public void setKeystore(String keystore)
    {
        _keystore = keystore;
    }
    
    /* ------------------------------------------------------------ */
    public String getKeystore()
    {
        return _keystore;
    }
    
    /* ------------------------------------------------------------ */
    public void setPassword(String password)
    {
        _password = Password.getPassword(PASSWORD_PROPERTY,password,null);
    }

    /* ------------------------------------------------------------ */
    public void setKeyPassword(String password)
    {
        _keypassword = Password.getPassword(KEYPASSWORD_PROPERTY,password,null);
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
            _password = Password.getPassword(PASSWORD_PROPERTY,null,null);
        Log.event(PASSWORD_PROPERTY+"="+_password.toStarString());
        
        if (_keypassword==null)
            _keypassword = Password.getPassword(KEYPASSWORD_PROPERTY,
                                                null,
                                                _password.toString());
        Log.event(KEYPASSWORD_PROPERTY+"="+_keypassword.toStarString());

        KeyStore ks = KeyStore.getInstance( "JKS" );
        ks.load( new FileInputStream( new File( _keystore ) ),
                 _password.toString().toCharArray());
        
        KeyManagerFactory km = KeyManagerFactory.getInstance( "SunX509","SunJSSE"); 
        km.init( ks, _keypassword.toString().toCharArray() );
        KeyManager[] kma = km.getKeyManagers();                        
        
        TrustManagerFactory tm = TrustManagerFactory.getInstance("SunX509","SunJSSE");
        tm.init( ks ); 
        TrustManager[] tma = tm.getTrustManagers();
        
        SSLContext sslc = SSLContext.getInstance( "SSL" ); 
        sslc.init( kma, tma, SecureRandom.getInstance("SHA1PRNG"));
        
        SSLServerSocketFactory ssfc = sslc.getServerSocketFactory();
        Log.event("SSLServerSocketFactory="+ssfc);
        return ssfc;
    }
}



