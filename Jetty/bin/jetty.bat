@echo off
::
:: Startup script for jetty under *nix systems (it works under NT\cygwin too).
::
:: Configuration files
::
:: %HOME%\.jettyrc
::   If found, it is called at the start of batch file. It may perform
::   any sequence of commands, like setting relevant environment variables.
::
:: %JETTY_HOME%\etc\jetty.conf (NOTE: !!! THIS IS NOT WORKING AT THE MOMENT !!!)
::   If found, and no configurations were given on the command line,
::   the file will be used as this script's configuration.
::   Each line in the file may contain:
::     - A comment denoted by the pound (#) sign as first non-blank character.
::     - The path to a regular file, which will be passed to jetty as a
::       config.xml file.
::     - The path to a directory. Each *.xml file in the directory will be
::       passed to jetty as a config.xml file.
::
::   The files will be checked for existence before being passed to jetty.
::
:: Configuration variables
::
:: JAVA_HOME
::   Home of Java installation. This needs to be set as this script
::   will not look for it.
::
:: JAVA
::   Command to invoke Java. If not set, %JAVA_HOME%\bin\java will be
::   used.
::
:: JETTY_HOME
::   Where Jetty is installed. If not set, the script will try go
::   guess it by first looking at the invocation path for the script,
::   and then by looking in standard locations as %HOME%\opt\jetty
::   and \opt\jetty. The java system property "jetty.home" will be
::   set to this value for use by configure.xml files, f.e:
::
::    <Arg><SystemProperty name="jetty.home" default="."\>\webapps\jetty.war<\Arg>
::
:: JETTY_LOG
::   Where jetty logs should be stored. The only effect of this
::   variable is to set the "jetty.log" java system property so
::   configure.xml files can use it, f.e.:
::
::     <Arg><SystemProperty name="jetty.log" default=".\logs"\>\yyyy_mm_dd.request.log<\Arg>
::
::   This variable will be tipically set to something like \var\log\jetty. If
::   not set, it will default to %JETTY_HOME%\logs
::
:: JETTY_RUN
::   Where temporary files should be stored. It defaults to %TMP%.
::
:: $Id$
::

rem setlocal
:::::::::::::::::::::::::::::::::::::::::::::::::::
:: Get the action & configs
:::::::::::::::::::::::::::::::::::::::::::::::::::
set ACTION=%1
set ARGS=%2 %3 %4 %5 %6 %7 %8 %9
set CONFIGS=

:::::::::::::::::::::::::::::::::::::::::::::::::::
:: Check for JAVA_HOME
:::::::::::::::::::::::::::::::::::::::::::::::::::
if not x==x%JAVA_HOME% goto have_java_home
  echo "** ERROR: JAVA_HOME variable not set. Sorry, can't find java command."
  goto ERROR
:have_java_home

:::::::::::::::::::::::::::::::::::::::::::::::::::
:: See if there's a user-specific configuration file
:::::::::::::::::::::::::::::::::::::::::::::::::::
if not exist %HOME%\.jettyrc  goto no_jettyrc
  call "%HOME%\.jettyrc"
:no_jettyrc


:::::::::::::::::::::::::::::::::::::::::::::::::::
:: Jetty's hallmark
:::::::::::::::::::::::::::::::::::::::::::::::::::
set JETTY_JAR=lib\com.mortbay.jetty.jar


goto check_home_jetty
:::::::::::::::::::::::::::::::::::::::::::::::::::
:: if no JETTY_HOME, search likely locations.
:::::::::::::::::::::::::::::::::::::::::::::::::::
if not x==x%JETTY_HOME% goto check_home_jetty
   ::@TODO Find a way to search for the jetty directory
   if not exist .\%JETTY_JAR% goto check_parent_dir
   set JETTY_HOME=.
   :check_parent_dir
   if not exist ..\%JETTY_JAR% goto check_home_jetty
   set JETTY_HOME=..
   :: add other likely locations here

:check_home_jetty
:::::::::::::::::::::::::::::::::::::::::::::::::::
:: No JETTY_HOME yet? We're out of luck!
:::::::::::::::::::::::::::::::::::::::::::::::::::
if not x==x%JETTY_HOME% goto check_jar_jetty
    echo "** ERROR: JETTY_HOME not set, you need to set it or install in a standard location"
    goto ERROR

:check_jar_jetty
if exist %JETTY_HOME%\%JETTY_JAR% goto have_home_jetty
   echo "** ERROR: Oops! %JETTY_HOME%\%JETTY_JAR% is not readable!"
   goto ERROR

:have_home_jetty
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Get the list of config.xml files from the command line.
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
if x==x%ARGS% goto no_args
    set CONFIGS=%ARGS%
:no_args

::@TODO Find a way to parse jetty.conf
goto no_conf_jetty
:::::::::::::::::::::::::::::::::::::::::::::::::::
:: Try to find this script's configuration file,
:: but only if no configurations were given on the
:: command line.
:::::::::::::::::::::::::::::::::::::::::::::::::::
if x==x%JETTY_CONF% set JETTY_CONF=%JETTY_HOME%\etc\jetty.conf

:::::::::::::::::::::::::::::::::::::::::::::::::::
:: Read the configuration file if one exists
:::::::::::::::::::::::::::::::::::::::::::::::::::
set CONFIG_LINES=
if not x==x%CONFIGS% goto have_configs
  if not exist "%JETTY_CONF%" goto no_conf_jetty
     for /f %%L in (%JETTY_CONF%) do set CONFIG_LINES=%CONFIG_LINES% %%L


:::::::::::::::::::::::::::::::::::::::::::::::::::
:: Get the list of config.xml files from jetty.conf
:::::::::::::::::::::::::::::::::::::::::::::::::::
if not x==x%CONFIG_LINES (
  for /f %%C in (%CONFIG_LINES%) do (
    if not exist "%%C" (
      echo "** WARNING: Cannot read '%%C' specified in '%JETTY_CONF%'
    ) else (
    if not exist "%%C\*.xml" (
      :: assume it's a configure.xml file
      set CONFIGS=%CONFIGS% %%C
    ) else (
      :: assume it's a directory with configure.xml files
      :: for example: /etc/jetty.d/
      :: sort the files before adding them to the list of CONFIGS
      for %%F in ("%%C\*.xml") do (
         if exist "%%FILE" (
            set CONFIGS=%CONFIGS% %%F
         ) else (
           echo ** WARNING: Cannot read '%%F' specified in '%JETTY_CONF%'
         )
      )
    )
  )
)

:no_conf_jetty

::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Run the demo server if there's nothing else to run
::::::::::::::::::::::::::::::::::::::::::::::::::::::
if not x==x%CONFIGS% goto have_configs
  set CONFIGS=%JETTY_HOME%\etc\demo.xml %JETTY_HOME%\etc\admin.xml


:have_configs
echo CONFIGS=%CONFIGS%


::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Check where logs should go.
::::::::::::::::::::::::::::::::::::::::::::::::::::::
if x==x%JETTY_LOG% set JETTY_LOG=%JETTY_HOME%\logs

::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Find a location for the pid file
:::::::::::::::::::::::::::::::::::::::::::::::::::::
if x==x%JETTY_RUN% set JETTY_RUN=%TEMP%

:::::::::::::::::::::::::::::::::::::::::::::::::::
:: Determine which JVM of version >1.2
:: Try to use JAVA_HOME
:::::::::::::::::::::::::::::::::::::::::::::::::::
if x==x%JAVA% (
  if not x==x%JAVACMD% (
     set JAVA="%JAVACMD%"
  ) else (
    if exist %JAVA_HOME%\bin\jre (
      set JAVA=%JAVA_HOME%\bin\jre
    ) else (
       set JAVA=%JAVA_HOME%\bin\java
    )
  )
)


::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Build the classpath with Jetty's bundled libraries.
::::::::::::::::::::::::::::::::::::::::::::::::::::::
set CP=%JETTY_HOME%\lib\javax.servlet.jar
set CP=%CP%;%JETTY_HOME%\lib\com.mortbay.jetty.jar
set CP=%CP%;%JETTY_HOME%\lib\com.microstar.xml.jar
set CP=%CP%;%JETTY_HOME%\lib\cryptix-sasl-jetty.jar;%JETTY_HOME%\lib\javax-sasl.jar
set CP=%CP%;%JETTY_HOME%\lib\log4j.jar
if exist "%JETTY_HOME%\LIB\org.apache.jasper.jar"  set CP=%CP%;%JETTY_HOME%\lib\org.apache.jasper.jar
if exist "%JETTY_HOME%\LIB\com.sun.net.ssl.jar"    set CP=%CP%;%JETTY_HOME%\lib\com.sun.net.ssl.jar
if exist "%JAVA_HOME%\lib\tools.jar"               set CP=%CP%;%JAVA_HOME%\lib\tools.jar
set CLASSPATH=%CP%;%CLASSPATH%

::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Add jetty properties to Java VM options.
::::::::::::::::::::::::::::::::::::::::::::::::::::::
rem set JAVA_OPTIONS=-Djetty.home="%JETTY_HOME%" -Djetty.log="%JETTY_LOG%" %JAVA_OPTIONS%
set JAVA_OPTIONS=-Djetty.home="%JETTY_HOME%" -Djetty.log="%JETTY_LOG%" -Djavax.security.sasl.server.pkgs=cryptix.sasl -Dcryptix.sasl.srp.passwordfile=etc/tpasswd %JAVA_OPTIONS%

::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: This is how the Jetty server will be started
::::::::::::::::::::::::::::::::::::::::::::::::::::::
set RUN_CMD=%JAVA% %JAVA_OPTIONS% com.mortbay.Jetty.Server %CONFIGS%


::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Comment these out after you're happy with what
:: the script is doing.
::::::::::::::::::::::::::::::::::::::::::::::::::::::
echo JETTY_HOME     =  %JETTY_HOME%
echo JETTY_CONF     =  %JETTY_CONF%
echo JETTY_LOG      =  %JETTY_LOG%
echo JETTY_RUN      =  %JETTY_RUN%
echo CONFIGS        =  %CONFIGS%
echo PATH_SEPARATOR =  %PATH_SEPARATOR%
echo JAVA_OPTIONS   =  %JAVA_OPTIONS%
echo JAVA           =  %JAVA%
echo CLASSPATH      =  %CLASSPATH%
echo RUN_CMD        =  %RUN_CMD%



:::::::::::::::::::::::::::::::::::::::::::::::::::
:: Do the action
:::::::::::::::::::::::::::::::::::::::::::::::::::

if not "%ACTION%"=="start" goto try_stop
        echo "Starting Jetty: "
        echo "STARTED %date%" >>%JETTY_LOG%\jetty.out 
        echo "RUN_CMD %RUN_CMD%" >> %JETTY_LOG%\jetty.out
        start %RUN_CMD% >> %JETTY_LOG%\jetty.out
        goto END

:try_stop
if not "%ACTION%"=="stop" goto try_restart
        echo "Shutting down Jetty: "

        rem @TODO stop the server here
        del /q /f %JETTY_RUN%\jetty.pid
        echo "STOPPED `date`" >> %JETTY_LOG%\jetty.out
        goto END

:try_restart
if not "%ACTION%"=="restart" goto try_run
        %0 stop %*
        sleep 5
        %0 start %*
        goto END

:try_run
if not "%ACTION%"=="run" goto try_check
        echo "Running Jetty: "
        echo "STARTED `date`" >>%JETTY_LOG%\jetty.out
        %RUN_CMD%
        goto END

:try_check
if not "%ACTION%"=="check" goto no_action
        echo "Checking arguments to Jetty: "
        :: do nothing
        goto END

:no_action
echo "Usage: %0 {start|stop|run|restart|check} [ CONFIGS ... ] "
goto ERROR

:ERROR

:END
rem endlocal







