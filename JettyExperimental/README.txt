

This is the experimental release of Jetty.  Currently it is 
focus on providing a small, lightweight and complete HTTP/1.1
protocol stack.   

This is mostly a clean slate implementation with only a little code 
taken from Jetty 5, so that 8 years of cruft can be removed.

It is also trying to be IO nuetral so that NIO buffering and non-blocking
approaches can be used. 

It currently implements most of HTTP/1.1 in a jar of approx 110k, with
no other jars required.

Very little doco at the moment, but the things you need to know are:

 + it is built with maven

     maven jar:jar

 + eclipse project files are checked in.

 + You can run it with

    java -classpath target/jetty-EXP0.jar org.mortbay.http.nio.SocketChannelListener
   or
    java -classpath target/jetty-EXP0.jar org.mortbay.http.bio.SocketListener


 + The HttpServer is nailed to port 8080 and just does a simple
   dump or serves a static files from the current directory.

 + Most of RFC2616 is covered and the server handles HTTP 0.9, 1.0
   and 1.1 requests in persistent and non-persistent forms.

 + Test harnesses are currently covering about 60% of the code.


TODO:

 + Lots more javadoc
 + A Connector architecture (same as Jetty)
 + A Handler architecture (probably nested as with tomcat)
 + A resource handler
 + Contentlets

