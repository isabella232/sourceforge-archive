The JettyServer.xml file contains an example config file for running the
PackageIndex servlet using Jetty. You will need to change the first 2
contexts to serve files from the right spot on your fileserver.

The PackageIndex.prp file is referenced from the JettyServer.xml file and
requires site-specific modifications to work correctly. Read the comments
contained within.