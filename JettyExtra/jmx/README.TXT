
Jetty 3 JMX
===========  

This extra packages instruments Jetty with JMX model MBeans.  

JMX management is highly experimental and likely to change.
Feedback is welcome and needed!


Quick Start
===========
+ Install the ant build tool from http://jakarta.apache.org
+ Download, install and test a recent release of Jetty3.1.x
+ Set the JETTY_HOME environment variable
+ Optional:  Build the jmx package with "ant all"
+ Optional:  Test the jmx package with "ant test"
+ Run the jetty demo via JMX with "ant demo"
+ View the jetty demo at http://localhost:8080
+ View the MBeans at http://localhost:8082


Detail
======
Look at the javadoc and code for com.mortbay.Jetty.JMX

The JMX MBeans are started via mlets which are defined in the
etc/jetty.mlet file.   

The mlet file can be run with:

  java com.mortbay.Jetty.JMX.Main etc/jetty.mlet




  
