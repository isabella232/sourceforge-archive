
Jetty J2EE Integration Package
-------------------------------

This package encompasses all the portable (i.e. written to J2EE or
commonly available APIs) aspects of the integration of Jetty with a
J2EE AppServer, or parts thereof - either locally, within the same VM,
or remotely.

The JBoss-Jetty integration code is also reproduced in this module
and is covered by the GPL license.  The full history of this integration
is available via the jboss CVS repository.


To build a jboss-jetty.sar for this version of jetty, you need to
edit the jboss.properties file to point to your version of jboss
and xdoclet.  The default ant target will make a sar in base directory
of this module, called 

  jboss-x.x.x-jetty-x.x.x.sar

This may be deployed by copying this file into the $JBOSS_HOME/server/default/deploy
directory (make sure no other webcontainer is deployed first).  

Note that it may be more conveniant to deploy the sar in unpacked in a directory
called jboss-jetty.sar, so that the configuration be edited simply.
The ant target "deploy" does this for you.


