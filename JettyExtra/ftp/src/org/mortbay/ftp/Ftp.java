// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

/*
Optimus Solutions Pty Ltd of Frenchs Forest and Mort Bay Consulting
Pty. Ltd. of Balmain, hold co-copyright on the org.mortbay.ftp package.
*/

package org.mortbay.ftp;

import org.mortbay.util.Code;
import org.mortbay.util.LineInput;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.Vector;

// ===========================================================================
/** FTP Client.
 * <p>
 * File Transfer Protocol client class. Provides basic FTP client
 * functionality in an Asynchronous interface.
 *
 * <p><h4>Notes</h4>
 * <p> see rfc959.
 *
 * <p><h4>Usage</h4>
 * <pre>
 * Ftp ftp = new Ftp(InetAddress.getByName("RemoteHost"),
 *                   "TestUser",
 *                   "TestPass");
 * ftp.setType(Ftp.IMAGE);
 * ftp.startGet("RemoteFileName","LocalFileName");
 * ftp.waitUntilTransferComplete();
 *
 * ftp.startPut("LocalFileName","RemoteFileName");
 * ftp.waitUntilTransferComplete();
 * </pre>
 *
 * @version $Id$
 * @author Greg Wilkins
 */
public class Ftp
{
    /* -------------------------------------------------------------------- */
    public final static String anonymous = "anonymous";
    public final static int defaultPort = 21;
    public final static char ASCII = 'A';
    public final static char LOCAL = 'L';
    public final static char EBCDIC = 'E';
    public final static char IMAGE = 'I';
    public final static char BINARY = 'I';
    public final static char NON_PRINT = 'N';
    public final static char TELNET = 'T';
    public final static char CARRIAGE_CONTROL = 'C';

    /* -------------------------------------------------------------------- */
    Socket command = null;
    CmdReplyStream in = null;
    Writer out=null;
    DataPort transferDataPort=null;
    Exception transferException=null;

    /* -------------------------------------------------------------------- */
    /** Ftp constructor
     */
    public Ftp()
    {}

    /* -------------------------------------------------------------------- */
    /** Ftp constructor
     * Construct an FTP endpoint, open the default command port and
     * authenticate
     * the user.
     * @param hostAddr The IP address of the remote host
     * @param username User name for authentication, null implies no user
     *                 required
     * @param password Password for authentication, null implies no password
     * @exception FtpException For local problems or negative server responses
     */
    public Ftp(InetAddress hostAddr,
               String username,
               String password)
         throws FtpException, IOException
    {
        this(hostAddr,defaultPort,username,password);
    }

    /* -------------------------------------------------------------------- */
    /** Ftp constructor
     * Construct an FTP endpoint, open the command port and authenticate
     * the user.
     * @param hostAddr The IP address of the remote host
     * @param port     The port to use for the control connection. The
     *                 default value is used if the port is 0.
     * @param username User name for authentication, null implies no user
     *                 required
     * @param password Password for authentication, null implies no password
     * @exception FtpException For local problems or negative server responses
     */
    public Ftp(InetAddress hostAddr,
               int port,
               String username,
               String password)
         throws FtpException, IOException
    {
        open(hostAddr,port);
        authenticate(username,password);
    }


    /* -------------------------------------------------------------------- */
    public InetAddress getLocalAddress() {
        return command.getLocalAddress();
    }

    /* -------------------------------------------------------------------- */
    void cmd(String cmd)
         throws IOException
    {
        Code.debug("Command="+cmd);
        out.write(cmd);
        out.write("\015\012");
        out.flush();
    }


