
There are two ways to run Jetty as a service on a Win32 platform:

+ Compile the C code in %JETTY_HOME%\win32\service.

or

+ Obtain a copy of the free JavaService.exe from Alexandria
  Software Consulting:

  http://www.alexandriasc.com/software/JavaService/index.html

Note that JavaService.exe does NOT suffer from the bug that causes 
the service to end if the user starting the service logs off, 
whereas this can happen with provided C code.

In both cases described below, it is assumed that the following
environment variables have been set correctly:

set JAVA_HOME=C:\Java\JDK1.3.1_04
REM -- Sun Java VM choices are server, hotspot and classic --
set JAVA_VM=%JAVA_HOME\jre\bin\server
set JETTY_HOME=C:\Java\Jetty
set CLASSPATH=%JETTY_HOME%\lib\org.mortbay.jetty.jar
set CLASSPATH=%CLASSPATH%;%JETTY_HOME%\lib\javax.servlet.jar
set CLASSPATH=%CLASSPATH%;%JETTY_HOME%\lib\javax.xml.jaxp.jar
set CLASSPATH=%CLASSPATH%;%JETTY_HOME%\lib\org.apache.crimson.jar
set CLASSPATH=%CLASSPATH%;%JETTY_HOME%\lib\org.apache.jasper.jar
set CLASSPATH=%CLASSPATH%;%JETTY_HOME%\lib\com.sun.net.ssl.jar
set CLASSPATH=%CLASSPATH%;%JAVA_HOME%\lib\tools.jar


Using the C Code
================
1. If the C code in %JETTY_HOME%\win32\service has not been compiled:

     edit %JETTY_HOME%\win32\service\make # set JDK variable
     cd %JETTY_HOME%\win32\service
     nmake

2. Copy the jettysvc.exe file to the JVM DLL directory:

     copy %JETTY_HOME%\win32\service\jettysvc.exe %JAVA_VM%

3. Goto that directory:

     cd %JAVA_VM%

4. Test that you can run the exe:

     jettysvc.exe -?

5. Test that the exe can run Jetty:

     jettysvc.exe -c -Djava.class.path=%CLASSPATH% -DDEBUG %JETTY_HOME%\etc\jetty.xml wrkdir=%JETTY_HOME%
     # use browser to hit http://localhost:8080
     # ctrl-C

6. Install the service:

     jettysvc.exe -i -Djava.class.path=%CLASSPATH% -DDEBUG %JETTY_HOME%\etc\jetty.xml wrkdir=%JETTY_HOME%

7. Use the services applet from the control panel to start Jetty (or 
   reboot if that makes it feel more win32 :-).

8. Use browser to hit http://localhost:8080 and check the log files in 

     %JETTY_HOME/logs

9. The Service can be removed with:

     jettysvc.exe -r


Using JavaService.exe
=====================
1. Copy JavaService.exe to %JETTY_HOME%\bin\JettyService.exe. Note that
   the path MUST NOT have spaces in it!

2. Install the service:

	%JETTY_HOME%\bin\JettyService -install "Jetty Java HTTP Server" %JAVA_VM%\jvm.dll -Djava.class.path=%CLASSPATH% -start org.mortbay.jetty.win32.Service -params ./etc/jetty.xml -stop org.mortbay.jetty.win32.Service -method stopAndDestroy -out ./logs/jettysvc.out -err ./logs/jettysvc.err -current %JETTY_HOME%

   (Check the documentation in the JavaService download for more options.)

3. Use the services applet from the control panel to start the Jetty
   Java HTTP Server service.

4. Use the event viewer to check for any error messages.

5. The Service can be removed with:

     %JETTY_HOME%\bin\JettyService -uninstall "Jetty Java HTTP Server"
