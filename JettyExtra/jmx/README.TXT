Jetty JMX
===========  

This extra packages instruments Jetty with JMX model MBeans.  

JMX management is highly experimental and likely to change.
Feedback is welcome and needed!


Quick Start
===========
+ Install the ant build tool from http://jakarta.apache.org
+ Download, install and test a recent release of Jetty
+ Set the JETTY_HOME environment variable
+ Optional:  Build the jmx package with "ant all"
+ Optional:  Test the jmx package with "ant test"
+ Run the jetty demo via JMX with "ant demo"
+ View the jetty demo at http://localhost:8080
+ View the MBeans at http://localhost:8082


Detail
======
Look at the javadoc and code for org.mortbay.jetty.jmx

The JMX MBeans are started via mlets which are defined in the
etc/jetty.mlet file.   

The mlet file can be run with:

  java org.mortbay.jetty.jmx.Main etc/jetty.mlet




  