    /* -------------------------------------------------------------------- */
    /** Open connection
     * @param hostAddr The IP address of the remote host
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void open(InetAddress hostAddr)
         throws FtpException, IOException
    {
        open(hostAddr,defaultPort);
    }

    /* -------------------------------------------------------------------- */
    /** Open connection
     * @param hostAddr The IP address of the remote host
     * @param port     The port to use for the control connection. The
     *                 default value is used if the port is 0.
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void open(InetAddress hostAddr,
                                  int port)
         throws FtpException, IOException
    {
        Code.assertTrue(command==null,"Ftp already opened");

        if (port==0)
            port=defaultPort;
        command = new Socket(hostAddr,port);
        in = new CmdReplyStream(command.getInputStream());
        out= new OutputStreamWriter(command.getOutputStream(),"ISO8859_1");
        in.waitForCompleteOK();
        Code.debug("Command Port Opened");
    }

    /* -------------------------------------------------------------------- */
    /** Authenticate User
     * @param username User name for authentication, null implies no user
     *                 required
     * @param password Password for authentication, null implies no password
     * @exception FtpException For local problems or negative server responses
     */
     public synchronized void authenticate(String username,
                                           String password)
         throws FtpException,IOException
    {
        waitUntilTransferComplete();
        cmd("USER "+username);
        CmdReply reply = in.readReply();
        if (reply.intermediate())
        {
            Code.debug("Sending password");
            cmd("PASS "+password);
        }
        else if (reply.positive())
            Code.debug("No password required");
        else
            throw new FtpReplyException(reply);

        in.waitForCompleteOK();
        Code.debug("Authenticated");
    }


    /* ------------------------------------------------------------ */
    /** Set the connection data type.
     * The data type is not interpreted by the FTP client.
     * @param type One of Ftp.ASCII, Ftp.EBCDIC or Ftp.IMAGE
     * @exception FtpException For local problems or negative server responses
     * @exception IOException IOException
     */
    public synchronized void setType(char type)
         throws FtpException,IOException
    {
        waitUntilTransferComplete();

        cmd("TYPE "+type);
        in.waitForCompleteOK();
    }

    /* ------------------------------------------------------------ */
    /** Set the connection data type.
     * The data type is not interpreted by the FTP client.
     * @param type One of Ftp.ASCII or Ftp.EBCDIC
     * @param param One of Ftp.NON_PRINT, Ftp.TELNET or Ftp.CARRIAGE_CONTROL
     * @exception FtpException For local problems or negative server responses
     * @exception IOException IOException
     */
    public synchronized void setType(char type, char param)
         throws FtpException,IOException
    {
        waitUntilTransferComplete();

        cmd("TYPE "+type+' '+param);
        in.waitForCompleteOK();
    }

    /* ------------------------------------------------------------ */
    /** Set the connection data type to Local.
     * The data type is not interpreted by the FTP client.
     * @param length Length of word.
     * @exception FtpException For local problems or negative server responses
     * @exception IOException IOException
     */
    public synchronized void setType(int length)
         throws FtpException,IOException
    {
        waitUntilTransferComplete();

        cmd("TYPE "+Ftp.LOCAL+' '+length);
        in.waitForCompleteOK();
    }


    /* -------------------------------------------------------------------- */
    /** Command complete query
     * @return    true if the no outstanding command is in progress, false
     *            if there is an outstanding command or data transfer.
     * @exception FtpException For local problems or negative server responses.
     *            The problem may have been detected before the call to
     *            complete during a data transfer, but is only reported when
     *            the call to complete is made.
     */
    public synchronized boolean transferComplete()
         throws FtpException, IOException
    {
        if (transferException!=null)
        {
            if (transferException instanceof FtpException)
                throw (FtpException)transferException;
            if (transferException instanceof IOException)
                throw (IOException)transferException;
            Code.fail("Bad exception type",transferException);
        }
        return (transferDataPort==null);
    }


    /* -------------------------------------------------------------------- */
    /** Wait until Transfer is complete.
     * Used to synchronous with an asynchronous transfer.  If any exceptions
     * occurred during the transfer, the first exception will be thrown by
     * this method.  Multiple threads can wait on the one transfer and all
     * will be given a reference to any exceptions.
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void waitUntilTransferComplete()
         throws FtpException,IOException
    {
        while (transferDataPort!=null)
        {
            Code.debug("waitUntilTransferComplete...");
            try{wait(10000);}catch(InterruptedException e){};
        }

        if (transferException!=null)
        {
            if (transferException instanceof FtpException)
                throw (FtpException)transferException;
            if (transferException instanceof IOException)
                throw (IOException)transferException;
            Code.fail("Bad exception type",transferException);
        }
    }


    /* -------------------------------------------------------------------- */
    /** Notification from DataPort that transfer is complete.
     * Called by DataPort.
     * @param dataPortException Any exception that occurred on the dataPort
     */
    synchronized void transferCompleteNotification(Exception dataPortException)
    {
        Code.debug("Transfer Complete");
        transferException=dataPortException;
        try{
            if (in!=null)
                in.waitForCompleteOK();
        }
        catch (Exception e){
            if (transferException==null)
                transferException=e;
        }
        finally{
            transferDataPort=null;
            notifyAll();
            transferCompleteNotification();
        }
    }

