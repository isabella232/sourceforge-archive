JettyPlus README
----------------
JettyPlus contains a number of useful facilities integrated with Jetty, namely J2EE user transactions and
DataSources, JNDI, JAAS and Log4J.



Building
--------

To build JettyPlus, execute the following at a prompt in the $jetty.home/extra/plus directory:

     ant jars

This will produce the following jars in $jetty.home/extra/lib:

    + org.mortbay.jetty.plus.jar
        This is the main JettyPlus jar file

    + org.mortbay.jetty.plus.resource.jar
        This contains property and xml config files:
           + jndi.properties
	       The JNDI service configuration file
           + carol.properties
	       The JNDI service configuration file
	   + log4j.xml
	       The log4j configuration file

    + org.mortbay.jaas.jar
        The JAAS integration 



Demos
-----

Demonstration webapps are available for both JAAS and JettyPlus:

+  ant run.jaas.demo

   This target will build and run a webapp which demonstrates using a JDBC JAAS login module to
   authenticate and authorize a user based on web.xml declarative role-based security constraints, and
   furthermore to authorize the user based on Java security permissions. 

   The URL is http://localhost:8080/jaas/index.html. Output is logged to $jetty.home/extra/logs/jetty.log.

   Related files:

     + $jetty.home/extra/etc/jettyplus.xml
       The Jetty server configuration file

     + $jetty.home/extra/etc/login.conf
       The JAAS login config file

     + $jetty.home/extra/etc/jaas.policy
       J2SDK policy permission file
      + $jetty.home/extra/etc/jettyplus.xml
        The Jetty server config file
      
      NOTE: if you modify any files in $jetty.home/extra/plus/resources, then ensure that you rerun the "ant jars"
            target to re-create the org.mortbay.jetty.plus.jar file.


+ ant run.jettyplus.demo

  This target builds and runs a webapp which demonstrates the use of JNDI for
  environment lookups, database connectivity using DataSources and
  UserTransactions and a Mail service.

  The URL is http://localhost:8080/jettyplus/index.html. Output is logged to $jetty.home/logs.

  Related files:

      + $jetty.home/extra/etc/tmtest.properties
         Database property file for hsqldb

      + $jetty.home/extra/etc/tmtest.script
         Database configuration script

      + $jetty.home/extra/plus/resources/carol.properties
         JNDI config file

      + $jetty.home/extra/plus/resources/log4j.xml
         Log4j config file

