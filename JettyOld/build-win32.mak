
#
# Makefile for Jetty on Win32
# No multiline recipes to avoid tab/space conversion problems.
#
# REQUIRES: make find rm (Cygwin), jar javac (JDK), zip (Info-Zip)
#
# The only(?) thing Win32 specific about this file is the use of
# the path delimiter '\' instead of '/' and the classpath
# delimiter ';' instead of ':'.
#
# Otherwise this should work as well with Unix.
#

all : build

build : jars-debug jars-release
rebuild : clean build
clean : ; -rm -rf classes lib/*.jar

build-debug   : classes-compile-debug properties-copy-debug
build-release : classes-compile-release properties-copy-release

classes-mkdir           : ; cd classes || md classes
classes-mkdir-debug     : classes-mkdir ; cd classes\debug || md classes\debug
classes-mkdir-release   : classes-mkdir ; cd classes\release || md classes\release
classes-compile-debug   : classes-mkdir-debug   ; set CLASSPATH=src;contrib;. && javac -g -d classes\debug src/BuildJetty.java
classes-compile-release : classes-mkdir-release ; set CLASSPATH=src;contrib;. && javac -O -d classes\release src/BuildJetty.java

properties-copy           : properties-copy-debug properties-copy-release

properties-copy-debug     : properties-zip ; cd classes\debug && unzip -o ../properties
properties-copy-release   : properties-zip ; cd classes\release && unzip -o ../properties
properties-zip            : properties-zip-gnujsp properties-zip-javax
properties-zip-gnujsp     : ; cd contrib && find org/gjt/jsp -name *.properties | zip -@u ../classes/properties
properties-zip-javax      : ; cd src     && find javax -name *.properties | zip -@u ../classes/properties

jars-release              : jar-gnujsp-release jar-javax-servlet-release jar-com-mortbay-release
classes-release           : classes-compile-release properties-copy-release
jar-gnujsp-release        : classes-release ; cd classes\release && jar -cf ../../lib/gnujsp.jar org/gjt/jsp
jar-javax-servlet-release : classes-release ; cd classes\release && jar -cf ../../lib/javax.servlet.jar javax/servlet
jar-com-mortbay-release   : classes-release ; cd classes\release && jar -cf ../../lib/com.mortbay.jar com/mortbay

jars-debug                : jar-gnujsp-debug jar-javax-servlet-debug jar-com-mortbay-debug
classes-debug             : classes-compile-debug properties-copy-debug
jar-gnujsp-debug          : classes-debug ; cd classes\debug && jar -cf ../../lib/debug.gnujsp.jar org/gjt/jsp
jar-javax-servlet-debug   : classes-debug ; cd classes\debug && jar -cf ../../lib/debug.javax.servlet.jar javax/servlet
jar-com-mortbay-debug     : classes-debug ; cd classes\debug && jar -cf ../../lib/debug.com.mortbay.jar com/mortbay