    /* -------------------------------------------------------------------- */
    /** Transfer completion notification.
     * This protected member can be overridden in a derived class as an
     * alternate notification mechanism for transfer completion.
     * Default implementation does nothing.
     */
    protected void transferCompleteNotification()
    {
    }



    /* -------------------------------------------------------------------- */
    /** Start get file
     * Start a file transfer remote file to local file.
     * Completion of the transfer can be monitored with the transferComplete() or
     * waitUntilTransferComplete() methods.
     * @param remoteName Remote file name
     * @param localName Local file name
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void startGet(String remoteName, String localName)
         throws FtpException,IOException
    {
        FileOutputStream file = new FileOutputStream(localName);
        startGet(remoteName,file);
    }
    
    /* -------------------------------------------------------------------- */
    /** Start get file
     * Start a file transfer remote file to local inputStream. 
     * Completion of the transfer can be monitored with the transferComplete() or
     * waitUntilTransferComplete() methods.
     * @param remoteName Remote file name
     * @return InputStream, the data fetched may be read from this inputStream.
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized InputStream startGet(String remoteName)
         throws FtpException,IOException
    {
        PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout);
        startGet(remoteName,pout);
        return pin;
    }

    /* -------------------------------------------------------------------- */
    /** Start get file
     * Start a file transfer remote file to local file.
     * Completion of the transfer can be monitored with the transferComplete() or
     * waitUntilTransferComplete() methods.
     * @param remoteName Remote file name
     * @param destination OutputStream to which the received file is written
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void startGet(String remoteName,
                                      OutputStream destination)
         throws FtpException,IOException
    {
        waitUntilTransferComplete();

        transferException=null;
        transferDataPort = new DataPort(this,destination);
        try{
            cmd(transferDataPort.getFtpPortCommand());
            in.waitForCompleteOK();
            cmd("RETR "+remoteName);
            in.waitForPreliminaryOK();
        }
        catch(FtpException e){
            transferDataPort.close();
            transferDataPort=null;
            throw e;
        }
        catch(IOException e){
            transferDataPort.close();
            transferDataPort=null;
            throw e;
        }
    }


    /* -------------------------------------------------------------------- */
    /** Start put file
     * Start a file transfer local file to input remote file.
     * Completion of the transfer can be monitored with the transferComplete() or
     * waitUntilTransferComplete() methods.
     * @param remoteName Remote file name
     * @param localName Local file name
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void startPut(String localName, String remoteName)
         throws FtpException, IOException
    {
        FileInputStream file = new FileInputStream(localName);
        startPut(file,remoteName);
    }
    
    /* -------------------------------------------------------------------- */
    /** Start put file
     * Start a file transfer local file to input remote file.
     * Completion of the transfer can be monitored with the transferComplete() or
     * waitUntilTransferComplete() methods.
     * @param remoteName Remote file name
     * @return OutputStream Data written to this output stream is sent
     * to the remote file.
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized OutputStream startPut(String remoteName)
         throws FtpException, IOException
    {
        PipedOutputStream pout = new PipedOutputStream();
        PipedInputStream pin = new PipedInputStream(pout);
        startPut(pin,remoteName);
        return pout;
    }


    /* -------------------------------------------------------------------- */
    /** Start put file
     * Start a file transfer local file to input remote file.
     * Completion of the transfer can be monitored with the transferComplete() or
     * waitUntilTransferComplete() methods.
     * @param remoteName Remote file name
     * @param localName Local file name
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void startPut(InputStream source, String remoteName)
         throws FtpException, IOException
    {
        waitUntilTransferComplete();

        transferException=null;
        transferDataPort = new DataPort(this,source);
        try{
            cmd(transferDataPort.getFtpPortCommand());
            in.waitForCompleteOK();
            cmd("STOR "+remoteName);
            in.waitForPreliminaryOK();
        }
        catch(FtpException e){
            transferDataPort.close();
            transferDataPort=null;
            throw e;
        }
        catch(IOException e){
            transferDataPort.close();
            transferDataPort=null;
            throw e;
        }
    }

    /* -------------------------------------------------------------------- */
    /** Start passive get file
     * Start a file transfer remote file to local file.
     * Completion of the transfer can be monitored with the transferComplete() or
     * waitUntilTransferComplete() methods.
     * @param remoteName Remote file name
     * @param localName Local file name
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void startPasvGet(String remoteName, String localName)
         throws FtpException,IOException
    {
        FileOutputStream file = new FileOutputStream(localName);
        startPasvGet(remoteName,file);
    }

    /* -------------------------------------------------------------------- */
    public synchronized void startPasvGet(String remoteName,
					  OutputStream destination)
	throws FtpException,IOException
    {
        waitUntilTransferComplete();
        transferException=null;

        // Put it into passive mode
        cmd("PASV");
        CmdReply reply = in.waitForCompleteOK();

	// Work out the dataport
	String pasv=reply.text.substring(reply.text.lastIndexOf("(")+1,
					 reply.text.lastIndexOf(")"));
	int i1=pasv.indexOf(",");
	i1=pasv.indexOf(",",i1+1);
	i1=pasv.indexOf(",",i1+1);
	i1=pasv.indexOf(",",i1+1);
	int i2=pasv.indexOf(",",i1+1);
	int dataPort =
	    256*Integer.parseInt(pasv.substring(i1+1,i2))+
	    Integer.parseInt(pasv.substring(i2+1));

	// Setup the dest server to send the file
        cmd("RETR "+remoteName);

	// start the send
        transferDataPort = new DataPort(this,
					destination,
					command.getInetAddress(),
					dataPort);
	in.waitForPreliminaryOK();
    }

