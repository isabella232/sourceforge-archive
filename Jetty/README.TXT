
Jetty 3
=======  
Jetty is an Open Source HTTP Servlet Server written in 100% Java. 
It is designed to be light weight, high performance, embeddable, 
extensible and flexible, thus making it an ideal platform for serving  
dynamic HTTP requests from any Java application.

This is an ALPHA release of Jetty 3, which is a significant revision of
the Jetty 2 code base. The aims of the Jetty 3 developement include:

  + Fully RFC2616 compliant implementation of HTTP/1.1.
  + Decouple HTTP server from specific Servlet container.
  + Integrate with the jakarta.apache.org  Servlet 2.2 Container.
  + Integrate with the Jetty Servlet 2.1 Container.
  + Remain efficient, embedable & extensible.
  + Utilize java collection classes for improved performance.
  + Improved coding standards and increasing test coverage.

Currently the release contains only a skeleton 2.1 servlet container. 
Work to date in the HTTP package has concentrated on RFC2616 compliance, 
performance and testing.  
See the VERSION.TXT and FEATURES.TXT files for more detail.


Environment
===========
Development has been done in JDK1.1 with the 1.2 Collection classes.
The compiled classes excluding test harnesses are put into lib/jetty3.jar,
so the following CLASSPATH is required (or the non unix equivalent):

  CLASSPATH=$JETTY3_HOME/lib/jetty3.jar:$JETTY3_HOME/lib/collections.jar

To compile the source with JDK1.1 the following class path should be used.

  CLASSPATH=$JETTY3_HOME/src:$JETTY3_HOME/lib/collections.jar

To compile with JDK1.2, the com.sun.java.util.collections package
must be removed by running the bin/useJava2Collections script (the
distribution includes the results of this in src1.2 directory).
Then compile using the class path:

  CLASSPATH=$JETTY3_HOME

In order to support multiple versions of the javax.servlet API, the 
javax.servlet.jar is loaded as a resource as part of the particular
ServletHandler.  Each ServletHandler instantiated has it's own classloader
and may have it own class path.


Test Harnesses
==============
The class com.mortbay.HTTP.TestRFC2616 is the start of 
a test harness linked to the RFC document.   This and the other test
harnesses can be run with

  java com.mortbay.Util.TestHarness
  java com.mortbay.HTTP.TestHarness

Note that the test harnesses are not in the jar file, so you will need
to build all the source and run with the following classpath(or the non 
unix equivalent):

  CLASSPATH=$JETTY3_HOME/src:$JETTY3_HOME/lib/collections.jar


WWW Server
==========
A HTTP server can be run with
 
  java com.mortbay.HTTP.HttpServer

By default it runs on port 8080 and serves files and simple request dumps. 
If run from the install directory, a set of simple demo pages are served.
The server can be configured by using the options:

  -a [<addr>:]port    - listen on a port
  -d <path>           - DumpHandler at path
  -f <path>=<dir>     - FileHandler at path
  -h <path>=<class>   - Map a handler at path
  -v <host>=<alias>   - Remaining options are for virtual host.
  -ds <path>=<classpath>
                      - Add a dynamic servlet directory.
  -s <path>=<class>
                      - Add a servlet.


The default is equivalent to
  java com.mortbay.HTTP.HttpServer            \
       -a 8080                                \
       -f /=.                                 \
       -d /                                   \
       -ds /servlet/*=./servlets             \
       -h /=com.mortbay.HTTP.NotFoundHandler  

Multiple addresses, handlers and hosts can be specified.

Servers can also be configured in code.  The default server can be
configured and started with:

   // Make server
   HttpServer server = new HttpServer();

   // Configure handlers
   server.mapFiles(null,"/",".");
   server.mapHandler(null,"/",new DumpHandler());
   server.mapHandler(null,"/",new NotFoundHandler());
   server.addListener(new SocketListener(server,new InetAddrPort(8080)));

   // Start handlers and listener
   server.startAll();


Distribution
============
Jetty distributions and information is available from:

   http://jetty.mortbay.com
   ftp://jetty.mortbay.com/pub
   

How to Contribute
=================
Comments, contributions, feedback, bugs, testing, etc. please....

