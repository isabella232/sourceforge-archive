
Jetty J2EE Integration Package
-------------------------------

This package encompasses all the portable (i.e. written to J2EE or
commonly available APIs) aspects of the integration of Jetty with a
J2EE AppServer, or parts thereof - either locally, within the same VM,
or remotely.

Any code that Jetty integrations with other J2EE components might
share, should find it's home here.

Initially this means the portable parts of the Jetty/JBoss integration
- particularly those of my distributed HttpSession implementation.


The Jetty 5 sar can only be deployed in Jboss 3.2 if 

 + The xerces and xml-api jars are upgraded to a recent version
 + The jasper jars are replaced with jasper from jetty 4


Enjoy,


Jules

