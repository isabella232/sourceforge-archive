
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

  CLASSPATH=\
    $JETTY_HOME/lib/com.mortbay.jetty.jar:\
    $JETTY_HOME/lib/javax.servlet.jar:\
    $JETTY_HOME/lib/com.microstar.xml.jar

To use SSL you will also need to add the the jar below, which is
a merge of the jsse, jnet and jcert jars from the JSSE package
(see $JETTY_HOME/doc/JsseSSL.html for more details):

    $JETTY_HOME/com.sun.net.ssl.jar

To run JSP with the JDK compiler, you will also need to add the
jar containing the JDK compiler (if you wish to use another compiler
you will have to configure jasper):

   $JDK_HOME/lib/tools.jar


Running the Demo Server
=======================
A Demo server can be run on port 8080 with the command:

  java com.mortbay.Jetty.Demo

This demo is configured using the HttpServer API and is a good
example of coded configuration. The same demo is configured by the
xml file $JETTY_HOME/etc/demo.xml and can be run with the command:

  java com.mortbay.Jetty.Server etc/demo.xml


Configuring the Server
======================

The class com.mortbay.Jetty.Server reads an xml configuration
file and configures 1 or more instances of com.mortbay.HTTP.HttpServer:

  java com.mortbay.Jetty.Server [configuration.xml]


The class com.mortbay.HTTP.HttpServer is the main class to be
configured.  It has a configuratin API and a command line API and
can be run with

  java com.mortbay.HTTP.HttpServer [options | -?]

The com.mortbay.Jetty.Server class is simply an xml configuration
frontend to com.mortbay.HTTP.HttpServer.


Jetty comes with the tiny aelfred SAX parser from microstar.com.  This
is only a partially validating parser, so care must be taken when 
editing xml files.  If in doubt, use the org.sml.sax.parser property
to specify an alternative parser (eg jaxp, xerces etc.).

The DTDs used by jetty are resolved as resources from the system classpath,
thus they are stored in com.mortbay.HTTP/web.dtd and 
com.mortbay.Util.configure.dtd within the source tree and jar file.  Copies
are also placed in etc for reference.


Building from source
====================
To compile the code, the following addition to the CLASSPATH should 
be used:

   $JETTY_HOME/src:

The code can be build with gnu make just by typing 'make'.  Other
make options can be viewed with the command 'make help'.  An Ant
build file is included in etc/build.xml.  Ant is available from

    http://jakarta.apache.org/ant/index.html  

And it can be run with:

    ant -buildfile etc\build.xml 


Test Harnesses
==============
The class com.mortbay.HTTP.TestRFC2616 is a test harness linked 
to the RFC document.  This and the other test harnesses can be run with

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


NOTES
=====

SSL:
----
A document in the doc directory describes the configuration for SSL in Jetty.


CGI:
----
A CGI servlet is included in src/com/mortbay/Servlet/CGI.java  ,  but
it is not yet configured into the release demo, nor is it documented.

