
Jetty 3
=======  
Jetty is an Open Source HTTP Servlet Server written in 100% Java. 
It is designed to be light weight, high performance, embeddable, 
extensible and flexible, thus making it an ideal platform for serving  
dynamic HTTP requests from any Java application.

This is a BETA release of Jetty 3, which is a significant revision of
the Jetty 2 code base. The aims of the Jetty 3 developement include:

  + Fully RFC2616 compliant implementation of HTTP/1.1.
  + Decouple HTTP server from specific Servlet container.
  + Implement Servlet 2.2 Container.
  + Remain efficient, embedable & extensible.
  + Utilize java collection classes for improved performance.
  + Improved coding standards and increasing test coverage.


Environment
===========
Development has been done in JDK1.2 and the following CLASSPATH is required 
(or the non unix equivalent):

  CLASSPATH=$JETTY_HOME/lib/com.mortbay.jetty.jar:$JETTY_HOME/lib/aelfred.sax.jar:$JETTY_HOME/lib/javax.servlet.jar


To compile the code, the following CLASSPATH should be used:

  CLASSPATH=$JETTY_HOME/src:$JETTY_HOME/lib/aelfred.sax.jar


Configuring the Server
======================
The class com.mortbay.HTTP.HttpServer is the main class to be
configured.  It has a configuratin API and a command line API and
can be run with

  java com.mortbay.HTTP.HttpServer [options | -?]


The class com.mortbay.Jetty.Server reads and xml configuration
file and configures 1 or more instances of com.mortbay.HTTP.HttpServer.

  java com.mortbay.Jetty.Server [configuration.xml]

Jetty comes with the tiny aelfred SAX parser from microstar.com.  This
is only a partially validating parser, so care must be taken when 
editiing xml files.  If in doubt, use the org.sml.sax.parser property
to specify an alternative parser (eg jaxp, xerces etc.).

The DTDs used by jetty are resolved as resources from the system classpath,
thus they are stored in com.mortbay.HTTP/web.dtd and 
com.mortbay.Util.configure.dtd within the source tree and jar file.  Copies
are also placed in etc for reference.

A document in the doc directory describes the configuration for SSL in Jetty.


Running the Demo Server
=======================
A Demo server can be run on port 8080 with the command:

  java com.mortbay.Jetty.Demo

This demo is configured using the HttpServer API and is a good
example of coded configuration. The same demo is configured by the
xml file $JETTY_HOME/etc/demo.xml and can be run with the command:

  java com.mortbay.Jetty.Server etc/demo.xml



Test Harnesses
==============
The class com.mortbay.HTTP.TestRFC2616 is the start of 
a test harness linked to the RFC document.   This and the other test
harnesses can be run with

  java com.mortbay.Util.TestHarness
  java com.mortbay.HTTP.TestHarness

Note that the test harnesses are not in the jar file, so you will need
to build all the source and run with the source hierarchy in the CLASSPATH.



Distribution
============
Jetty distributions and information is available from:

   http://jetty.mortbay.com
   http://sourceforge.net/project/jetty
   ftp://jetty.mortbay.com/pub
   

How to Contribute
=================
Comments, contributions, feedback, bugs, testing, etc. please....
