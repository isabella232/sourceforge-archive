@echo off
:: ===========================================================
:: RunJetty.bat
:: ===========================================================
:: This batch file initializes the environment and runs the 
:: Jetty web server under Windows NT. It uses Windows NT cmd
:: extensions and does not work under Window 9x. 
::
::
:: USAGE: 
:: runjetty [configuration files]
:: Example: jetty.bat etc\admin.xml etc\demo.xml
::
:: ENVIRONMENT VARIABLES:
:: The following environment variables should be set to use this
:: batch file. These can be set jettyenv.bat file which willed
:: be called if found in the current working directory.
::
:: JAVA_HOME - this should be set to the directory that the 
:: Java Developers Kit or JDK has been installed.
:: Example: set JAVA_HOME=c:\jdk1.3
::
:: JETTY_HOME - this should be set to the base installation directory
:: where the Jetty server was installed.  The batch file will try to set
:: this on its own by looking for the jetty.jar file in the lib
:: subdirectory.
:: Example: set JETTY_HOME=c:\Jetty-3.1.RC9
::
:: JETTY_LOG - (Optional) this should be the full name of the subdirectory
:: that should be used by Jetty for storing log files.  If it is not 
:: provided then the logs directory below JETTY_HOME will be created 
:: if needed and used.
::
:: JETTY_OPTIONS - (Optional) Any options to be passed to the JVM 
:: can be set in this variable.  It will have appended to it:
::     -Djetty.home=%JETTY_HOME% -Djetty.logs=%JETTY_LOG%
::
:: NOTES: 
:: -  etc\admin.xml file is always prepended to each set of arguments
::
:: -  The drive and directory are changed during execution of the batch file
::    to make JETTY_HOME the current working directory.  The original drive
::    and directory are restored when Jetty is stopped and the batch file 
::    is completed.
::
:: Created by John T. Bell
:: j_t_bell@yahoo.com
:: September 14th, 2001
:: ===========================================================
rem ===========================================================
rem == save environment variables
rem ===========================================================
setlocal
set x_PATH=%path%
set x_CP=%CP%

rem ===========================================================
rem == save the current directory and drive
rem ===========================================================
for /F "delims=;" %%i in ('cd') do set x_PWD=%%i
set x_DRIVE=%x_PWD:~0,2%

rem ===========================================================
rem == Look for batch file to set environment variables
rem ===========================================================
IF EXIST jettyenv.bat CALL jettyenv.bat

rem ===========================================================
rem == check for JAVA_HOME environment variable
rem ===========================================================
if NOT "%JAVA_HOME%"=="" goto got_java_home
	echo The environment variable JAVA_HOME must be set.
	goto done
:got_java_home

rem == if JETTY_HOME is not set
if NOT "%JETTY_HOME%"=="" goto got_jetty_home
rem ==   set JETTY_HOME to the current directory

rem ===========================================================
rem == try to set JETTY_HOME by looking for the jetty.jar file
rem ===========================================================
if EXIST .\lib\org.mortbay.jetty.jar goto found_jar
cd ..
if EXIST .\lib\org.mortbay.jetty.jar goto found_jar
        echo The environment variable JETTY_HOME must be set!
        goto done
:found_jar
	for /F "delims=;" %%i in ('cd') do set JETTY_HOME=%%i
:endif1

:got_jetty_home
rem ===========================================================
rem == get Drive information
rem ===========================================================
set JETTY_DRIVE=%JETTY_HOME:~0,2%

rem ===========================================================
rem == Change directory to the JETTY_HOME root directory.
rem ===========================================================
%JETTY_DRIVE%
cd "%JETTY_HOME%"

rem ===========================================================
rem == set CLASSPATH
rem ===========================================================
set CP=%JETTY_HOME%\lib\javax.servlet.jar
set CP=%CP%;%JETTY_HOME%\lib\javax.servlet.jar
set CP=%CP%;%JETTY_HOME%\lib\org.mortbay.jetty.jar
set CP=%CP%;%JETTY_HOME%\lib\javax.xml.jaxp.jar
set CP=%CP%;%JETTY_HOME%\lib\org.apache.crimson.jar
set CP=%CP%;%JETTY_HOME%\lib\org.apache.jasper.jar
set CP=%CP%;%JETTY_HOME%\lib\com.sun.net.ssl.jar
set CP=%CP%;%JAVA_HOME%\lib\tools.jar
set CP="%CP%"

rem ===========================================================
rem == check for and set command line args
rem ===========================================================
rem == if no args then set admin.xml and demo.xml
rem == note: since we will cd to the JETTY_HOME directory
rem          we do not need to append JETTY_HOME onto the
rem          file names.
if [%1]==[] (set ARGS=etc\admin.xml etc\demo.xml & goto args_done)
rem == append command line arguments on ARGS
:setargs
if NOT [%1]==[] (set ARGS=%args% %1 & shift & goto setargs)
:args_done

rem ===========================================================
rem == check for log directory
rem ===========================================================
if NOT "%JETTY_LOG%"=="" goto logs_set
dir /b /ad | find /I "logs" >NUL
if ERRORLEVEL 0 goto found_logs
	mkdir logs
:found_logs
	set JETTY_LOG=%JETTY_HOME%\logs
:logs_set

rem ===========================================================
rem == build jvm options
rem ===========================================================
set OPTIONS=-Djetty.home="%JETTY_HOME%" -Djetty.log="%JETTY_LOG%"
if DEFINED JETTY_OPTIONS set OPTIONS=%OPTIONS% %JETTY_OPTIONS%

rem ===========================================================
rem == build run command
rem ===========================================================
set RUNME="%JAVA_HOME%\bin\java" -cp %CP% %OPTIONS% org.mortbay.jetty.Server %ARGS%

rem ===========================================================
rem == echo environment variables to aid in debugging
rem ===========================================================
echo 
echo JAVA_HOME=%JAVA_HOME%
echo JETTY_HOME=%JETTY_HOME%
echo JETTY_DRIVE=%JETTY_DRIVE%
echo JETTY_LOG=%JETTY_LOG%
echo OPTIONS=%OPTIONS%
echo ARGS=%args%
echo RUNME=%RUNME%
echo 

rem ===========================================================
rem == run jetty
rem ===========================================================
rem pause
%RUNME%

:done
rem ===========================================================
rem == clean up our toys
rem ===========================================================
%x_DRIVE%
cd "%x_PWD%"
set PATH=%x_PATH%
set CP=%x_CP%
set ARGS=
set OPTIONS=
set RUNME=
endlocal
