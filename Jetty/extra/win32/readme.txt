

This directory uses the java wrapper provided by http://wrapper.tanukisoftware.org
to provide jetty as an Win32 service.

The file wrapper.conf needs to be edited and the location of your JVM added:

    wrapper.java.command=c:/j2sdk1.4.1/bin/java


No other changes are required if the admin.xml & jetty.xml configuration
files are used to run Jetty.


To run Jetty on the console, use 

   Wrapper.exe -c wrapper.conf


To install Jetty as a win32 service:

   Wrapper.exe -i wrapper.conf


To remove the Jetty service

   Wrapper.exe -r wrapper.conf





