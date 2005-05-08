

This is the experimental release of Jetty. 

This is mostly a clean slate implementation with only a little code 
taken from Jetty 5, so that 8 years of cruft can be removed.

Thus it has been able to be rearchitected to more closely match/use the 
current servlet API and to closer model concepts such as filters and contexts.

It is also trying to be IO nuetral so that NIO buffering and non-blocking
approaches can be used. 

It currently implements HTTP/1.1 and the core servlet API in a jar of approx 210k, with
only the serlvet and slf4j jars required.


