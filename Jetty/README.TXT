
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
  + Implement Servlet 2.2 Container.
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

  CLASSPATH=$JETTY3_HOME/lib/com.mortbay.jetty.jar:$JETTY3_HOME/lib/collections.jar:$JETTY3_HOME/lib/javax.servlet.jar

To compile the source with JDK1.1 the following class path should be used.

  CLASSPATH=$JETTY3_HOME/src:$JETTY3_HOME/lib/collections.jar:$JETTY3_HOME/lib/javax.servlet.jar

To compile with JDK1.2, the com.sun.java.util.collections package
must be removed by running the bin/useJava2Collections script (the
distribution includes the results of this in src1.2 directory).
Then compile using the class path:

  CLASSPATH=$JETTY3_HOME/src:$JETTY3_HOME/lib/javax.servlet.jar


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

A Demo server server can be run on port 8080 with the command:

  java com.mortbay.Jetty.Demo


A raw HTTP server can be run with the command
 
  java com.mortbay.HTTP.HttpServer

By default it runs on port 8080 and serves files from "./FileBase" and 
dynamic servlet from "./servlets" and the system class path. 
The server can be configured by using command line options, which can 
be displayed with 

  java com.mortbay.HTTP.HttpServer -?

Servers can also be configured in code.  The demo server is configured
with code similar to:

   // Make server
   HttpServer server = new HttpServer();

   server.addListener(new InetAddrPort(8080));

   // Configure handlers
   HandlerContext context;
   context=server.getContext(null,"/");
   context.setFileBase(".");
   context.addServlet("/dump,/dump/*","com.mortbay.Servlet.Dump");
   context.setServingFiles(true);
   context.addHandler(new NotFoundHandler());
   
   context=server.getContext(null,"/servlet/*");
   context.setClassPath("./servlets");
   context.setServingDynamicServlets(true);
   context.addHandler(new NotFoundHandler());

   // Start handlers and listener
   server.start();


A configuration file format based on XML will soon be developed.


Distribution
============
Jetty distributions and information is available from:

   http://jetty.mortbay.com
   ftp://jetty.mortbay.com/pub
   

How to Contribute
=================
Comments, contributions, feedback, bugs, testing, etc. please....

