JETTY - Extensible Java HTTP Server. 
By Mort Bay Consulting Pty. Ltd.  http://www.mortbay.com.au

INSTALLATION
    Jetty comes compiled and ready to go:
  
    1. Unpack the Jetty distribution to a shared locations 
       (/usr/local/Jetty-X.X is a good spot).   The distribution
       names this directory Jetty-X.X, but you may wish to rename
       this (or link it) to MortBay if other MortBay packages
       are to be installed.

       The environment variable MORTBAY_HOME should be set to 
       this directory. For the rest of this README the unix 
       convention of $MORTBAY_HOME refers to the installed
       directory path (%MORTBAY_HOME% on dos/NT/windows).

       Unix:
          cd /usr/local
          gunzip < Jetty-X.X.tgz | tar xfv -
	  ln -s Jetty-X.X MortBay
          MORTBAY_HOME=/usr/local/MortBay
          export MORTBAY_HOME

       Win95,NT:
          c:
          cd "\Program Files"
          gunzip Jetty-X.X.tgz
          tar xfv Jetty-X.X.tar
          set MORTBAY_HOME="c:\Program files\Jetty-X.X"


    2. Make sure your CLASSPATH includes all the jar files in
       the $MORTBAY_HOME/lib directory  eg.
       
       Unix:
          CLASSPATH=$CLASSPATH:$MORTBAY_HOME/lib/javax.servlet.jar:$MORTBAY_HOME/lib/com.mortbay.Jetty.jar

    3. cd to $MORTBAY_HOME

    4. Run the demo server with the following commands:

           java [-DDEBUG] com.mortbay.Jetty.Demo [port]

    5. Use a browser to access the Jetty site at
           http://localhost:port
       where the default port is 8080. The Demo server provides 
       documentation and demonstrations.

    6. The server may be run in the other modes as follows:
	+ To run a HTTP file server, in the current directory:
	   java com.mortbay.HTTP.Configure.FileServer [port]

        + To run a single servlet:
           java com.mortbay.HTTP.Configure.SimpleServletConfig path name class [port]
  
       See the com.mortbay.HTTP.Configure package for more detail
       of how to customize Jetty.


HTTP/1.1
    + This has not been fully tested yet, nor even used against
      many HTTP/1.1 clients. Please give lots of feed back.

    + Servlets by default will only use persistent connections if
      the content length is set or the chunked transfer encoding has
      been set in the response header.  However, chunking can be used
      by default if the java property CHUNK_BY_DEFAULT is set or 
      ServletHolder.setChunkByDefault(true) is called, which allows
      persistent connections to be used in more situations.

CONTACTS
    jetty-support@mortbay.com     - for problems with Jetty
    mortbay@mortbay.com           - for general contact
    jetty-list@mortbay.com        - for the Jetty mailing list
    jetty-subscribe@mortbay.com   - join the mailing list




