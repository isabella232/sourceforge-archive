
Jetty 3
=======  
Jetty is an Open Source HTTP Servlet Server written in 100% Java. 
It is designed to be light weight, high performance, embeddable, 
extensible and flexible, thus making it an ideal platform for serving  
dynamic HTTP requests from any Java application.

This  release of Jetty 3 is a significant revision of the Jetty 2 code 
base. The aims of the Jetty 3 developement include:

  + Fully RFC2616 compliant implementation of HTTP/1.1.
  + Decouple HTTP server from specific Servlet container.
  + Implement Servlet 2.2 Container.
  + Remain efficient, embedable & extensible.
  + Utilize java collection classes for improved performance.
  + Improved coding standards and increasing test coverage.


Quick start on Unix
===================

run:
  <install-directory>/bin/jetty.sh start admin demo

Browse to:
  http://localhost:8080

If this doesn't work - you will need to read some more and setup the environment.



Environment
===========
Development has been done in JDK1.2 and the following CLASSPATH is required 
(or the non unix equivalent):

  CLASSPATH=\
    $JETTY_HOME/lib/com.mortbay.jetty.jar:\
    $JETTY_HOME/lib/javax.servlet.jar:\
    $JETTY_HOME/lib/org.apache.jasper.jar:\
    $JETTY_HOME/lib/com.microstar.xml.jar

To use SSL you will also need to add the the jar below, which is
a merge of the jsse, jnet and jcert jars from the JSSE package
(see $JETTY_HOME/doc/JsseSSL.html for more details):

    $JETTY_HOME/lib/com.sun.net.ssl.jar

To run JSP with the JDK compiler, you will also need to add the
jar containing the JDK compiler (if you wish to use another compiler
you will have to configure jasper):

   $JDK_HOME/lib/tools.jar


Running the Demo Server
=======================
A Demo server can be run on port 8080 with the commands:

  cd $JETTY_HOME
  java com.mortbay.Jetty.Demo

You can access this demo with at http://localhost:8080
The demo also runs a simple admin servlet at http://localhost:8888,
with user username=admin, password=admin.

This demo is configured using the HttpServer API and is a good
example of coded configuration. The same demo is configured by the
xml files etc/admin.xml etc/demo.xml and can be run with the command:

  java com.mortbay.Jetty.Server etc/admin.xml etc/demo.xml


There is also a Unix start/stop script bin/jetty.sh which can run
the demo with:

  bin/jetty.sh start admin demo

This script find the JVM and sets the CLASSPATH, so it can be used as 
an /etc/init.d script and will look for xml config files in 
/etc/jetty (among other places).


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
editing xml files.  If in doubt, use the org.xml.sax.parser property
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
make options can be viewed with the command 'make help'.  

An Ant build file is included in build.xml.  This file is configured to
use the jikes compiler from IBM. To use the normal JDK compiler comment
out the jikes line in build.xml.  Ant is available from

    http://jakarta.apache.org/ant/index.html  

You can run ant for the following targets:

    ant         - Build source
    ant all     - Build everything
    ant test    - Build and run tests
    ant demo    - Build and run the demo
    ant clean   - Remove generated files


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
   

Documentation
=============
Jetty is currently under documented, but this is improving.  The
documentation is currently being hosted and developed as a Wiki site.
For details see:

  http://jetty.mortbay.com:8080/wiki/JettyWiki


How to Contribute
=================
Comments, contributions, feedback, bugs, testing, etc. please....

The mailing lists are the best place to start:

  http://www.egroups.com/group/jetty-announce/
  http://www.egroups.com/group/jetty-discuss/
  http://www.egroups.com/group/jetty-support/
  http://sourceforge.net/mail/?group_id=7322



NOTES
=====

Classpaths Handling.
--------------------
If a context has it's own classpath set, then a ContextLoader
is created with tha classpath set.  A ContextLoader is a specialize
URLClassLoader so any Resource/URL/File combination may be used to
specify a path.   This classloader has the ServletHandlers classloader
set as it parent, so normal Java2 loader hierarchy semantics are 
followed.

Note that if the context is a packed WAR file, then the elements
of the classpath are likely to be jar:file URLs.


JSP Classpaths
--------------
The context classloader is passed to the Jasper JspServlet and
is used as the parent loader of the JspLoader.
A file classpath is also passed for used by the JSP compiler. Note
that if the context is a packed WAR file, then there is no 
reasonable file based classpath that can be passed to the compiler.

If this proves to be a problem for you, then unpack your WAR files 
(which is what most other containers do by default anyway).



Unimplemented WebApplication features
-------------------------------------
The following WebApplication tags are not implemented:
 
  <distributable>
  <error-page>
  <tag-lib>
  <env-entry>
  <ejb-ref>


Run script
----------

The unix start/stop script bin/jetty.sh which can run with:

   bin/jetty.sh start [ jetty.xml ... ]

This script is controlled by the following environment variables:
 
 JAVA       - executable JVM
 JAVA_HOME  - Home of java, specifically $JAVA_HOME/lib/tools.jar
 JETTY_HOME - Home of jetty
 JETTY_ETC  - Jetty configuration file directory
 JETTY_RUN  - Directory for jetty pid file
 JETTY_LOG  - Directory for jetty output log

If any of these variables are not set, then frantic searches are made
of likely locations to try and find them.

To use this to start jetty at boot time:
 + put your jetty.xml files in a /etc/jetty directory  
   Make sure they use absolute paths or relative to $JETTY_HOME
 + copy jetty.sh to the /etc/init.d directory
 + make a symlink from the rc directory of your choice:  
    cd /etc/rc2.d
    ln -s ../init.d/jetty.sh S20jetty
 + Make similar kill links:
    cd /etc/rc6.d
    ln -s ../init.d/jetty.sh K20jetty
    cd /etc/rc1.d
    ln -s ../init.d/jetty.sh K20jetty

Note if you don't setup the etc/jetty directory then running
"jetty.sh start" without a config specified, will start multiple 
servers (demo, jetty, watchdog) on the same port - causing errors.
    

