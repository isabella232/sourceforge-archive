
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


Environment
===========
Development has been done in JDK1.2 using the collection classes.
The compiled classes excluding test harnesses are put into lib/com.mortbay.jetty.jar,
so the following CLASSPATH is required (or the non unix equivalent):

  CLASSPATH=$JETTY3_HOME/lib/com.mortbay.jetty.jar:$JETTY3_HOME/lib/jaxp.jar:$JETTY3_HOME/lib/parser.jar:$JETTY3_HOME/lib/javax.servlet.jar

Most of Jetty can be built with JDK1.1 using the 1.1 collection jar,
if the commented XXX-JDK1.1 and XXX-JDK1.2 commented imports are
switched.

Test Harnesses
==============
The class com.mortbay.HTTP.TestRFC2616 is the start of 
a test harness linked to the RFC document.   This and the other test
harnesses can be run with

  java com.mortbay.Util.TestHarness
  java com.mortbay.HTTP.TestHarness

Note that the test harnesses are not in the jar file, so you will need
to build all the source and run with the source hierarchy in the CLASSPATH.


WWW Server
==========

A Demo server can be run on port 8080 for the web application in 
$JETTY_HOME/webapps/jetty with the command:

  java com.mortbay.Jetty.Demo


A raw HTTP server can be run with the command
 
  java com.mortbay.HTTP.HttpServer

By default it runs on port 8080 and serves files from "./FileBase" and 
dynamic servlet from "./servlets" and the system class path. 
It also runs the web application in $JETTY_HOME/webapps.default.

The server can be configured by using command line options, which can 
be displayed with 

  java com.mortbay.HTTP.HttpServer -?


Servers can also be configured in code. For example:

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




Distribution
============
Jetty distributions and information is available from:

   http://jetty.mortbay.com
   http://sourceforge.net/project/jetty
   ftp://jetty.mortbay.com/pub
   

How to Contribute
=================
Comments, contributions, feedback, bugs, testing, etc. please....

