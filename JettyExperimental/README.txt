

This is the experimental release of Jetty. It will eventually become Jetty 6.x.x.

This is mostly a clean slate implementation with only a little code 
taken from Jetty 5, so that 8 years of cruft can be removed.

Thus it has been able to be rearchitected to more closely match/use the 
current servlet API and to closer model concepts such as filters and contexts.

To learn more, run the server:

   java -jar start.jar etc/jetty.xml

and then point your browser at 

   http://localhost:8080

and click to the test webapp.


