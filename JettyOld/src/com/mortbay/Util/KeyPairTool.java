// ========================================================================
// Copyright (c) 1998 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import java.io.*;
import java.security.cert.*;
import java.security.spec.*;
import java.util.*;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.Security;
import java.security.Provider;

/* ------------------------------------------------------------ */
/**
 * Perform simple private key management for keystores.
 *
 * <p> The current keytool lacks the ability to insert a key/cert pair sourced
 *     from another tool. This utility fills that gap.
 * 
 * <p> Currently this only works for RSA key/cert pairs.
 *
 * <p> The inverse operation, exporting a keypair to an external format, has
 *     been left as an exercise for the reader... :-)
 *
 * @version $Id$
 * @author Brett Sealey
 */
public class KeyPairTool
{
    // Default settings...
    private File keyStoreFile
	= new File(System.getProperty("user.home"), ".keystore");
    private String keyStoreType = KeyStore.getDefaultType();
    private Password keyStorePassword = null;
    private Password keyPassword = null;
    private String alias = "mykey";
    private File privateKeyFile = null;
    private File certFile = null;
    String providerClassName
	  = "org.bouncycastle.jce.provider.BouncyCastleProvider";


    private final String usageString
	= "Tool to insert a private key/certificate pair into a keystore.\n"
	+ "Parameters:\n"
	+ " -key        FILENAME, location of private key [MANDATORY]\n"
	+ " -cert       FILENAME, location of certificate [MANDATORY]\n"
	+ " -storepass  PASSWORD, keystore password       [OPTIONAL - security RISK!]\n"
	+ " -keypass    PASSWORD, password for new entry  [=STOREPASS]\n"
	+ " -keystore   FILENAME, location of keystore,   [~/.keystore]\n"
	+ " -storetype  STRING,   name/type of keystore,  ["
        +                                  keyStoreType + "]\n"
	+ " -alias      NAME,     alias used to store key [mykey]\n"
	+ " -provider   NAME,     name of provider class [org.bouncycastle.jce.provider.BouncyCastleProvider]\n\n"
	+ "The keystore and key passwords will be prompted for or can be\n"
	+ "set with the following JVM system properties:\n"
	+ "  jetty.ssl.password\n"
	+ "  jetty.ssl.keypassword";

    
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
	// Doit
	KeyPairTool tool = new KeyPairTool();
	tool.doit(args);
    }

    /* ------------------------------------------------------------ */
    /**
     * Load parameters and perform the import command.
     * Catch any exceptions and clear the password arrays.
     */
    private void doit(String[] args)
    {
	try
	{
	    // load parameters from the commandline
	    loadParameters(args);

	    // Try to load the private key
	    importKeyPair();
	}
	catch (Exception e)
	{
	    System.out.println("Exception: " + e.getMessage());
	    e.printStackTrace();
	    
	    System.exit(23);
	}
	finally
	{
	    keyStorePassword.zero();
	    keyPassword.zero();
	}
    }

    /**
     * Import a key/cert pair into the keystore.
     * <p> Class variables hold the state information required for this
     *     operation.
     * @throws IOException if there are problems with file IO
     * @throws GeneralSecurityException if there are cryptographic failures.
     */
    private void importKeyPair()
    throws IOException, java.security.GeneralSecurityException
    {
	FileInputStream privateKeyInputStream
	  = new FileInputStream(privateKeyFile);
	byte[] keyBytes = new byte[(int) privateKeyFile.length()];
	privateKeyInputStream.read(keyBytes);

	PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyBytes);
	KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

	System.out.println("Loaded the private key...");

	// Import the cert...
	FileInputStream certInputStream
	  = new FileInputStream(certFile);

	CertificateFactory certificateFactory
	  = CertificateFactory.getInstance("X509");
	Collection collection
	  = certificateFactory.generateCertificates(certInputStream);
	Certificate[] certChain = (Certificate[])collection.toArray();

	System.out.println("Loaded the public key...");

	//--------------------------------------------------

	// Load the KeyStore
	if (keyPassword == null)
	    keyPassword = keyStorePassword;

	KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
	InputStream keyStoreStream = null;
	try
	{
	     keyStoreStream = new FileInputStream(keyStoreFile);
	     System.out.println("Will load keystore: " + keyStoreFile);
	}
	catch (FileNotFoundException e)
	{
	    // That's OK, we'll just create a new one
	    System.out.println("Will create keystore: " + keyStoreFile);
	}

	// The load method can accept a null keyStoreStream.
	keyStore.load(keyStoreStream, keyStorePassword.getCharArray());

	if (keyStoreStream != null)
	{
	    keyStoreStream.close();
	    System.out.println("Keystore loaded OK...");
	}

	// Insert the new key pair
	keyStore.setKeyEntry(alias,
			     privateKey,
			     keyPassword.getCharArray(),
			     certChain);

	// To save the KeyStore to disk
	FileOutputStream keyStoreOut = new FileOutputStream(keyStoreFile);
	keyStore.store(keyStoreOut,
		       keyStorePassword.getCharArray());
	keyStoreOut.close();

	System.out.println("Keys have been written to keystore");
    }
 
    /**
     * Show a usage message.
     */
    void usage()
    {
	System.out.println(usageString);
	System.exit(23);
    }

    /**
     * Load parameters from the given args and check usage.
     * Will exit on usage errors.
     * <p> Class variables are populated from the command line arguments
     * @param args Array of Strings from the command line.
     */
    void loadParameters(String[] args)
    {
	for (int i = 0; (i < args.length) && args[i].startsWith("-"); i++)
	{
	    String parameterName = args[i];
	    if (parameterName.equalsIgnoreCase("-key"))
		privateKeyFile = new File(args[++i]);
	    else if (parameterName.equalsIgnoreCase("-cert"))
		certFile = new File(args[++i]);
 	    else if (parameterName.equalsIgnoreCase("-keystore"))
		keyStoreFile = new File(args[++i]);
	    else if (parameterName.equalsIgnoreCase("-storetype"))
		keyStoreType = args[++i];
	    else if (parameterName.equalsIgnoreCase("-alias"))
		alias = args[++i];
	    else if (parameterName.equalsIgnoreCase("-provider"))
		providerClassName = args[++i];
	    else
	    {
		System.err.println("Illegal parameter: " + parameterName);
		usage();
	    }
	}

	// Check that mandatory fields have been populated
	if (privateKeyFile == null || certFile == null)
	{
	    usage();
	}

	keyStorePassword = new Password("jetty.ssl.password");
	keyPassword = new Password("jetty.ssl.keypassword",
				   null,
				   keyStorePassword.toString());
	

	// Dynamically install the Bouncy Castle provider for RSA support.
	try
	{
	    Class providerClass = Class.forName(providerClassName);
	    Provider provider = (Provider)providerClass.newInstance();
	    Security.addProvider(provider);
	}
	catch (Exception e)
	{
	    System.out.println("Exception: " + e.getMessage());
	    e.printStackTrace();

	    System.out.println("Unable to load provider: "
                               + providerClassName);
	    
	    usage();
	}
    }
}