    /* -------------------------------------------------------------------- */
    /** Start passive put file
     * Start a file transfer local file to input remote file.
     * Completion of the transfer can be monitored with the transferComplete() or
     * waitUntilTransferComplete() methods.
     * @param remoteName Remote file name
     * @param localName Local file name
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void startPasvPut(String localName, String remoteName)
         throws FtpException, IOException
    {
        FileInputStream file = new FileInputStream(localName);
        startPasvPut(file,remoteName);
    }

    /* -------------------------------------------------------------------- */
    public synchronized void startPasvPut(InputStream source,
					  String remoteName)
	throws FtpException,IOException
    {
        waitUntilTransferComplete();
        transferException=null;

        // Put it into passive mode
        cmd("PASV");
        CmdReply reply = in.waitForCompleteOK();

	// Work out the dataport
	String pasv=reply.text.substring(reply.text.lastIndexOf("(")+1,
					 reply.text.lastIndexOf(")"));
	int i1=pasv.indexOf(",");
	i1=pasv.indexOf(",",i1+1);
	i1=pasv.indexOf(",",i1+1);
	i1=pasv.indexOf(",",i1+1);
	int i2=pasv.indexOf(",",i1+1);
	int dataPort =
	    256*Integer.parseInt(pasv.substring(i1+1,i2))+
	    Integer.parseInt(pasv.substring(i2+1));

	// Setup the dest server to store the file
        cmd("STOR "+remoteName);

	// start the send
        transferDataPort = new DataPort(this,source,command.getInetAddress(),dataPort);
	in.waitForPreliminaryOK();
    }


     /* -------------------------------------------------------------------- */
     /** Create remote directory
      * @param remoteName The remote directory name
      * @exception FtpException For local problems or negative server responses
      */
     public synchronized void mkdir(String remoteName)
          throws FtpException, IOException
     {
         waitUntilTransferComplete();
         cmd("MKD "+remoteName);
         in.waitForCompleteOK();
         Code.debug("Created "+remoteName);
     }


