JETTY - Extensible Java HTTP Server. 
By Mort Bay Consulting Pty. Ltd.  http://www.mortbay.com.au

INSTALLATION
    Jetty comes compiled and ready to go:
  
    1. Unpack the Jetty distribution (/usr/local is a good spot)

          cd /usr/local
          gunzip < MortBay-X.X.tgz | tar xfv -
	  ln -s MortBay-X.X MortBay

    2. Make sure your CLASSPATH includes all the jar files in
       /usr/local/MortBay/lib

    3. cd to /usr/local/MortBay

    4. Run a server:

        + To run the demo server:
           java [-DDEBUG] com.mortbay.Jetty.Demo [port]

	+ To run a HTTP file server, in the current directory:
	   java com.mortbay.HTTP.Configure.FileServer [port]

        + To run a single servlet:
           java com.mortbay.HTTP.Configure.SimpleServletConfig path name class [port]

    5. Use a browser to access the Jetty site at
           http://localhost:port
       where the default port is 8080.

    6. The Demo server provides documentation and demonstrations.

HTTP/1.1
    + This has not been fully tested yet, nor even used against
      many HTTP/1.1 clients. Please give lots of feed back.

    + Servlets by default will only use persistent connections if
      the content lenght is set or the chunked transfer encoding has
      been set in the response header.  However, chunking can be used
      by default if the java property CHUNK_BY_DEFAULT is set or 
      ServletHolder.setChunkByDefault(true) is called, which allows
      persistent connections to be used in more situations.

CONTACTS
    jetty-support@mortbay.com     - for problems with Jetty
    mortbay@mortbay.com           - for general contact
    jetty-list@mortbay.com        - for the Jetty mailing list
    jetty-subscribe@mortbay.com   - join the mailing list
