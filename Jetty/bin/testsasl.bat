@echo off

set CP=%JETTY_HOME%\lib\cryptix-sasl-jetty.jar;%JETTY_HOME%\lib\javax-sasl.jar
set CP=%CP%;%JETTY_HOME%\lib\com.mortbay.jetty.jar

set CLASSPATH=%CP%;%CLASSPATH%

%JAVA_HOME%\bin\java -Djavax.security.sasl.client.pkgs=cryptix.sasl com.mortbay.Util.SaslTest %1 %2 %3
