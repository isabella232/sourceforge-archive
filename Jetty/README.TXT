
Jetty 3
=======
This is an alpha release of the Jetty 3 project.  It is currently not
usable as a servlet server, but is released now to keep the development
open.

Jetty 3 was started from a clean slate.  Classes are moved from Jetty 2
on an as needed basis.  

Currently the release contains the Utils package (with about half a 
cleanup done) and the HTTP package. Work to date in the HTTP package 
has concentrated on RFC2616 compliance, performance and testing.  
See the VERSION.TXT file for more detail.

Environment
===========
Development has been done in JDK1.1 with the 1.2 Collection classes.
The compiled classes are currently kept in the src directory, so
the following CLASSPATH is required (or the non unix equivalent):

  CLASSPATH=$JETTY3_HOME/src:$JETTY3_HOME/lib/collections.jar


Test Harnesses
==============
The class com.mortbay.HTTP.TestRFC2616 is the start of 
a test harness linked to the RFC document.   This and the other test
harnesses can be run with

  java com.mortbay.Util.TestHarness
  java com.mortbay.HTTP.TestHarness


WWW Server
==========
A HTTP server can be run with
 
  java com.mortbay.HTTP.HttpServer

By default it runs on port 8080 and serves files and simple request dumps. 
If run from the install directory, a set of simple demo pages are served.
The server can be run at one or more specific addresses by using the options:

  -a [<addr>:]port    - listen on a port
  -d <path>           - DumpHandler at path
  -f <path>=<dir>     - FileHandler at path

The default is equivalent to
  java com.mortbay.HTTP.HttpServer -a 8080 -f /=. -d /


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

