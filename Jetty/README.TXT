
Jetty 3
=======

This is an alpha release of the Jetty 3 project.  It is currently not
usable as a WWW server, but is released now to keep the development open.

Jetty 3 has started from a clean slate.  Classes are moved from Jetty 2
on an as needed basis.  Development has been done in JDK1.1 with
the 1.2 Collection classes.

Currently the release contains the Utils package (with about half a 
cleanup done) and the HTTP package. Work to date in the HTTP package 
has concentrated on RFC2616 compliance, performance and testing.  
See the VERSION.TXT file for more detail.

Test Harnesses
==============
The class com.mortbay.HTTP.TestRFC2616 is the start of 
a test harness linked to the RFC document.   This and the other test
harnesses can be run with

  java com.mortbay.Util.TestHarness
  java com.mortbay.HTTP.TestHarness


WWW Server
==========
A trivial WWW server can be run with
 
  java com.mortbay.HTTP.HttpServer

It always runs at port 8080 and just does a simple dump of the request.


Distribution
============
Currently the jetty.mortbay.com host has a dead hard disk, so no public
CVS repository is available yet (hopefully next week).

For now, the package and collections jar will be made available on the
document vault of the jetty3-discuss egroups page:

   http://www.egroups.com/docvault/jetty3-discuss/


Contributions
=============
Comments, contributions, feedback, bugs, testing, etc. please....