    /* -------------------------------------------------------------------- */
    /** send file
     * Do a file transfer remote file to remote file on another server.
     * This is a synchronous method, unlike startGet and startPut.
     * @param srcName Remote file name on source server
     * @param destAddr The IP address of the destination host
     * @param port     The port to use for the control connection. The
     *                 default value is used if the port is 0.
     * @param username User name for authentication, null implies no user
     *                 required
     * @param password Password for authentication, null implies no password
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void sendFile(String srcName,
                                      InetAddress destAddr,
                                      int destPort,
                                      String username,
                                      String password,
                                      String destName)
         throws FtpException,IOException
    {
        Code.debug("startSend("+
                   srcName+','+
                   destAddr+','+
                   destPort+','+
                   username+','+
                   password+','+
                   destName+')');

        waitUntilTransferComplete();

        // Make connection with other server
        Ftp destFtp = new Ftp(destAddr,destPort,username,password);

        // Put it into passive mode
        destFtp.cmd("PASV");
        CmdReply reply = destFtp.in.waitForCompleteOK();

        // Tell the src server the port
        String portCommand = "PORT "+
            reply.text.substring(reply.text.lastIndexOf("(")+1,
                                 reply.text.lastIndexOf(")"));
        Code.debug(portCommand);
        cmd(portCommand);
        in.waitForCompleteOK();

            // Setup the dest server to store the file
        destFtp.cmd("STOR "+destName);

            // start the send
        cmd("RETR "+srcName);
        in.waitForCompleteOK();
        destFtp.in.waitForCompleteOK();

    }



    /* -------------------------------------------------------------------- */
    /** Report remote working directory
     * @return The remote working directory
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized String workingDirectory()
         throws FtpException,IOException
    {
        waitUntilTransferComplete();

        cmd("PWD");
        CmdReply reply =in.waitForCompleteOK();
        Code.debug("PWD="+reply.text);

        return reply.text;
    }


    /* -------------------------------------------------------------------- */
    /** Set remote working directory
     * @param dir The remote working directory
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void workingDirectory(String dir)
         throws FtpException,IOException
    {
        waitUntilTransferComplete();

        cmd("CWD "+dir);
        CmdReply reply =in.waitForCompleteOK();
        Code.debug("CWD="+reply.text);
    }

    /* -------------------------------------------------------------------- */
    /** Rename remote file
     * @param oldName The original file name
     * @param newName The new file name
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void rename(String oldName, String newName)
         throws FtpException, IOException
    {
        waitUntilTransferComplete();

        cmd("RNFR "+oldName);
        in.waitForIntermediateOK();
        cmd("RNTO "+newName);
        in.waitForCompleteOK();
        Code.debug("Renamed");
    }


    /* -------------------------------------------------------------------- */
    /** Delete remote file
     * @param remoteName The remote file name
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void delete(String remoteName)
         throws FtpException, IOException
    {
        waitUntilTransferComplete();
        cmd("DELE "+remoteName);
        in.waitForCompleteOK();
        Code.debug("Deleted "+remoteName);
    }


    /* -------------------------------------------------------------------- */
    /** Abort transfer command
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void abort()
         throws FtpException, IOException
    {
        cmd("ABOR");
        if (transferDataPort==null)
            in.waitForCompleteOK();
        else
            waitUntilTransferComplete();
    }


    /* -------------------------------------------------------------------- */
    /** Get list files in remote working directory
     * @return Array of file names
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized Vector list()
         throws FtpException, IOException
    {
        Code.debug("list");
        waitUntilTransferComplete();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        transferException=null;
        transferDataPort = new DataPort(this,bout);
        try{
            cmd(transferDataPort.getFtpPortCommand());
            in.waitForCompleteOK();
            cmd("NLST");
            in.waitForPreliminaryOK();
            waitUntilTransferComplete();
        }
        catch(FtpReplyException e)
        {
            transferDataPort.close();
            transferDataPort=null;
            // Return null if there was no directory.
            if ("550".equals(e.reply.code))
                return null;
            throw e;
        }
        catch(FtpException e)
        {
            transferDataPort.close();
            transferDataPort=null;
            throw e;
        }
        catch(IOException e)
        {
            transferDataPort.close();
            transferDataPort=null;
            throw e;
        }

        LineInput in = new LineInput(
            new ByteArrayInputStream(bout.toByteArray()));

        Vector listVector = new Vector();
        String file;
        while((file=in.readLine())!=null)
            listVector.addElement(file);

        Code.debug("Got list "+listVector.toString());
        return listVector;
    }


    /* -------------------------------------------------------------------- */
    /** Get remote server status
     * @return String description of server status
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized String status()
         throws FtpException, IOException
    {
        waitUntilTransferComplete();

        cmd("STAT");
        CmdReply reply =in.waitForCompleteOK();
        Code.debug("STAT="+reply.text);

        return reply.text;
    }

    /* -------------------------------------------------------------------- */
    /** close the FTP session
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void close()
         throws IOException
    {
        if (out!=null)
        {
            cmd("QUIT");
            if (command!=null)
            {
                command.close();
                command=null;
                in=null;
                out=null;
                if (transferDataPort!=null)
                    transferDataPort.close();
            }
        }
    }


    /* -------------------------------------------------------------------- */
    /** Get file from a URL spec
     * @param url string of the form:
     * "ftp://username:password@host:port/path/to/file"
     * @param out the OutputStream to place the fetched file in
     */
    public void getUrl(String url, OutputStream out)
         throws FtpException,IOException
    {
        Code.assertTrue(url.startsWith("ftp://"),
                    "url must be for the form: "+
                    "ftp://username:password@host:port/path/to/file");

        String uri = url.substring(6);
        if (uri.indexOf("?")>=0)
            uri=uri.substring(0,uri.indexOf("?"));

        StringTokenizer tok = new StringTokenizer(uri,":@/",true);

        String user="anonymous";
        String pass="org.mortbay.ftp@"+InetAddress.getLocalHost().getHostName();
        String host=null;
        String port=null;
        String path=null;

        String s[]=new String[3];
        int i=0;

    loop:

        while(tok.hasMoreTokens())
        {
            String t = tok.nextToken();
            if (t.length()==1)
            {
                switch (t.charAt(0))
                {
                  case ':':
                      continue;
                  case '@':
                      user=s[0];
                      pass=s[1];
                      i=0;
                      s[0]=null;
                      s[1]=null;
                      continue;

                  case '/':
                      host=s[0];
                      if (i==2)
                          port=s[1];
                      try{
                          path=tok.nextToken(" \n\t");
                      }
                      catch(NoSuchElementException e){
                          path="/";
                      }

                      break loop;
                }
            }

            s[i++]=t;
        }

        Code.debug("getUrl=ftp://"+user+
                   ((pass==null)?"":(":"+pass))+
                   "@"+host+
                   ((port==null)?"":(":"+port))+
                   ((path.startsWith("/"))?path:("/"+path)));

        close();
        if (port!=null)
            open(InetAddress.getByName(host),Integer.parseInt(port));
        else
            open(InetAddress.getByName(host));

        authenticate(user,pass);
        startGet(path,out);
        waitUntilTransferComplete();
    }


