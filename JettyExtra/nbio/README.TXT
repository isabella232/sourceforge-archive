
Non-Blocking IO.
----------------
The org.mortbay.http.nbio package provides a SocketListener that
uses the non-blocking IO library from 
  http://www.cs.berkeley.edu/~mdw/proj/java-nbio/
To avoid having threads assigned to Idle connections.   It is 
HIGHLY EXPERIMENTAL, but appears to work OK at the moment. Feedback
would be highly appreciated.

To use the NBIO listener, the nbio package must be install on your 
classpath and ld_library_path as per their instructions. 

The env variable NBIO_HOME should be set to the NBIO directory.

Build the nbio classes into the jetty jars with:
  ant 

The etc/nbio.xml file shows how Jetty can be setup to use this listener.

Testing and benchmarking should be done systems with large numbers of 
connections (idle or otherwise).  It is intended that the number of
connections may greatly exceed the maximum threads allocated to the 
listener.

