// ========================================================================
// Copyright (c) 2001, Forge Research Pty. Limited. All rights reserved.
// $Id$
// ========================================================================

package com.mortbay.HTTP.SASL;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;


/* ------------------------------------------------------------ */
/** <P>A simple test class for the Jetty SASL Listener. Requires
 * the <A HREF="http://cryptix-sasl.sourceforge.net">Cryptix SASL
 * library</A>.</P>
 * <P>The <tt>main()</tt> method of this class accepts, at most three
 * positional parameters: the host name (first argument) and port number
 * (second argument) of where a SASLListener capable jetty is running and
 * listening to incoming HTTP requests, and (third argument) a test
 * document location accessible by the server instance.</P>
 * <P>Default values for all three arguments are used if and when they are
 * not detected at invocation time. Specifically, the default value for
 * the host name is "localhost", for the port number it is 8100, and
 * finally for the test document it is "jetty/test-sasl.html" (under
 * the webapps folder).</P>
 * <P>If these default values are not suitable they may be changed by
 * specifying new values for them on the command line as follows:
 * <pre>
 *    java com.mortbay.Util.SaslTest [[[host] port] file]
 * </pre>
 * </P>
 * @version $Id$
 * @author Raif S. Naffah (raif)
 */
public class SaslTest
{
    /* ------------------------------------------------------------------- */
    private static final String[] mechanisms = new String[] {"SM2-SRP"};
    private static final String CRLF = "\r\n";
    private static final String TITLE_START_TAG = "<title>";
    private static final String TITLE_END_TAG = "</title>";
    private static final Hashtable properties = new Hashtable();
    static
    {
        properties.put("cryptix.sasl.username", "test");
        properties.put("cryptix.sasl.password", "test");
        properties.put("cryptix.sasl.srp.replaydetection", "true");
        properties.put("cryptix.sasl.srp.integrity", "true");
        properties.put("cryptix.sasl.srp.confidentiality", "false");
    }

    /* ------------------------------------------------------------------- */
    /** A simple test case for SASL Listener.
     * Accepts, at most three positional parameters: the host name (first
     * argument) and port number (second argument) of where a SASLListener
     * capable jetty is running and listening to incoming HTTP requests,
     * and (third argument) a test document location accessible by the
     * server instance.<p>
     * Default values for all three arguments are used if and when they are
     * not detected at invocation time. Specifically, the default value for
     * the host name is "localhost", for the port number it is 8100, and
     * finally for the test document it is "jetty/test-sasl.html" (under
     * the webapps folder).<p>
     * If these default values are not suitable they may be changed by
     * specifying new values for them on the command line as follows:
     * <pre>
     *    java com.mortbay.Util.SaslTest [[[host] port] file]
     * </pre>
     */
    public static final void main(String[] args)
    {
        try
        {
            SaslClient client = Sasl.createSaslClient(
                mechanisms, "test", "http", "jetty", properties, null);
            if (client == null)
                throw new RuntimeException("Unable to create SASL client");

            if (args == null)
                args = new String[0];

            String host = "localhost";
            String port = "8100";
            String file = "jetty/test-sasl.html";
            switch (args.length)
            {
                case 0:
                    break; // use all defaults
                case 1:
                    file = args[0];
                    break;
                case 2:
                    port = args[0];
                    file = args[1];
                    break;
                case 3:
                    host = args[0];
                    port = args[1];
                    file = args[2];
                    break;
                default:
                    throw new RuntimeException("Too many arguments on command line");
            }

            int portNbr = Integer.parseInt(port);
            Socket socket = new Socket(host, portNbr);
            InputStream inStream = socket.getInputStream();
            OutputStream outStream = socket.getOutputStream();

            outStream.write(client.getMechanismName().getBytes("ASCII"));
            outStream.write(0x00);

            byte[] challenge;
            byte[] response;
            int first, limit;

            // ----- begin authentication phase

            if (client.hasInitialResponse())
            {
                response = client.evaluateChallenge(null);
                // implies that server has last word; ie. client sends nothing
        	       // after server sends last challenge
        		    do
        		    {
                    outStream.write(response);
                    first = inStream.read();
                    limit = inStream.available();
                    challenge = new byte[limit+1];
                    challenge[0] = (byte) first;
                    inStream.read(challenge, 1, limit);
        			     response = client.evaluateChallenge(challenge);
                }
                while (!client.isComplete());
            }
            else
                // implies that client has last word; ie. server sends nothing
        	       // after client sends last response
        	       do
        	       {
                    first = inStream.read();
                    limit = inStream.available();
                    challenge = new byte[limit+1];
                    challenge[0] = (byte) first;
                    inStream.read(challenge, 1, limit);
        			     response = client.evaluateChallenge(challenge);
        			     outStream.write(response);
                }
                while (!client.isComplete());

            // ----- authentication phase completed

            // get secure input and output streams
            inStream = client.getInputStream(inStream);
            outStream = client.getOutputStream(outStream);

            // build HTTP request
            byte[] request = new StringBuffer()
                .append("GET ").append(file).append(" HTTP/1.0")
                .append(CRLF)
                .append("Host: ").append(host).append(":")
                    .append(String.valueOf(port))
                .append(CRLF)
                .append(CRLF)
                .toString()
                .getBytes();

            // write HTTP request
            outStream.write(request);

            // read HTTP response
            first = inStream.read();
            limit = inStream.available();
            response = new byte[limit+1];
            response[0] = (byte) first;
            inStream.read(response, 1, limit);

            String title = new String(response);

            // parse HTTP response
            int start = title.indexOf(TITLE_START_TAG);
            if (start == -1)
                throw new RuntimeException(
                    "Missing "+TITLE_START_TAG+" tag in response");

            title = title.substring(start+TITLE_START_TAG.length());
            title = title.substring(0, title.indexOf(TITLE_END_TAG));

            System.out.println();
            System.out.println("Title = \""+title+"\"");
        }
        catch (IOException x)
        {
            x.printStackTrace(System.err);
        }
    }
}