    /* -------------------------------------------------------------------- */
    /** Delete remote directory
     * @param remoteName The remote directory name
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized void rmdir(String remoteName)
        throws FtpException, IOException
    {
        waitUntilTransferComplete();
        cmd("RMD "+remoteName);
        in.waitForCompleteOK();
        Code.debug("Deleted "+remoteName);
    }


    /* -------------------------------------------------------------------- */
    /**
     * 
     * 
     */
    public synchronized String getLastModifiedTime(String remoteName)
        throws FtpException, IOException
    {
        waitUntilTransferComplete();
        cmd("MDTM "+remoteName);
        CmdReply reply = in.waitForCompleteOK();
        return reply.text;
    }


    /* -------------------------------------------------------------------- */
    /**
     * Get a directory listing from the remote server.
     * @return Array of file information.
     * @exception FtpException For local problems or negative server responses
     */
    public synchronized Vector list( String mask )
        throws FtpException, IOException
    {
        Code.debug("list [",mask,"]");
        waitUntilTransferComplete();

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        transferException=null;
        transferDataPort = new DataPort(this,bout);
        try{
            cmd(transferDataPort.getFtpPortCommand());
            in.waitForCompleteOK();
            if( mask == null ) {
                cmd("LIST");
            }
            else {
                cmd( "LIST "+mask );
            }
            in.waitForPreliminaryOK();
            waitUntilTransferComplete();
        }
        catch(FtpReplyException e)
        {
            transferDataPort.close();
            transferDataPort=null;
            // Return null if there was no directory.
            if ("550".equals(e.reply.code))
                return null;
            throw e;
        }
        catch(FtpException e)
        {
            transferDataPort.close();
            transferDataPort=null;
            throw e;
        }
        catch(IOException e)
        {
            transferDataPort.close();
            transferDataPort=null;
            throw e;
        }

        LineInput in = new LineInput(
                                     new ByteArrayInputStream(bout.toByteArray()));

        Vector listVector = new Vector();
        String file;
        while((file=in.readLine())!=null)
            listVector.addElement(file);

        Code.debug("Got list "+listVector.toString());
        return listVector;
    } 

