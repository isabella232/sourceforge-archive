

This is the experimental release of Jetty.  Currently it is 
focus on providing a small, lightweight and complete HTTP/1.1
protocol stack.   

This is mostly a clean slate implementation with only a little code 
taken from Jetty 5, so that 8 years of cruft can be removed.

It is also trying to be IO nuetral so that NIO buffering and non-blocking
approaches can be used. 


Very little doco at the moment, but the things you need to know are:

 + it is built with maven

     maven jar:jar

 + You can run it with

    java -classpath target/jetty-EXP.jar org.mortbay.http.HttpServer


 + The HttpServer is nailed to port 8080 and just does a simple
   dump.

 + Most of RFC2616 is covered and the server handles HTTP 0.9, 1.0
   and 1.1 requests in persistent and non-persistent forms.

 + Test harnesses are currently covering about 60% of the code.


