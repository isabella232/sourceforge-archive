
A simple load balancer using NIO.  Not yet seriously tested.

To run listening on port 8000 and distributing to ports 8001 and 8002 you do:

    CLASSPATH=$JETTY_HOME/lib/org.mortbay.jetty.jar:$JETTY_HOME/extra/loadbalancer/classes:$JETTY_HOME/ext/commons-logging.jar 
    java org.mortbay.loadbalancer.Balancer :8000 - :8081 :8082

In general the usage is:

    java org.mortbay.loadbalancer.Balancer [[host]:port ... ] - [[server]:port ... ]


