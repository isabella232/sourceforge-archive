
Jetty 3
=======  
Jetty is an Open Source HTTP Servlet Server written in 100% Java. 
It is designed to be light weight, high performance, embeddable, 
extensible and flexible, thus making it an ideal platform for serving  
dynamic HTTP requests from any Java application.

This release of Jetty 3 is a significant revision of the Jetty 2 code 
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
  $JETTY_HOME/bin/jetty.sh run demo

Browse to:
  http://localhost:8080

If this doesn't work - you will need to read some more and setup the
environment.


Quick start on Windows
======================
Due to problems with the servlet security model (see below). It is vital
that windows users use the exact case for drive letters, URLs and filenames. 

run:
  %JETTY_HOME%\bin\jetty.sh run demo.xml

Browse to:
  http://localhost:8080

If this doesn't work - you will need to read some more and setup the
environment.


Environment
===========
Development has been done in JDK1.2 and the following CLASSPATH is required 
(or the non unix equivalent):

  CLASSPATH=\
    $JETTY_HOME/lib/com.mortbay.jetty.jar:\
    $JETTY_HOME/lib/javax.servlet.jar:\
    $JETTY_HOME/lib/javax.xml.jaxp.jar:\
    $JETTY_HOME/lib/org.apache.jasper.jar:\
    $JETTY_HOME/lib/org.apache.crimson.jar

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

  bin/jetty.sh run demo

This script finds the JVM and sets the CLASSPATH, so it can be used as 
an /etc/init.d script and also looks for xml config files in 
/etc/jetty (among other places).


Configuring the Server
======================
The class com.mortbay.Jetty.Server reads an xml configuration
file and configures one or more instances of com.mortbay.HTTP.HttpServer:

  java com.mortbay.Jetty.Server [configuration.xml]

The class com.mortbay.HTTP.HttpServer is the main class to be
configured.  It has a configuration API and a command line API and
can be run with

  java com.mortbay.HTTP.HttpServer [options | -?]

The com.mortbay.Jetty.Server class is simply an xml configuration
frontend to com.mortbay.HTTP.HttpServer.

Jetty comes with the JAXP 1.1 reference XML parser, which is cromson from
apache. Use the org.xml.sax.parser property to specify an alternative SAX parser.

The DTDs used by jetty are resolved as resources from the system classpath,
thus they are stored in com.mortbay.HTTP/web.dtd and com.mortbay.Util.configure.dtd 
within the source tree and jar file.  Copies are also placed in etc for reference.

There is also a Jetty extension configuration mechanism for web applications.
If a file WEB-INF/web-jetty.xml is found, it is treated as a Jetty XmlConfiguration
file and applied to the WebApplicationContext object when the context
is started.

Building from source
====================
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

Jetty3Extra
-----------

Extra jetty functionality is available via the Jetty3Extra
package.  This include JMX and SASL support.


Classpaths Handling.
--------------------
If a context has it's own classpath set, then a ContextLoader
is created with tha classpath set.  A ContextLoader is a specialized
URLClassLoader so any Resource/URL/File combination may be used to
specify a path.   This classloader has the ServletHandlers classloader
set as its parent, so normal Java2 loader hierarchy semantics are 
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
  <resource-ref>
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
 JETTY_CONF - Location of the script's configuration file, set to 
              /etc/jetty.conf, or $JETTY_HOME/etc/jetty.conf by
              default.
 JETTY_RUN  - Directory for jetty pid file
 JETTY_LOG  - Directory for jetty output log

If any of these variables are not set, then frantic searches are made
of likely locations to try and find them.

The /etc/jetty.conf file may contain:
 - Coment lines starting with #
 - References to files, which are considered .xml server configurations, and
   passed to jetty for serving.
 - References to directories (i.e. /etc/jetty.d). Each directory is scanned
   for .xml files, and those found are passed to jetty for serving.

Note: If .xml configuration files are given in the command line, jetty will
not use /etc/jetty.conf. This is useful for running single servers while
testing.

Note: if you don't setup the /etc/jetty.conf file, then running
"jetty.sh start" without a config specified may start multiple 
servers (demo, jetty, watchdog) on the same port - causing errors.

Other jetty.sh actions are:
   start -- To start the server.
   stop  -- Stops a server started with "start". 
   run   -- Runs Jetty in foreground mode.
   check -- Does nothing. Useful checking the configuration.


    
Unix SystemV Init
-----------------
Jetty includes a SystemV Init script that has been tested under 
RedHat Linux 6.2. This script allows Jetty to be automatically started 
and stoped when the operating system is booted or shutdown.

To use the script, follow these steps:

  - Login as root.
  
  - Make the script executable:

       chmod u+x $JETTY_HOME/bin/jetty.sysvinit
        
  - Place a  symbolic link to the script in the /etc/rc.d/init.d directory:

       ln -s $JETTY_HOME/bin/jetty.sysvinit /etc/rc.d/init.d/jetty

    you can also copy the script there. Just remember to make it executable.

  - Edit the script and make sure that the JETTY_HOME and JAVA_HOME 
    variables are set properly.

  - Enable the script:

      chkconfig --add jetty
      chkconfig --level 345 jetty on

    you can also use ntsysv if available for this purpose.

