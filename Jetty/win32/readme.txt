

These instructions describe how to run Jetty from a win32 platform from
the console and as a service. The following sets up the Jetty environment,
assuming that Jetty has been unpacked at C:\java\Jetty-3.0.0:

  set JETTY_HOME=C:\java\Jetty-3.0.0
  %JETTY_HOME%\win32\jettyenv.bat

Jetty can now be run on the console with

  cd %JETTY_HOME%
  java com.mortbay.Jetty.Server etc\jetty.xml


To run Jetty as a win32 service:

1. If the C code in %JETTY_HOME%\win32\service has not been compiled:

     edit %JETTY_HOME%\win32\service\make # set JDK variable
     cd %JETTY_HOME%\win32\service
     nmake

2. Locate where your JVM is and look for the jvm.dll file.

     set JVMDLLDIR=%JAVA_HOME%\bin

3. Copy the jettysrv.exe file to that directory:

     copy %JETTY_HOME%\win32\service\jettysvc.exe %JVMDLLDIR%

4. Goto that directory:

     cd %JVMDLLDIR%

5. Test that you can run the exe:

     jettysvc.exe -?

6. Test that the exe can run Jetty:

     jettysvc.exe -c -Djava.class.path=%CLASSPATH% -DDEBUG %JETTY_HOME%\etc\jetty.xml wrkdir=%JETTY_HOME%
     # use browser to hit http://localhost:8080
     # ctrl-C

7. Install the service:

     jettysvc.exe -i -Djava.class.path=%CLASSPATH% -DDEBUG %JETTY_HOME%\etc\jetty.xml wrkdir=%JETTY_HOME%

8. Use the services panel from the control panel to start Jetty (or 
   reboot if that makes it feel more win32 :-).

9. Use browser to hit http://localhost:8080 and check the log files in 

     %JETTY_HOME/logs

A. The Service can be removed with:

     jettysvc.exe -r

