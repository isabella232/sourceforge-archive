JETTY - Extensible Java HTTP Server. 
By Mort Bay Consulting Pty. Ltd.  http://www.mortbay.com

INSTALLATION
============
Jetty comes compiled and ready to go. Unpack the Jetty distribution 
to a shared locations (/usr/local/Jetty is a good spot).   

The environment variable JETTY_HOME should be set to this directory. 
For the rest of this README the unix convention of $JETTY_HOME refers 
to the installed directory path (%JETTY_HOME% on dos/NT/windows).

   Unix:
	cd /usr/local
	gunzip < Jetty-X.X.X.tgz | tar xfv -
	JETTY_HOME=/usr/local/Jetty
	export JETTY_HOME

   Win95,NT:
	c:
	cd "\Program Files"
        winzip Jetty-X.X.X.tgz
	set JETTY_HOME="c:\Program files\Jetty"


Make sure your CLASSPATH includes all the jar files in
the $JETTY_HOME/lib directory  eg.

   Unix:
	CLASSPATH=$CLASSPATH:$JETTY_HOME/lib/javax.servlet.jar
        CLASSPATH=$CLASSPATH:$JETTY_HOME/lib/com.mortbay.Jetty.jar


RUNNING JETTY
=============
The demo server can be run with the following command:

    cd $JETTY_HOME
    java [-DDEBUG] com.mortbay.Jetty.Demo

Use a browser to access the Jetty site at:

    http://localhost:port 
    http://127.0.0.1:8080

The Jetty server may be run in the other modes as follows:

  + To run as a config file driven WWW server, configure
    the file JettyServer.prp and then run:

        cd $JETTY_HOME
	java com.mortbay.HTTP.Server [configFile]

  + To run a HTTP file server, in the current directory:

        java com.mortbay.HTTP.Configure.FileServer [port]

  + To run a single servlet:
 
        java com.mortbay.HTTP.Configure.SimpleServletConfig path name class [port]

See the com.mortbay.HTTP.Configure package for more detail
of how to customize Jetty.


HTTP/1.1
========
Servlets by default will only use persistent connections if
the content length is set or the chunked transfer encoding has
been set in the response header.  However, chunking can be used
by default if the java property CHUNK_BY_DEFAULT is set or 
ServletHolder.setChunkByDefault(true) is called, which allows
persistent connections to be used in more situations.


CONTACTS
========
    jetty-support@mortbay.com     - for problems with Jetty
    mortbay@mortbay.com           - for general contact
    jetty-announce@mortbay.com    - for the Jetty mailing list
                                    This is for announcements of interest
                                    to all Jetty users. It is not for
                                    support questions.
    subscribe-jetty-announce@mortbay.com   
                                  - join the mailing list


SUPPORT
=======
Mort Bay provides informal support for Jetty via email, for non-licensed
users.   If you are having a problem with Jetty, please supply the 
following information when sending email to jetty-support.

  Jetty version
  Java version
  Hardware platform.
  Operating System and version.
  Directory Jetty is installed in.
  Value of JETTY_HOME environment variable.
  Value of CLASSPATH environment variable.
  Command line used to run the server.
  Server properties file (if used and modified).


And remember, it's spelled J-e-t-t-y, but it's pronounced "Jetty".