To start the jetty server for the first time without rebooting the machine,
execute:

   /etc/rc.d/init.d/jetty start

Check /var/log/messages to see if everything worked fine.

The Jetty SystemV Init script uses the $JETTY_HOME/bin/jetty.sh shell script
to launch Jetty. Read the shell script's documentation above, or the one in 
the script's initial comments to learn about available configuration options.
        

Win32 Service
-------------
The win32 directory contains instructions of how to start Jetty as
a win32 service.


Web Application Security
------------------------
Jetty has made a few extensions to the security mechanism "described" in the servlet
specifiction.  The default security model is that all access is allowed unless a 
particular security constraint prevents access. This is vulnerable to alias attacks
where alternate case or names can be used to get to a resource.

Jetty makes the following interpretations:

 + Methods PUT, DELETE and GET are disabled unless both a web.xml and a default
   config file are provided.

 + If multiple security constraints are defined, the most specific applies to a
   request.

 + A security constraint with no data or auth constraint prevents all access.

 + A security constraint with an auth constraint of NONE grants access.

 + On platforms without the / file separator or when the system parameter 
   com.mortbay.Util.FileResource.checkAliases is true, then the FileResouce class 
   compares the absolutePath and canonicalPath and treats the resource as not found
   if they do not match.

 + Dynamic servlets by default, can only be loaded from the context classpath.
   Use ServletHandler.setServeDynamicSystemServlets to control this behaivour.

It is strongly recommended that secure WebApplications take following 
approach.  All access should be denied by default with 

  <security-constraint>
    <web-resource-collection>
      <url-pattern>/</url-pattern>
    </web-resource-collection>
  </security-constraint>

Specific access should be granted with constraints like:

  <security-constraint>
    <web-resource-collection>
      <url-pattern>/public/*</url-pattern>
      <url-pattern>/images/*</url-pattern>
      <http-method>GET</http-method>
      <http-method>HEAD</http-method>
    </web-resource-collection>
    <web-resource-collection>
      <url-pattern>/servlet/*</url-pattern>
      <http-method>GET</http-method>
      <http-method>HEAD</http-method>
      <http-method>POST</http-method>
    </web-resource-collection>
    <auth-constraint>
      <role-name>NONE</role-name>
    </auth-constraint>
  </security-constraint>


XML Parsing
-----------
Jetty uses the reference implementation of JAXP1.1 for XML parsing.
The jars have been renamed with their main package name.
Jetty does not use any of the DOM classes, so these jars can be
trimmed significantly if you want to reduce size.


JMX Management
--------------
Jetty is instrumented with an optional JMX managment layer.
This is contained in the Jetty3Extra release
  

SASL Authentication
-------------------
A SASL listener is included in the Jetty3Extra release


PDF and Adobe Acrobat support
-----------------------------
Adobe Acrobat pluggin for IE has a problem handling chunked content and the
partial implementation of ranges supported by Jetty.  If you are serving
PDF files from Jetty, then you will need to configure the HttpServer to
not force chunking and the ResourceHandler to not accept ranges:

<Configure  class="com.mortbay.HTTP.HttpServer">
  ...
  <Set name="ChunkingForced" type="boolean">false</Set>
  ...
   
  <Call name="addWebApplication">
      ...
      <Call name="getResourceHandler">
         <Set name="AcceptRanges" type="boolean">false</Set>
      </Call>
  </Call>
  ...
</Configure>


Minimal HTTP Jar
-----------------
The ant target mini.http.jar builds an all-in-one minimal(ish) jar that
contains Util and HTTP packages.   This gives a full HTTP/1.1 server with
the HttpHandler extension mechanism (but no servlets) in about 220K.

OK so there has been a bit of code bloat from the old Jetty2 100K days,
but then there has been specification bloat as well.  This jar could be
trimmed significantly if all HTTP/1.1 features are not used (eg Ranges or
security).

The minimal server can be run from the command line with 

 java -cp com.mortbay.http.jar com.mortbay.HTTP.HttpServer


Non-Blocking IO.
----------------
The com.mortbay.HTTP.NBIO package provides a SocketListener that
uses the non-blocking IO library from 
  http://www.cs.berkeley.edu/~mdw/proj/java-nbio/
To avoid having threads assigned to Idle connections.   It is 
HIGHLY EXPERIMENTAL, but appears to work OK at the moment. Feedback
would be highly appreciated.

To use the NBIO listener, the nbio package must be install on your 
classpath and ld_library_path as per their instructions. 
Build the nbio classes into the jetty jars with:
  ant nbio jars

The etc/nbio.xml file shows how Jetty can be setup to use this listener.

Testing and benchmarking should be done systems with large numbers of 
connections (idle or otherwise).  It is intended that the number of
connections may greatly exceed the maximum threads allocated to the 
listener.