    /* -------------------------------------------------------------------- */
    public static void main(String[] args)
    {
        try{
            if (args.length!=1 &&
                (args.length<3 || args.length>=4 &&
                !(args[3].equals("del") ||
                  args[3].equals("ren") ||
                  args[3].equals("get") ||
                  args[3].equals("pget") ||
                  args[3].equals("snd") ||
                  args[3].equals("put") ||
                  args[3].equals("pput") ||
                  args[3].equals("mkdir") ||
                  args[3].equals("url"))))
            {
                System.err.println("Usage: java org.mortbay.ftp.Ftp host user password [ del|[p]get|[p]put|ren|snd|mkdir args... ]");
                System.err.println("       java org.mortbay.ftp.Ftp ftp://user:pass@host:port/file/path");
                System.exit(1);
            }

            if (args.length==1)
            {
                Ftp ftp = new Ftp();
                ftp.getUrl(args[0],System.out);
            }
            else
            {
                Ftp ftp = new Ftp(InetAddress.getByName(args[0]),
                                  args[1],args[2]);

                //try{
                //    System.out.println("Status: "+ftp.status());
                //}catch (Exception ignore){}

                if (args.length==3)
                    System.out.println(ftp.list());
                else
                {
                    for (int file=4; file<args.length; file++)
                    {
                        System.out.println(args[3]+" "+args[file]);

                        try{
                            if (args[3].equals("del"))
                                ftp.delete(args[file]);
                            else if (args[3].equals("ren"))
                                ftp.rename(args[file],args[++file]);
                            else  if (args[3].equals("get"))
                            {
                                if (file+1==args.length)
                                    ftp.startGet(args[file],System.out);
                                else
                                    ftp.startGet(args[file],args[++file]);
                            }
                            else  if (args[3].equals("pget"))
                            {
                                if (file+1==args.length)
                                    ftp.startPasvGet(args[file],System.out);
                                else
                                    ftp.startPasvGet(args[file],args[++file]);
                            }
                            else if (args[3].equals("put"))
                                ftp.startPasvPut(args[file],args[++file]);
                            else if (args[3].equals("pput"))
                                ftp.startPut(args[file],args[++file]);
                            else if (args[3].equals("snd"))
                                ftp.sendFile(args[file],
                                             InetAddress.getByName(args[++file]),
                                             0,
                                             args[1],args[2],
                                             args[++file]);
                            else if (args[3].equals("url"))
                                ftp.getUrl(args[++file],System.err);
                            else if (args[3].startsWith("mkd"))
			        ftp.mkdir(args[file]);

                            ftp.waitUntilTransferComplete();
                        }
                        catch(Exception e){
                            System.err.println(e.toString());
                            Code.debug(args[3]+" failed",e);
                        }
                    }
                }
            }
        }
        catch(Exception e){
            System.err.println(e.toString());
            Code.debug("Ftp failed",e);
        }
        finally{
            Code.debug("Exit main thread");
        }
    }

   
}
