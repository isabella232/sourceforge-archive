
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

Currently the release contains the Utils package (with about half a 
cleanup done) and the HTTP package. Work to date in the HTTP package 
has concentrated on RFC2616 compliance, performance and testing.  
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
must be removed by running the bin/useJava2Collections script must be
run in the src directory and then compile using the class path:

  CLASSPATH=$JETTY3_HOME



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

The default is equivalent to
  java com.mortbay.HTTP.HttpServer            \
       -a 8080                                \
       -f /=.                                 \
       -d /                                   \
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
Currently the jetty.mortbay.com host has a dead hard disk, so no public
CVS repository is available yet.

For now, the package and collections jar will be made available on the
Mort Bay FTP site or the document vault of the jetty3-discuss egroups page:

   ftp://ftp.mortbay.com/pub
   http://www.egroups.com/docvault/jetty3-discuss/


How to Contribute
=================
Comments, contributions, feedback, bugs, testing, etc. please....

The mailing list for Jetty3 development can be subscribed to via
  http://www.egroups.com/group/jetty3-discuss

Other information can be obtained via
  http://www.mortbay.com/software/Jetty.html

A dedicated project host will soon be available at
  http://jetty.mortbay.com
